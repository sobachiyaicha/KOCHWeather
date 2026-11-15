package com.koch.weather.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.annotation.AnyRes
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.koch.weather.data.model.AdCreative
import com.koch.weather.data.model.AdMediaType
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Тег логов: видно в `adb logcat -s AdVideoEngine`, какие ветки реально живут. */
private const val TAG = "AdVideoEngine"

/**
 * Сколько тёплый плеер (вместе с уже сконфигурированным кодеком) живёт после
 * закрытия поп-апа. Дальше простой освобождает его фоновой корутиной.
 */
private const val IDLE_RELEASE_MS = 60_000L

/**
 * Выбор декодера AVC для рекламного ролика.
 *
 * `true` — программный H.264-декодер (`c2.android.*` / `OMX.google.*`): для
 * 360×480 он дешевле, чем 5-секундное ожидание вендорного `AtlasService`
 * на MediaTek/ColorOS. `false` — аппаратный (поведение по умолчанию вендора).
 * Переключение — одной строкой, без правок логики.
 */
private const val PREFER_SOFTWARE_AVC_DECODER = true

/**
 * Движок видео-рекламы поп-апа: **предзагрузка** ролика и владение экземпляром плеера.
 *
 * Зачем отдельный синглтон. Раньше `MediaPlayer` создавался только в композиции диалога,
 * поэтому кодек на OPPO/realme (Android 12) стартовал 5–10 с уже после открытия поп-апа:
 * пользователь видел чёрный экран. Теперь `MainScreenViewModel.requestAdPopup()` зовёт
 * [prepare] в момент стирания облаков — плеер создаётся, получает `MediaItem` и
 * `prepare()` **до** появления диалога. Диалог только прикрепляет готовый плеер
 * к `PlayerView` и вызывает `play()`: первый кадр появляется сразу, без индикаторов
 * загрузки (их пользователь просил не делать).
 *
 * Прогрев кодека. После `setMediaItem` + `prepare()` плееру сразу даётся
 * **фиктивная offscreen-поверхность** (`SurfaceTexture(0)` + `Surface` 2×2).
 * `MediaCodecVideoRenderer` конфигурирует кодек, как только появляется выходная
 * поверхность, поэтому `c2.mtk.avc.decoder` (и его binder-ожидание
 * `AtlasService`) поднимается ещё до открытия поп-апа. При показе `PlayerView`
 * переключает выход на реальную поверхность через `MediaCodec.setOutputSurface` —
 * кодек переиспользуется, повторной инициализации нет.
 *
 * Тёплый плеер. После закрытия поп-апа экземпляр **не освобождается**:
 * [parkPlayer] делает `pause()` + `seekTo(0)` + возврат на фиктивную поверхность,
 * следующий показ того же ролика — сразу `play()`. Освобождение только в
 * [shutdown] (`MainActivity.onDestroy(isFinishing)`) и по таймеру простоя
 * [IDLE_RELEASE_MS] в фоновой корутине.
 *
 * Что важно про освобождение. У ExoPlayer `release()` обязан вызываться на том же
 * потоке, на котором создан плеер: `ExoPlayerImpl.release()` начинается с
 * `verifyApplicationThread()` и бросает `IllegalStateException` при вызове из другого
 * потока. Поэтому экземпляр создаётся на main (иначе его нельзя отдать в `PlayerView`)
 * и `release()` выполняется на main, но **не внутри кадра закрытия**: [releasePlayer]
 * запускает освобождение из `Dispatchers.IO` и ставит задачу в очередь
 * application-thread плеера. Перед `release()` обязательно
 * `player.clearVideoSurface()`: иначе `MediaCodec.setOutputSurface` получает уже
 * освобождённую Surface (`IllegalArgumentException: The surface has been released`).
 * В `DisposableEffect.onDispose` нет ни одного блокирующего вызова.
 *
 * Fallback. Если ExoPlayer поднять/подготовить не удалось (`Throwable` при создании
 * или `prepare()`), движок переключается на один общий `MediaPlayer` на процесс
 * ([LegacyClip]): он **никогда не освобождается во время показа**, между роликами
 * делает `pause()` + `seekTo(0)`, а `release()` вызывается только в [shutdown]
 * (выход из приложения). Так 20-секундный `release()` в принципе не попадает
 * в горячий путь.
 */
