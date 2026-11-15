package com.koch.weather.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.annotation.AnyRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.koch.weather.data.model.AdCatalog
import com.koch.weather.data.model.AdCreative
import com.koch.weather.data.model.AdMediaType
import com.koch.weather.ui.theme.AdBuyRed
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Фото: крестик доступен через 3 секунды после открытия поп-апа (ТЗ п. 4). */
private const val CLOSE_DELAY_PHOTO_MS = 3_000L

/** Видео: крестик доступен через 5 секунд (ТЗ п. 4). */
private const val CLOSE_DELAY_VIDEO_MS = 5_000L

/** Подпись «КУПИТЬ СЕЙЧАС!» не появляется раньше 300 мс после медиа (ТЗ п. 2). */
private const val BUY_LABEL_DELAY_MS = 300L

/** Запасной аспект, пока реальные размеры медиа не прочитаны (3:4 — как у «льва»). */
private const val DEFAULT_MEDIA_ASPECT = 3f / 4f

/** Размер крестика: 44 dp — комфортная цель для пальца. */
private val CloseButtonSize = 44.dp

/**
 * Полноэкранный рекламный поп-ап (ТЗ «реклама», п. 4).
 *
 * Показывает креатив из пула [AdCatalog] без надписей и текста: медиа по центру,
 * без обрезки (`ContentScale.Fit`). Единственные элементы интерфейса —
 * подпись «КУПИТЬ СЕЙЧАС!» у трёх фото ([AdCreative.hasBuyLabel]) и крестик,
 * который появляется через 3 с для фото и через 5 с для видео.
 *
 * Автозакрытие — только для видео: когда ролик доиграл до конца, поп-ап
 * закрывается тем же путём, что и крестик ([requestDismiss]). Закрытие планируется
 * через `Handler(Looper.getMainLooper()).post`: колбэк `MediaPlayer` не разрушает
 * диалог внутри себя, иначе на части прошивок (realme/ColorOS) освобождение плеера
 * идёт из его же callback и main thread зависает.
 *
 * Системный «назад» больше не глушится: back закрывает рекламу тем же отложенным
 * путём, что и крестик (пользователь не заперт в полноэкранном поп-апе).
 *
 * Видео-ролик проигрывается целиком через `TextureView` + `MediaPlayer`: без контролов,
 * без паузы и перемотки, со звуком. `TextureView` — обычный View в иерархии, поэтому
 * не создаёт отдельный `SurfaceView`-слой поверх окна `Dialog` и не тормозит
 * композицию кадрами чужого surface (жалоба realme RMX3151: ~3000 пропущенных кадров).
 */
@Composable
fun AdPromoDialog(
    creative: AdCreative,
    onDismiss: () -> Unit
) {
    // Колбэк всегда актуальный: смена лямбды из MainScreen не пересоздаёт диалог.
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    // «Уже закрываем»: крестик, back и конец ролика не должны запустить onDismiss дважды.
    val isDismissing = remember { AtomicBoolean(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Единственная точка закрытия поп-апа. post() гарантирует, что onCompletion
    // плеера вернётся раньше, чем композиция снимет диалог и `DisposableEffect`
    // освободит MediaPlayer/Surface.
    val requestDismiss: () -> Unit = {
        if (isDismissing.compareAndSet(false, true)) {
            mainHandler.post { currentOnDismiss() }
        }
    }

    // Диалог ушёл из композиции раньше очереди — снимаем запланированное закрытие.
    DisposableEffect(Unit) {
        onDispose { mainHandler.removeCallbacksAndMessages(null) }
    }

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Back не глушим: BackHandler ниже и платформенный dismissOnBackPress
            // оба ведут в requestDismiss (флаг «уже закрываем» не даст закрыть дважды).
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            // Фон тянем под системные бары: чёрный поп-ап без светлых полос сверху/снизу.
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Системный «назад» закрывает рекламу так же, как крестик.
            BackHandler(enabled = true) { requestDismiss() }
            AdMediaFrame(
                creative = creative,
                // Видео доиграло — закрываем поп-ап тем же отложенным путём.
                onVideoEnded = requestDismiss,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = KochSpace.xl, vertical = KochSpace.x3l)
            )
            AdCloseButton(
                creativeId = creative.id,
                // Фото — 3 с, видео — 5 с; раньше крестика нет.
                visibleAfterMs = if (creative.type == AdMediaType.VIDEO) {
                    CLOSE_DELAY_VIDEO_MS
                } else {
                    CLOSE_DELAY_PHOTO_MS
                },
                onDismiss = requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(KochSpace.l)
            )
        }
    }
}

