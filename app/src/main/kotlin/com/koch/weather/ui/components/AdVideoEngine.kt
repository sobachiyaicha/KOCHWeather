package com.koch.weather.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.annotation.AnyRes
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.core.view.doOnLayout
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.koch.weather.data.model.AdCreative
import com.koch.weather.data.model.AdMediaType

/** Тег логов: видно в `adb logcat -s AdVideoEngine`, какие ветки реально живут. */
private const val TAG = "AdVideoEngine"

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
 * Один тёплый плеер и одна постоянная выходная поверхность на процесс.
 *
 * Почему так. На realme RMX3151 (Android 12, MediaTek) `CCodec` при **каждом**
 * создании кодека и при **каждой смене выходной поверхности** ждёт несуществующий
 * сервис `AtlasService` ~5 с (реальное имя сервиса — `multimediaDaemon`), а
 * legacy OMX-декодеров на устройстве нет вообще (`OMX.google.h264.decoder` —
 * алиас C2). Поэтому единственный способ убрать задержку из показа рекламы —
 * не пересоздавать кодек и не менять поверхность:
 *
 * 1. `TextureView` создаётся **один раз** в оверлее `AdPromoOverlay` и **всегда**
 *    занимает весь экран — и показанный, и скрытый (`alpha = 0`). Размер View /
 *    `SurfaceTexture` не меняется никогда: изменение размера буфера поверхности
 *    заставляет видеорендерер переконфигурировать кодек (те самые 5 с через
 *    0,9 с после первого кадра).
 * 2. `ExoPlayer` создаётся **один раз** ([warmUp] на старте приложения, после
 *    первого кадра UI) и привязывается к `Surface` этого `TextureView`
 *    единожды ([attachHostView] / [bindSurface]) — повторный `setVideoSurface`
 *    с той же поверхностью не вызывается вовсе.
 * 3. При старте сразу ставится первый рекламный ролик и вызывается `prepare()`:
 *    кодек конфигурируется на запуске приложения, а не в момент показа.
 * 4. Показ: другой ролик — `setMediaItem` + `prepare()` на **том же** плеере
 *    (без `stop()`: он освобождает кодек), затем `seekTo(0)` + `play()`.
 *    Закрытие — только `pause()` ([pause]).
 * 5. Размер кадра известен заранее (передаётся в [AdCreative] вместе с `resId`),
 *    поэтому fit-матрица выставляется до `play()` и не сбрасывается между
 *    показами: первый же кадр идёт с letterbox, а не растянутым.
 * 6. Освобождение — только [shutdown] из `MainActivity.onDestroy(isFinishing)`,
 *    строго `clearVideoSurface()` **до** `release()`.
 *
 * Все методы, кроме [pausePlayback], вызываются с main-потока: `ExoPlayer`
 * требует, чтобы `release()`/`setVideoSurface()` шли с его application-потока.
 */
object AdVideoEngine {

    /** Единственный плеер процесса (создаётся лениво, живёт до [shutdown]). */
    private var player: ExoPlayer? = null

    /** Постоянный `TextureView` оверлея; его `SurfaceTexture` не пересоздаётся. */
    private var hostView: TextureView? = null

    /** `Surface`, собранный из `SurfaceTexture` постоянного `TextureView` — одна на процесс. */
    private var hostSurface: Surface? = null

    /** Текущий `SurfaceTexture` постоянного `TextureView` (владелец — сам View). */
    private var hostTexture: SurfaceTexture? = null

    /**
     * Поверхность, уже отданная плееру. Повторный `setVideoSurface` с тем же
     * `Surface` — лишний `MediaCodec.setOutputSurface` (на MediaTek он
     * пересоздаёт кодек), поэтому привязка идёт ровно один раз.
     */
    private var boundSurface: Surface? = null

    /** `R.raw`-ресурс, под который подготовлен текущий плеер. 0 — ничего не готово. */
    private var preparedRes: Int = 0

    /** Идёт ли показ прямо сейчас: только тогда `ON_START` возобновляет звук. */
    private var adActive = false

    /** Колбэк «ролик доиграл»; выставляется оверлеем на время показа. */
    private var endedCallback: (() -> Unit)? = null

    /**
     * Размер кадра активного ролика. Известен заранее (из [AdCreative]) и не
     * сбрасывается между показами: матрица `TextureView` не должна «прыгать».
     */
    private var frameWidth: Int = 0
    private var frameHeight: Int = 0