object AdVideoEngine {

    /** Чем реально воспроизводится текущий ролик. */
    enum class Renderer {
        /** Ролик не подготовлен (нет видео-креатива / ещё не звали [prepare]). */
        NONE,

        /** Основной путь: AndroidX Media3 `ExoPlayer` + `PlayerView`. */
        EXOPLAYER,

        /** Страховка: общий `MediaPlayer` + `TextureView`, освобождается только на выходе. */
        LEGACY_MEDIA_PLAYER
    }

    /**
     * Фоновый scope движка: сюда уходит всё, что нельзя делать в кадре показа/закрытия.
     * Живёт столько же, сколько процесс, — движок и есть синглтон на процесс.
     */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var currentRenderer: Renderer = Renderer.NONE

    /** `R.raw`-ресурс, под который подготовлен текущий плеер. 0 — ничего не готово. */
    @Volatile
    private var preparedVideoRes: Int = 0

    /** Один тёплый экземпляр плеера на процесс: переиспользуется между показами. */
    private var exoPlayer: ExoPlayer? = null

    /**
     * Фиктивная offscreen-поверхность: кодек получает выход сразу после
     * `prepare()`. Живёт до [shutdown] и переиспользуется всеми экземплярами.
     */
    private var dummyTexture: SurfaceTexture? = null
    private var dummySurface: Surface? = null

    /** Таймер простоя: освобождает тёплый плеер через [IDLE_RELEASE_MS]. */
    private var idleReleaseJob: Job? = null

    /** Открыт ли сейчас поп-ап: пока true, таймер простоя не имеет права освобождать. */
    private var isVisible = false

    /** Fallback-плеер: один общий на процесс, освобождается только в [shutdown]. */
    private var legacyClip: LegacyClip? = null

    /** Колбэк «ролик доиграл» fallback-пути; выставляется диалогом на время показа. */
    private var legacyEndedCallback: (() -> Unit)? = null

    /** Текущий режим воспроизведения (то, что должен отрисовать диалог). */
    val renderer: Renderer
        get() = currentRenderer

    /**
     * Готовит ролик видео-креатива заранее — вызывается из `requestAdPopup()` до показа
     * диалога. Идемпотентно: повторный вызов с тем же роликом ничего не пересоздаёт.
     * Фото-креатив не требует ролика, поэтому просто паркует тёплый плеер
     * (освободит его таймер простоя).
     */
    @MainThread
    fun prepare(context: Context, creative: AdCreative) {
        if (creative.type != AdMediaType.VIDEO) {
            parkCurrent()
            return
        }
        prepare(context, creative.mediaRes)
    }