/**
 * Медиа поп-апа: укладывается в экран по `Fit` (никакой обрезки), по центру.
 * Под медиа — подпись «КУПИТЬ СЕЙЧАС!» у трёх фото; у видео и «сына» надписей нет.
 * [onVideoEnded] вызывается, когда ролик доиграл до конца (фото его не зовёт).
 */
@Composable
private fun AdMediaFrame(
    creative: AdCreative,
    onVideoEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspect = rememberMediaAspect(creative)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // weight(fill = false) отдаёт медиа всю высоту за вычетом подписи,
        // поэтому «Fit» считается от реального свободного места, а не от экрана.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentAlignment = Alignment.Center
        ) {
            val fitted = fitInside(maxWidth, maxHeight, aspect)
            when (creative.type) {
                AdMediaType.PHOTO -> Image(
                    painter = painterResource(id = creative.mediaRes),
                    contentDescription = "Реклама",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(fitted.width, fitted.height)
                )

                AdMediaType.VIDEO -> AdVideoPlayer(
                    videoRes = creative.mediaRes,
                    onEnded = onVideoEnded,
                    modifier = Modifier.size(fitted.width, fitted.height)
                )
            }
        }
        if (creative.hasBuyLabel) {
            Spacer(Modifier.height(KochSpace.m))
            BuyNowLabel(creativeId = creative.id)
        }
    }
}

/** Размер «Fit»-рамки для медиа с известным аспектом внутри [maxWidth] × [maxHeight]. */
private data class FittedSize(val width: Dp, val height: Dp)

private fun fitInside(maxWidth: Dp, maxHeight: Dp, aspect: Float): FittedSize {
    if (!aspect.isFinite() || aspect <= 0f) return FittedSize(maxWidth, maxHeight)
    val heightFromWidth = maxWidth / aspect
    return if (heightFromWidth <= maxHeight) {
        FittedSize(maxWidth, heightFromWidth)
    } else {
        FittedSize(maxHeight * aspect, maxHeight)
    }
}

/**
 * Реальный аспект медиа (ширина / высота). Читается в IO: заголовок JPEG и
 * метаданные MP4 — без декодирования полного кадра. `produceState` привязан
 * к `creative.id`: обычные рекомпозиции поп-апа не перезапускают чтение
 * (`MediaMetadataRetriever` и `BitmapFactory` — дорогие нативные вызовы, им
 * нечего делать на главном потоке). До ответа — [DEFAULT_MEDIA_ASPECT];
 * при ошибке остаётся он же, поэтому поп-ап не «прыгает» и не ломается.
 */
@Composable
private fun rememberMediaAspect(creative: AdCreative): Float {
    val context = LocalContext.current.applicationContext
    val aspect by produceState(
        initialValue = DEFAULT_MEDIA_ASPECT,
        key1 = creative.id,
        key2 = creative.mediaRes
    ) {
        value = withContext(Dispatchers.IO) {
            when (creative.type) {
                AdMediaType.PHOTO -> readPhotoAspect(context, creative.mediaRes)
                AdMediaType.VIDEO -> readVideoAspect(context, creative.mediaRes)
            }
        } ?: DEFAULT_MEDIA_ASPECT
    }
    return aspect
}

private fun readPhotoAspect(context: Context, @AnyRes resId: Int): Float? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, options)
    return if (options.outWidth > 0 && options.outHeight > 0) {
        options.outWidth.toFloat() / options.outHeight
    } else {
        null
    }
}

private fun readVideoAspect(context: Context, @AnyRes resId: Int): Float? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, resourceUri(context, resId))
        val width = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
        )?.toFloatOrNull()
        val height = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
        )?.toFloatOrNull()
        if (width != null && height != null && width > 0f && height > 0f) width / height else null
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun resourceUri(context: Context, @AnyRes resId: Int): Uri =
    Uri.parse("android.resource://${context.packageName}/$resId")