    /** Ждём первый layout `TextureView`, чтобы посчитать матрицу по его размерам. */
    private var fitPending = false

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Плеер уже создан и готов принять ролик. */
    val isWarm: Boolean
        get() = player != null

    /**
     * Подключает постоянный `TextureView` оверлея. Вызывается **один раз** из
     * `AndroidView.factory`; при пересоздании Activity — один раз на новый View.
     *
     * Плеер привязывается к `Surface` этого View ровно один раз ([bindSurface]):
     * дальше ни один показ рекламы не меняет выход кодека, поэтому CCodec не
     * проходит повторную инициализацию и не ждёт `AtlasService`.
     */
    @MainThread
    fun attachHostView(view: TextureView) {
        if (hostView === view && hostSurface != null) return
        hostView = view
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                bindSurface(surfaceTexture)
                applyFitTransform()
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                // Размер View постоянный (всегда fullscreen), сюда попадаем только
                // на системных изменениях раскладки; кадр вписываем заново.
                applyFitTransform()
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                // Сначала отвязываем кодек от поверхности, иначе он получит уже
                // освобождённую `Surface` (`The surface has been released`).
                // Только для актуальной текстуры: при пересоздании Activity новый
                // View успевает привязаться раньше, чем старый приходит сюда.
                if (hostTexture === surfaceTexture) {
                    runCatching { player?.clearVideoSurface() }
                    boundSurface = null
                    releaseSurface()
                    hostView = null
                }
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
        }
        // View уже с готовой поверхностью — колбэк `available` повторно не придёт.
        if (view.isAvailable) {
            view.surfaceTexture?.let { texture -> bindSurface(texture) }
        }
        applyFitTransform()
    }

    /**
     * Прогрев на старте приложения (после первого кадра UI): создаёт `ExoPlayer`,
     * ставит первый рекламный ролик, `prepare()` и привязывает постоянную
     * поверхность. Пять секунд ожидания кодека уходят в запуск приложения.
     */
    @MainThread
    fun warmUp(context: Context, creative: AdCreative) {
        if (creative.type != AdMediaType.VIDEO) return
        if (player != null && preparedRes == creative.mediaRes) {
            applyFrameSize(creative)
            bindSurfaceIfPossible()
            return
        }
        arm(context, creative)
        Log.i(TAG, "warm-up: ExoPlayer prepared for res=${creative.mediaRes}")
    }

    /**
     * Предзагрузка ролика до показа оверлея (вызывается из `requestAdPopup()`):
     * тот же ролик — ничего не пересоздаётся, другой — `setMediaItem` + `prepare()`
     * на существующем плеере. Фото-креатив просто ставит воспроизведение на паузу.
     */
    @MainThread
    fun prepare(context: Context, creative: AdCreative) {
        if (creative.type != AdMediaType.VIDEO) {
            pause()
            return
        }
        arm(context, creative)
    }

    /**
     * Показ ролика: тот же плеер, та же поверхность, та же матрица вписывания.
     *
     * Если нужен другой ролик — `setMediaItem` + `prepare()` на существующем
     * экземпляре (без создания нового), затем `seekTo(0)` и `play()`: первый кадр
     * появляется сразу, без индикаторов загрузки. Матрица выставляется в [arm]
     * **до** `play()`, поэтому первый кадр уже вписан.
     */
    @MainThread
    fun start(context: Context, creative: AdCreative, onEnded: () -> Unit) {
        if (creative.type != AdMediaType.VIDEO) {
            pause()
            return
        }
        endedCallback = onEnded
        arm(context, creative)
        val current = player ?: return
        adActive = true
        runCatching {
            current.seekTo(0)
            current.playWhenReady = true
            current.play()
        }.onFailure { Log.w(TAG, "start playback", it) }
    }

    /**
     * Закрытие показа: только `pause()`.
     *
     * Никакого `ExoPlayer.stop()`/`release()`: `stop()` переводит плеер в `IDLE`
     * и освобождает кодек, после чего следующий показ снова платит ~5 с ожидания
     * `AtlasService` (на realme RMX3151 это давало 10 с чёрного экрана без
     * единого кадра). Плеер, выходная поверхность и fit-матрица остаются живыми
     * до [shutdown].
     */
    @MainThread
    fun pause() {
        endedCallback = null
        adActive = false
        val current = player ?: return
        runCatching {
            current.playWhenReady = false
            current.pause()
        }.onFailure { Log.w(TAG, "pause playback", it) }
    }

    /** Пауза на `ON_STOP`: ролик не должен звучать из кармана. */
    fun pausePlayback() {
        runCatching { player?.pause() }
    }

    /** Возобновление на `ON_START` — только если оверлей действительно открыт. */
    fun resumePlayback() {
        if (!adActive) return
        val current = player ?: return
        runCatching {
            // Законченный ролик не перезапускаем: оверлей уже закрывается.
            if (current.playbackState != Player.STATE_ENDED) {
                current.playWhenReady = true
                current.play()
            }
        }
    }

    /**
     * Выход из приложения — единственное место освобождения плеера и поверхности.
     *
     * Порядок строгий: `clearVideoSurface()` **до** `release()`, иначе
     * `MediaCodec.setOutputSurface` получит уже освобождённую `Surface`
     * (`IllegalArgumentException: The surface has been released`).
     */
    @MainThread
    fun shutdown() {
        endedCallback = null
        adActive = false
        preparedRes = 0
        frameWidth = 0
        frameHeight = 0
        fitPending = false
        boundSurface = null
        val current = player
        player = null
        if (current != null) {
            runCatching { current.pause() }
            runCatching { current.clearVideoSurface() }
            runCatching { current.release() }
                .onFailure { Log.w(TAG, "ExoPlayer.release()", it) }
        }
        releaseSurface()
        hostView = null
        Log.i(TAG, "shutdown: player released")
    }

    // ------------------------------------------------------------------------
    // Внутреннее
    // ------------------------------------------------------------------------

    /**
     * Готовит ролик на тёплом плеере: `setMediaItem` + `prepare()`.
     * Новый экземпляр создаётся только если плеера ещё нет (первый запуск).
     *
     * Здесь нет `player.stop()`: он освобождает кодек и следующий показ снова
     * ждал бы `AtlasService`. Смена элемента плейлиста на живом плеере этого
     * не делает, а поверхность остаётся той же самой.
     */
    @MainThread
    private fun arm(context: Context, creative: AdCreative) {
        val videoRes = creative.mediaRes
        if (videoRes == 0) return
        val current = ensurePlayer(context) ?: return
        // Размер кадра известен заранее: матрица вписывания ставится до play().
        applyFrameSize(creative)
        // Поверхность отдаём кодеку до prepare(): MediaCodecVideoRenderer
        // конфигурирует декодер сразу, как только она появилась.
        bindSurfaceIfPossible()
        if (preparedRes == videoRes) return
        val switched = runCatching {
            current.playWhenReady = false
            current.setMediaItem(MediaItem.fromUri(resourceUri(context, videoRes)))
            current.prepare()
        }.isSuccess
        if (switched) {
            preparedRes = videoRes
            Log.i(TAG, "armed ExoPlayer for res=$videoRes")
        } else {
            Log.w(TAG, "не удалось подготовить ролик res=$videoRes")
        }
    }

    /** Запоминает размер кадра ролика и сразу пересчитывает fit-матрицу. */
    private fun applyFrameSize(creative: AdCreative) {
        if (creative.videoWidth > 0 && creative.videoHeight > 0) {
            frameWidth = creative.videoWidth
            frameHeight = creative.videoHeight
        }
        applyFitTransform()
    }

    /** Ленивое создание единственного плеера процесса. */
    @MainThread
    private fun ensurePlayer(context: Context): ExoPlayer? {
        player?.let { return it }
        val created = runCatching { buildPlayer(context.applicationContext) }
            .onFailure { Log.w(TAG, "ExoPlayer недоступен", it) }
            .getOrNull() ?: return null
        player = created
        // Постоянную поверхность отдаём сразу: кодек конфигурируется один раз
        // на процесс, привязка больше не меняется.
        bindSurfaceIfPossible()
        Log.i(TAG, "created ExoPlayer (one per process)")
        return created
    }

    @OptIn(UnstableApi::class)
    private fun buildPlayer(context: Context): ExoPlayer {
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
        val built = ExoPlayer.Builder(context, renderersFactory).build()
        // Звук рекламы включён: полная громкость, медиа-поток «как в кино».
        built.volume = 1f
        built.repeatMode = Player.REPEAT_MODE_OFF
        built.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ false
        )
        built.playWhenReady = false
        built.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_ENDED) return
                val callback = endedCallback
                Log.i(TAG, "playback ended")
                // Колбэк плеера не должен закрывать оверлей внутри себя: сначала
                // возвращаемся в цикл main, потом зовём оверлей (как крестик).
                if (callback != null) mainHandler.post { endedCallback?.invoke() }
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width <= 0 || size.height <= 0) return
                // Декодер подтвердил размер кадра: для наших роликов он совпадает
                // с заранее известным, кадр уже вписан и не «прыгает».
                frameWidth = size.width
                frameHeight = size.height
                applyFitTransform()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "player error", error)
            }
        })
        return built
    }

    /** Создаёт (один раз) `Surface` постоянного `TextureView` и отдаёт его плееру. */
    private fun bindSurface(texture: SurfaceTexture) {
        if (hostTexture === texture && hostSurface != null) {
            bindSurfaceIfPossible()
            return
        }
        releaseSurface()
        hostTexture = texture
        hostSurface = Surface(texture)
        bindSurfaceIfPossible()
        Log.i(TAG, "bound permanent TextureView surface")
    }

    /**
     * Отдаёт плееру постоянную поверхность ровно один раз.
     *
     * Повторный `setVideoSurface` с тем же `Surface` — это ещё один
     * `MediaCodec.setOutputSurface`, а на MediaTek/ColorOS он пересоздаёт кодек
     * (5 с `AtlasService`). Поэтому привязка идемпотентна.
     */
    private fun bindSurfaceIfPossible() {
        val current = player ?: return
        val surface = hostSurface ?: return
        if (boundSurface === surface) return
        runCatching { current.setVideoSurface(surface) }
            .onSuccess {
                boundSurface = surface
                // В logcat видно, что выход кодека привязан ровно один раз:
                // повторных `setVideoSurface`/`configure` на показах быть не должно.
                Log.i(TAG, "setVideoSurface: permanent surface bound (once per surface)")
            }
            .onFailure { Log.w(TAG, "setVideoSurface", it) }
    }

    /**
     * Вписать кадр целиком в постоянный `TextureView`, не растягивая.
     *
     * Матрица считается из заранее известного размера кадра и текущего размера
     * View (он всегда fullscreen), поэтому первый же кадр идёт с letterbox.
     * Пока размер кадра неизвестен, матрица не сбрасывается: прежнее вписывание
     * лучше растянутого кадра.
     */
    private fun applyFitTransform() {
        val view = hostView ?: return
        val width = frameWidth
        val height = frameHeight
        if (width <= 0 || height <= 0) return
        val viewWidth = view.width
        val viewHeight = view.height
        if (viewWidth <= 0 || viewHeight <= 0) {
            applyFitAfterLayout(view)
            return
        }
        val videoAspect = width.toFloat() / height
        val viewAspect = viewWidth.toFloat() / viewHeight
        val scaleX: Float
        val scaleY: Float
        if (videoAspect > viewAspect) {
            // Ролик шире экрана — вписываем по ширине, поля сверху и снизу.
            scaleX = 1f
            scaleY = viewAspect / videoAspect
        } else {
            // Ролик выше экрана — вписываем по высоте, поля слева и справа.
            scaleX = videoAspect / viewAspect
            scaleY = 1f
        }
        view.setTransform(
            Matrix().apply {
                setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
            }
        )
        Log.i(TAG, "fit frame ${width}x$height into ${viewWidth}x$viewHeight")
    }

    /** Матрица до первого layout: размеров View ещё нет, ждём раскладку. */
    private fun applyFitAfterLayout(view: TextureView) {
        if (fitPending) return
        fitPending = true
        view.doOnLayout {
            fitPending = false
            applyFitTransform()
        }
    }

    /** Освобождает наш `Surface` (сам `SurfaceTexture` принадлежит `TextureView`). */
    private fun releaseSurface() {
        hostSurface?.let { surface -> runCatching { surface.release() } }
        hostSurface = null
        hostTexture = null
        boundSurface = null
    }

    private fun resourceUri(context: Context, @AnyRes resId: Int): Uri =
        Uri.parse("android.resource://${context.packageName}/$resId")
}