    /**
     * Готовит ролик [videoRes] (`R.raw.*`) заранее — до появления диалога.
     *
     * Обязательно main thread: ExoPlayer создаётся и управляется на том потоке,
     * на котором будет жить его application-loop (main, т.к. отдаём в `PlayerView`).
     * Метод не блокирующий по декодированию: `prepare()` асинхронный.
     *
     * Тёплый экземпляр переиспользуется: тот же ролик — сразу выход, другой —
     * `setMediaItem` на существующем плеере (новый не создаётся). После
     * `prepare()` на плеер вешается фиктивная поверхность, чтобы кодек
     * сконфигурировался до открытия поп-апа.
     */
    @MainThread
    fun prepare(context: Context, @AnyRes videoRes: Int) {
        val app = context.applicationContext
        if (videoRes == 0) return
        cancelIdleRelease()

        // Тёплый плеер: тот же ролик — ничего не трогаем, другой — меняем
        // MediaItem на существующем экземпляре (кодек инициализируется заранее).
        val warm = exoPlayer
        if (warm != null && currentRenderer == Renderer.EXOPLAYER) {
            if (preparedVideoRes == videoRes) {
                if (!isVisible) {
                    runCatching { attachDummySurface(warm) }
                        .onFailure { Log.w(TAG, "warm-up surface недоступна", it) }
                }
                Log.i(TAG, "reuse warm ExoPlayer for res=$videoRes")
                return
            }
            val switched = runCatching {
                warm.playWhenReady = false
                warm.stop()
                warm.setMediaItem(MediaItem.fromUri(resourceUri(app, videoRes)))
                warm.prepare()
            }.isSuccess
            if (switched) {
                preparedVideoRes = videoRes
                if (!isVisible) {
                    runCatching { attachDummySurface(warm) }
                        .onFailure { Log.w(TAG, "warm-up surface недоступна", it) }
                }
                Log.i(TAG, "switched warm ExoPlayer to res=$videoRes")
                return
            }
            Log.w(TAG, "не удалось сменить ролик на тёплом плеере — пересоздаём")
            releasePlayer(warm)
        }

        // Первый показ (или тёплый плеер сломался): создаём движок, сразу
        // готовим ролик и вешаем фиктивную поверхность — кодек до поп-апа.
        preparedVideoRes = videoRes
        val uri = resourceUri(app, videoRes)

        val built = runCatching { createExoPlayer(app, uri) }
            .onFailure { Log.w(TAG, "ExoPlayer недоступен — включаем fallback MediaPlayer", it) }
            .getOrNull()

        if (built != null) {
            exoPlayer = built
            currentRenderer = Renderer.EXOPLAYER
            Log.i(TAG, "prepared ExoPlayer for res=$videoRes")
        } else {
            // ExoPlayer не поднялся: общий MediaPlayer, который не освобождаем.
            currentRenderer = Renderer.LEGACY_MEDIA_PLAYER
            ensureLegacyClip().apply {
                setEndedCallback(legacyEndedCallback)
                prepare(app, videoRes)
            }
            Log.i(TAG, "prepared MediaPlayer fallback for res=$videoRes")
        }
    }

    /**
     * Страховка для прямого показа диалога (например, ролик за попытку в лутбоксе):
     * если предзагрузки не было, плеер готовится здесь. В штатном пути это no-op.
     *
     * Таймер простоя снимаем сразу: диалог уже открывается, тёплый плеер
     * освобождать нельзя, даже если 60 с истекли ровно в этот кадр.
     */
    @MainThread
    fun ensurePrepared(context: Context, @AnyRes videoRes: Int): Renderer {
        cancelIdleRelease()
        if (preparedVideoRes != videoRes || currentRenderer == Renderer.NONE) {
            prepare(context, videoRes)
        }
        return currentRenderer
    }

    /** Подготовленный плеер для ролика [videoRes]; null, если готов другой/плеера нет. */
    @MainThread
    fun exoPlayerFor(@AnyRes videoRes: Int): ExoPlayer? =
        exoPlayer.takeIf { currentRenderer == Renderer.EXOPLAYER && preparedVideoRes == videoRes }