/**
 * Ролик рекламы на `TextureView` + `MediaPlayer`: без контролов, паузы и перемотки.
 *
 * Почему не `VideoView`: он рисует кадр в отдельный `SurfaceView`-слой поверх окна
 * `Dialog`. На realme RMX3151 композиция такого слоя давала тысячи пропущенных кадров
 * и «залипание» после ролика. `TextureView` — обычный View в иерархии, его кадр
 * композитится вместе с остальным UI.
 *
 * Жизненный цикл: при уходе приложения в фон — пауза, при возврате — продолжение
 * с того же места (звук не глушим); `DisposableEffect` при закрытии поп-апа
 * освобождает `MediaPlayer` и `Surface`. Освобождение идемпотентно, слушатели
 * снимаются до `release()`. Когда ролик доиграл до конца, вызывается [onEnded] —
 * получатель сам решает, как закрывать поп-ап (в `AdPromoDialog` — через `post`).
 */
@Composable
internal fun AdVideoPlayer(
    @AnyRes videoRes: Int,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    // Сессия создаётся один раз на ролик, а колбэк всегда читается актуальный:
    // смена лямбды между рекомпозициями не пересоздаёт плеер.
    val currentOnEnded by rememberUpdatedState(onEnded)
    val controller = remember(videoRes) {
        AdVideoController(
            context = context,
            videoRes = videoRes,
            onEnded = { currentOnEnded() }
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> TextureView(ctx).also(controller::attach) }
    )

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> controller.pause()
                Lifecycle.Event.ON_START -> controller.resume()
                else -> Unit
            }
        }
        // Локальный lifecycle ловит уход в фон, если диалог получает собственный
        // lifecycle-owner; ProcessLifecycleOwner — гарантированный дубль на Home/звонок.
        lifecycleOwner.lifecycle.addObserver(observer)
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
            // Плеер освобождается только здесь: к этому моменту колбэк MediaPlayer
            // (включая onCompletion) уже вернулся — диалог закрывается через post().
            controller.release()
        }
    }
}

/**
 * Плеер одного ролика: `MediaPlayer` + `Surface` на `TextureView`.
 *
 * Все переходы состояния защищены [isReleased]/[isPrepared] и `runCatching`:
 * поздний колбэк `prepareAsync` не запустит освобождённый плеер. Слушатели
 * снимаются перед `release()`, поэтому `onCompletion` не срабатывает на уже
 * снятом диалоге.
 */