    /**
     * Немедленное освобождение конкретного плеера (аварийная смена ролика/выход).
     * Штатное закрытие поп-апа сюда **не ходит**: движок паркует тёплый плеер
     * ([parkPlayer]) и отпускает его только по таймеру простоя или в [shutdown].
     *
     * Освобождение **не выполняется в кадре закрытия**: сначала задача уходит
     * в IO-scope, затем ставится в очередь application-thread плеера. Почему не
     * `Dispatchers.IO` напрямую: media3 жёстко требует свой поток
     * (`ExoPlayerImpl.verifyApplicationThread()`), вызов из чужого потока —
     * гарантированный `IllegalStateException`.
     *
     * Порядок teardown строгий: `clearVideoSurface()` **до** `release()`, иначе
     * `MediaCodec.setOutputSurface` получает уже освобождённую Surface
     * (`IllegalArgumentException: The surface has been released`).
     */
    private fun releasePlayer(player: ExoPlayer?, releaseDummy: Boolean = false) {
        if (player == null) return
        if (exoPlayer === player) {
            exoPlayer = null
            preparedVideoRes = 0
            currentRenderer = Renderer.NONE
            isVisible = false
            cancelIdleRelease()
        }
        val applicationLooper = player.applicationLooper
        // Фиктивную поверхность снимаем с учёта сразу: новый плеер получит новую,
        // а эта будет освобождена уже после release() старого на application-потоке.
        val dummyToRelease = if (releaseDummy) dummySurface.also { dummySurface = null } else null
        val textureToRelease = if (releaseDummy) dummyTexture.also { dummyTexture = null } else null
        ioScope.launch {
            Handler(applicationLooper).post {
                // Surface отвязываем ДО release: иначе освобождённая Surface
                // попадает в setOutputSurface (ошибка из лога realme RMX3151).
                runCatching { player.clearVideoSurface() }
                runCatching { player.release() }
                    .onFailure { Log.w(TAG, "ExoPlayer.release()", it) }
                dummyToRelease?.let { runCatching { it.release() } }
                textureToRelease?.let { runCatching { it.release() } }
            }
        }
    }

    /**
     * Закрытие поп-апа: плеер **не освобождаем**, а паркуем тёплым.
     *
     * Порядок важен: `pause()`, переключение выхода кодека на фиктивную
     * [dummySurface] (`MediaCodec.setOutputSurface` — кодек остаётся
     * сконфигурированным), затем `seekTo(0)`. Только после этого диалог снимает
     * `PlayerView`: он уже не сможет отдать кодеку освобождённую Surface. Через
     * [IDLE_RELEASE_MS] без показа плеер освободит фоновая корутина.
     */
    @MainThread
    fun parkPlayer(player: ExoPlayer?) {
        if (player == null || exoPlayer !== player || currentRenderer != Renderer.EXOPLAYER) return
        isVisible = false
        runCatching {
            player.pause()
            attachDummySurface(player)
            player.seekTo(0)
        }.onFailure { Log.w(TAG, "park ExoPlayer", it) }
        scheduleIdleRelease()
    }

    /** Спокойный вариант для ViewModel: паркует текущий тёплый плеер. */
    @MainThread
    fun parkCurrent() {
        parkPlayer(exoPlayer)
    }

    /** Поп-ап открыт: таймер простоя не должен освободить плеер под показом. */
    @MainThread
    fun markVisible() {
        isVisible = true
        cancelIdleRelease()
    }

    // ------------------------------------------------------------------------
    // Lifecycle: пауза/возобновление (ON_STOP / ON_START)
    // ------------------------------------------------------------------------

    /** Пауза на `ON_STOP`: ролик не должен звучать из кармана. */
    fun pausePlayback() {
        exoPlayer?.let { player -> runCatching { player.pause() } }
        legacyClip?.pause()
    }

    /** Возобновление на `ON_START` (ExoPlayer — через `Player.play()`). */
    fun resumePlayback() {
        // Тёплый, но не показанный ExoPlayer в фоне не играет: иначе после закрытия
        // поп-апа звук рекламы включился бы на возврате в приложение.
        if (isVisible) {
            exoPlayer?.let { player ->
                // Законченный ролик не перезапускаем: поп-ап уже закрывается.
                runCatching {
                    if (player.playbackState != Player.STATE_ENDED) player.play()
                }
            }
        }
        // Fallback живёт по своим флагам (поверхность/готовность) и сам решает, играть ли.
        legacyClip?.resume()
    }

    // ------------------------------------------------------------------------
    // Fallback: общий MediaPlayer + TextureView
    // ------------------------------------------------------------------------

    /** Подключить `TextureView` fallback-плеера (ролик уже подготовлен в [prepare]). */
    @MainThread
    fun attachLegacyView(view: TextureView) {
        legacyClip?.attach(view)
    }

    /** Отключить `TextureView`: между роликами только `pause()`, без `release()`. */
    @MainThread
    fun detachLegacyView(view: TextureView) {
        legacyClip?.detach(view)
    }

    /** Колбэк «fallback-ролик доиграл» (диалог ставит его на время показа). */
    @MainThread
    fun bindLegacyEnded(callback: (() -> Unit)?) {
        legacyEndedCallback = callback
        legacyClip?.setEndedCallback(callback)
    }

    /**
     * Выход из приложения: единственное место, где общий `MediaPlayer`, тёплый
     * ExoPlayer и фиктивная поверхность действительно освобождаются. Штатно
     * второй путь освобождения — таймер простоя [IDLE_RELEASE_MS].
     */
    @MainThread
    fun shutdown() {
        isVisible = false
        cancelIdleRelease()
        val player = exoPlayer
        if (player != null) {
            // release() отвяжет Surface от кодека и освободит фиктивную поверхность
            // на application-потоке плеера уже после снятия экрана.
            releasePlayer(player, releaseDummy = true)
        } else {
            // Плеера уже нет (освобождён по простою) — отпускаем только поверхность.
            releaseDummySurfaceNow()
        }
        exoPlayer = null
        preparedVideoRes = 0
        currentRenderer = Renderer.NONE
        legacyClip?.shutdown()
        legacyClip = null
        legacyEndedCallback = null
    }

    // ------------------------------------------------------------------------
    // Внутреннее
    // ------------------------------------------------------------------------