private class AdVideoController(
    private val context: Context,
    @param:AnyRes private val videoRes: Int,
    private val onEnded: () -> Unit
) {

    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var isPrepared = false
    private var isCompleted = false
    private var isForeground = true
    private var isReleased = false
    private var viewRef: WeakReference<TextureView>? = null

    /** Подключить `TextureView`: ролик стартует, как только появится поверхность. */
    fun attach(view: TextureView) {
        viewRef = WeakReference(view)
        if (isReleased) return
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (isReleased) return
                start(surfaceTexture)
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
                stop()
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
        }
        // TextureView уже с готовой поверхностью — колбэк available повторно не придёт.
        if (view.isAvailable) {
            view.surfaceTexture?.let(::start)
        }
    }

    private fun start(surfaceTexture: SurfaceTexture) {
        stop()
        if (isReleased) return
        isCompleted = false
        val mp = MediaPlayer()
        player = mp
        runCatching {
            surface = Surface(surfaceTexture)
            mp.setSurface(surface)
            // Кадр вписывается целиком, без растяжения. Матрица ниже дублирует это
            // для устройств, где MediaPlayer игнорирует scaling mode у TextureView.
            runCatching { mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT) }
            applyAudioAttributes(mp)
            context.resources.openRawResourceFd(videoRes)?.use { descriptor ->
                mp.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            } ?: error("res/raw/$videoRes не найден")
            mp.isLooping = false
            installListeners(mp)
            mp.prepareAsync()
        }.onFailure { stop() }
    }

    private fun installListeners(mp: MediaPlayer) {
        mp.setOnVideoSizeChangedListener { _, width, height ->
            videoWidth = width
            videoHeight = height
            fitToView()
        }
        mp.setOnPreparedListener { prepared ->
            if (isReleased || player !== prepared) return@setOnPreparedListener
            isPrepared = true
            // Звук ролика: полная громкость, медиа-поток как у фильма.
            runCatching { prepared.setVolume(1f, 1f) }
            // Страховка на случай, если onVideoSizeChanged не пришёл: берём размеры
            // уже подготовленного плеера, иначе кадр мог бы растянуться на всю рамку.
            if (videoWidth <= 0 || videoHeight <= 0) {
                runCatching {
                    videoWidth = prepared.videoWidth
                    videoHeight = prepared.videoHeight
                }
            }
            fitToView()
            if (isForeground && !isCompleted) {
                runCatching { prepared.start() }
            }
        }
        mp.setOnCompletionListener { completed ->
            if (isReleased || player !== completed) return@setOnCompletionListener
            isCompleted = true
            // Сам диалог здесь не закрываем: получатель планирует закрытие (post).
            onEnded()
        }
        mp.setOnErrorListener { failed, _, _ ->
            // Битый/неподдерживаемый файл не роняет поп-ап: остаётся чёрная подложка.
            if (isReleased || player !== failed) return@setOnErrorListener false
            stop()
            true
        }
    }

    private fun applyAudioAttributes(mp: MediaPlayer) {
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
        }
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

    /** Пауза при уходе в фон: ролик не должен звучать из кармана. */
    fun pause() {
        isForeground = false
        val mp = player ?: return
        runCatching { if (isPrepared && !isCompleted && mp.isPlaying) mp.pause() }
    }

    /** Возврат из фона: продолжаем с того же места, но не перезапускаем законченный ролик. */
    fun resume() {
        isForeground = true
        val mp = player ?: return
        runCatching { if (isPrepared && !isCompleted && !mp.isPlaying) mp.start() }
    }

    /** Освобождение плеера (закрытие поп-апа/выход из композиции). Идемпотентно. */
    fun release() {
        if (isReleased) return
        isReleased = true
        stop()
        viewRef?.get()?.surfaceTextureListener = null
        viewRef = null
    }

    private fun stop() {
        isPrepared = false
        player?.let { mp ->
            // Слушатели снимаем ДО release(): поздний колбэк не должен тронуть
            // освобождённый плеер и не может закрыть уже снятый диалог.
            runCatching { mp.setOnPreparedListener(null) }
            runCatching { mp.setOnCompletionListener(null) }
            runCatching { mp.setOnErrorListener(null) }
            runCatching { mp.setOnVideoSizeChangedListener(null) }
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.release() }
        }
        player = null
        surface?.let { runCatching { it.release() } }
        surface = null
        videoWidth = 0
        videoHeight = 0
    }
}

/**
 * Красная подпись «КУПИТЬ СЕЙЧАС!» (ТЗ п. 2): жирная, подчёркнутая, снизу от медиа.
 * Появляется не раньше 300 мс после медиа и слегка пружинисто впрыгивает
 * в кадр — `spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)`
 * по `translationY` и `alpha`.
 */
@Composable
private fun BuyNowLabel(creativeId: String) {
    var isShown by remember(creativeId) { mutableStateOf(false) }
    LaunchedEffect(creativeId) {
        delay(BUY_LABEL_DELAY_MS)
        isShown = true
    }
    val translationY by animateDpAsState(
        targetValue = if (isShown) 0.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buy_label_translation"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isShown) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buy_label_alpha"
    )
    Text(
        text = AdCatalog.BUY_LABEL_RU,
        style = KochType.titleSheet,
        color = AdBuyRed,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.graphicsLayer {
            this.translationY = translationY.toPx()
            // Пружина по alpha даёт перелёт > 1 — альфа обязана остаться в [0, 1].
            this.alpha = labelAlpha.coerceIn(0f, 1f)
        }
    )
}

/**
 * Крестик поп-апа в правом верхнем углу. Появляется через [visibleAfterMs]
 * (3 с фото / 5 с видео), после чего остаётся до конца показа.
 */
@Composable
private fun AdCloseButton(
    creativeId: String,
    visibleAfterMs: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember(creativeId) { mutableStateOf(false) }
    LaunchedEffect(creativeId) {
        delay(visibleAfterMs)
        isVisible = true
    }
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
            scaleIn(animationSpec = tween(durationMillis = 220), initialScale = 0.8f)
    ) {
        Box(
            modifier = Modifier
                .size(CloseButtonSize)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(0.8.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Закрыть рекламу",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