    /**
     * Создаёт ExoPlayer с выбранным селектором декодера и сразу прогревает кодек:
     * `prepare()` + фиктивная поверхность. Звук не играет (`playWhenReady = false`).
     */
    @OptIn(UnstableApi::class)
    private fun createExoPlayer(context: Context, uri: Uri): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            if (PREFER_SOFTWARE_AVC_DECODER) {
                // Селектор общий для аудио- и видеорендереров, поэтому программный
                // фильтр применяем только к AVC (video/avc), остальные — как обычно.
                setMediaCodecSelector(
                    MediaCodecSelector { mime, secure, tunneling ->
                        val all = runCatching {
                            MediaCodecUtil.getDecoderInfos(mime, secure, tunneling)
                        }.getOrDefault(emptyList())
                        if (mime != MimeTypes.VIDEO_H264) {
                            all
                        } else {
                            val chosen = all.filter { info ->
                                info.name.startsWith("c2.android.") ||
                                    info.name.startsWith("OMX.google.")
                            }.ifEmpty { all }
                            // Видно в logcat: на realme RMX3151 должен выбраться
                            // c2.android.avc.decoder, а не c2.mtk.avc.decoder.
                            Log.i(
                                TAG,
                                "AVC decoders ${all.map { it.name }} → ${chosen.map { it.name }}"
                            )
                            chosen
                        }
                    }
                )
            }
        }
        val player = ExoPlayer.Builder(context, renderersFactory).build()
        // Звук рекламы включён: полная громкость, медиа-поток «как в кино».
        player.volume = 1f
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ false
        )
        player.setMediaItem(MediaItem.fromUri(uri))
        // Готовим заранее, но НЕ играем: звук не должен идти до появления диалога.
        // Диалог прикрепит плеер к PlayerView и сам вызовет play().
        player.playWhenReady = false
        player.prepare()
        // Прогрев кодека: выходная поверхность есть сразу, поэтому
        // MediaCodecVideoRenderer конфигурирует декодер ещё до открытия поп-апа.
        runCatching { attachDummySurface(player) }
            .onFailure { Log.w(TAG, "warm-up surface недоступна", it) }
        return player
    }

    /**
     * Фиктивная offscreen-поверхность 2×2: кодеку есть куда отдавать кадры,
     * пока поп-ап закрыт. Создаётся на main (нужен Looper) и живёт до [shutdown].
     */
    private fun ensureDummySurface(): Surface {
        dummySurface?.let { return it }
        val texture = SurfaceTexture(0).apply { setDefaultBufferSize(2, 2) }
        val surface = Surface(texture)
        dummyTexture = texture
        dummySurface = surface
        Log.i(TAG, "created dummy output surface 2x2")
        return surface
    }

    private fun attachDummySurface(player: ExoPlayer) {
        player.setVideoSurface(ensureDummySurface())
    }

    /** Освободить фиктивную поверхность синхронно (main) — когда плеера уже нет. */
    private fun releaseDummySurfaceNow() {
        dummySurface?.let { runCatching { it.release() } }
        dummySurface = null
        dummyTexture?.let { runCatching { it.release() } }
        dummyTexture = null
    }

    /** Освободить тёплый плеер после [IDLE_RELEASE_MS] без показа (фоновая корутина). */
    private fun scheduleIdleRelease() {
        idleReleaseJob?.cancel()
        idleReleaseJob = ioScope.launch {
            delay(IDLE_RELEASE_MS)
            withContext(Dispatchers.Main.immediate) {
                val player = exoPlayer
                if (player != null && !isVisible) {
                    Log.i(TAG, "idle ${IDLE_RELEASE_MS}ms — освобождаем тёплый ExoPlayer")
                    // Вместе с плеером отпускаем и фиктивную поверхность.
                    releasePlayer(player, releaseDummy = true)
                }
            }
        }
    }

    private fun cancelIdleRelease() {
        idleReleaseJob?.cancel()
        idleReleaseJob = null
    }

    private fun resourceUri(context: Context, @AnyRes resId: Int): Uri =
        Uri.parse("android.resource://${context.packageName}/$resId")

    private fun ensureLegacyClip(): LegacyClip =
        legacyClip ?: LegacyClip().also { legacyClip = it }

    /**
     * Fallback-плеер: **один** `MediaPlayer` на процесс.
     *
     * `release()` здесь вызывается только из [shutdown]. Между роликами —
     * `pause()` + `seekTo(0)` и повторная установка источника, поэтому
     * многосекундное освобождение не попадает ни в показ, ни в закрытие поп-апа.
     * Всё состояние защищено `runCatching`: fallback не должен ронять экран.
     */
    private class LegacyClip {

        private var player: MediaPlayer? = null
        private var surface: Surface? = null
        private var viewRef: WeakReference<TextureView>? = null
        private var videoWidth = 0
        private var videoHeight = 0
        private var isPrepared = false
        private var isCompleted = false
        private var isForeground = true

        /** Колбэк «ролик доиграл»: его выставляет диалог через `onEnded` в композиции. */
        var onEnded: (() -> Unit)? = null

        fun prepare(context: Context, @AnyRes videoRes: Int) {
            val mp = player ?: createPlayer()
            isPrepared = false
            isCompleted = false
            runCatching {
                // Между роликами общий плеер НЕ освобождается: пауза и в начало.
                if (mp.isPlaying) mp.pause()
                mp.seekTo(0)
                mp.reset()
                context.resources.openRawResourceFd(videoRes)?.use { descriptor ->
                    mp.setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length
                    )
                } ?: error("res/raw не найден: $videoRes")
                mp.prepareAsync()
            }.onFailure { Log.w(TAG, "fallback prepare failed for res=$videoRes", it) }
        }

        private fun createPlayer(): MediaPlayer = MediaPlayer().also { mp ->
            player = mp
            runCatching {
                mp.setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
            }
            mp.setOnPreparedListener { prepared ->
                isPrepared = true
                runCatching { prepared.setVolume(1f, 1f) }
                if (videoWidth <= 0 || videoHeight <= 0) {
                    runCatching {
                        videoWidth = prepared.videoWidth
                        videoHeight = prepared.videoHeight
                    }
                }
                fitToView()
                startIfReady()
            }
            mp.setOnCompletionListener {
                isCompleted = true
                onEnded?.invoke()
            }
            mp.setOnVideoSizeChangedListener { _, width, height ->
                videoWidth = width
                videoHeight = height
                fitToView()
            }
            mp.setOnErrorListener { _, _, _ ->
                // Битый/неподдерживаемый файл не роняет поп-ап: остаётся чёрная подложка.
                true
            }
        }

        /** Подключить `TextureView`: ролик стартует, как только появится поверхность. */
        fun attach(view: TextureView) {
            viewRef = WeakReference(view)
            view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    bindSurface(surfaceTexture)
                }

                override fun onSurfaceTextureSizeChanged(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    fitToView()
                }

                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    // Наш Surface освобождаем сами; true — систему просим освободить SurfaceTexture.
                    releaseSurface()
                    return true
                }

                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
            }
            // TextureView уже с готовой поверхностью — колбэк available повторно не придёт.
            if (view.isAvailable) view.surfaceTexture?.let(::bindSurface)
        }

        /** Отвязать `TextureView` от общего плеера. Релиза здесь нет by design. */
        fun detach(view: TextureView) {
            view.surfaceTextureListener = null
            if (viewRef?.get() === view) viewRef = null
            releaseSurface()
            runCatching { if (player?.isPlaying == true) player?.pause() }
        }

        fun pause() {
            isForeground = false
            val mp = player ?: return
            runCatching { if (isPrepared && !isCompleted && mp.isPlaying) mp.pause() }
        }

        fun resume() {
            isForeground = true
            val mp = player ?: return
            runCatching { if (isPrepared && !isCompleted && !mp.isPlaying) mp.start() }
        }

        /** Единственное место освобождения общего `MediaPlayer` — выход из приложения. */
        fun shutdown() {
            releaseSurface()
            viewRef = null
            player?.let { mp ->
                runCatching { mp.setOnPreparedListener(null) }
                runCatching { mp.setOnCompletionListener(null) }
                runCatching { mp.setOnErrorListener(null) }
                runCatching { mp.setOnVideoSizeChangedListener(null) }
                runCatching { if (mp.isPlaying) mp.stop() }
                runCatching { mp.release() }
            }
            player = null
            isPrepared = false
        }

        private fun bindSurface(texture: SurfaceTexture) {
            releaseSurface()
            val created = Surface(texture)
            surface = created
            runCatching { player?.setSurface(created) }
            startIfReady()
        }

        private fun startIfReady() {
            val mp = player ?: return
            if (!isPrepared || isCompleted || !isForeground || surface == null) return
            runCatching { if (!mp.isPlaying) mp.start() }
        }

        private fun releaseSurface() {
            // Сначала снимаем поверхность с плеера, только потом освобождаем её сами:
            // иначе MediaPlayer может остаться с висящей ссылкой на чужой Surface.
            runCatching { player?.setSurface(null) }
            surface?.let { runCatching { it.release() } }
            surface = null
        }

        /** Вписать кадр целиком в TextureView, не растягивая: меньший из масштабов. */
        private fun fitToView() {
            val view = viewRef?.get() ?: return
            val viewWidth = view.width
            val viewHeight = view.height
            if (viewWidth <= 0 || viewHeight <= 0) return
            if (videoWidth <= 0 || videoHeight <= 0) {
                view.setTransform(Matrix())
                return
            }
            val scaleX = viewWidth.toFloat() / videoWidth
            val scaleY = viewHeight.toFloat() / videoHeight
            val fit = minOf(scaleX, scaleY)
            view.setTransform(
                Matrix().apply {
                    setScale(
                        fit / scaleX,
                        fit / scaleY,
                        viewWidth / 2f,
                        viewHeight / 2f
                    )
                }
            )
        }

        /** Один общий источник — общий колбэк завершения, актуальный всегда. */
        fun setEndedCallback(callback: (() -> Unit)?) {
            onEnded = callback
        }
    }
}
