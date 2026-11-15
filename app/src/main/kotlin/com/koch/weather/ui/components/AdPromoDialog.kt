package com.koch.weather.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.annotation.AnyRes
import androidx.annotation.OptIn
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.koch.weather.data.model.AdCatalog
import com.koch.weather.data.model.AdCreative
import com.koch.weather.data.model.AdMediaType
import com.koch.weather.ui.theme.AdBuyRed
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
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
 * через `Handler(Looper.getMainLooper()).post`: колбэк плеера не разрушает
 * диалог внутри себя, иначе на части прошивок (realme/ColorOS) освобождение плеера
 * идёт из его же callback и main thread зависает.
 *
 * Системный «назад» больше не глушится: back закрывает рекламу тем же отложенным
 * путём, что и крестик (пользователь не заперт в полноэкранном поп-апе).
 *
 * Видео-ролик проигрывается целиком через AndroidX Media3 (ExoPlayer): без контролов,
 * без паузы и перемотки, со звуком. Плеер не создаётся здесь — его готовит
 * [AdVideoEngine] заранее (`requestAdPopup`), поэтому первый кадр появляется сразу,
 * без индикаторов загрузки. `PlayerView` отдаёт кадр либо в `SurfaceView` (дефолтный
 * `surface_type`, не блокирует main), либо в `TextureView` fallback-режима.
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

    // Единственная точка закрытия поп-апа. post() гарантирует, что колбэк плеера
    // (в т.ч. `Player.Listener.onPlaybackStateChanged(STATE_ENDED)`) вернётся раньше,
    // чем композиция снимет диалог и его `DisposableEffect` отвяжет плеер от поверхности.
    // parkCurrent() вызывается ещё при живой View: выход кодека уходит на фиктивную
    // поверхность движка, поэтому teardown PlayerView не отдаст кодеку освобождённую
    // Surface (иначе `MediaCodec.setOutputSurface` падает с
    // `IllegalArgumentException: The surface has been released`).
    val requestDismiss: () -> Unit = {
        if (isDismissing.compareAndSet(false, true)) {
            AdVideoEngine.parkCurrent()
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

/** Ссылка-держатель `PlayerView` для `AndroidView`: без записи в snapshot-состояние. */
private class PlayerViewRef {
    var view: PlayerView? = null
}

/** Ссылка-держатель `TextureView` fallback-режима (тоже вне snapshot-состояния). */
private class TextureViewRef {
    var view: TextureView? = null
}

/**
 * Ролик рекламы поп-апа на AndroidX Media3.
 *
 * Плеер здесь **не создаётся и не готовится**: его заранее готовит [AdVideoEngine]
 * (вызов из `MainScreenViewModel.requestAdPopup()` в момент стирания облаков).
 * Диалог только выбирает ветку по фактическому состоянию движка:
 * - [AdVideoEngine.Renderer.EXOPLAYER] — основной путь: `PlayerView` (дефолтный
 *   `surface_type`, контролов и индикаторов нет);
 * - [AdVideoEngine.Renderer.LEGACY_MEDIA_PLAYER] — fallback: один общий `MediaPlayer`
 *   на процесс + `TextureView`, освобождается только при выходе из приложения;
 * - [AdVideoEngine.Renderer.NONE] — ролик не поднялся: чёрная подложка, поп-ап живёт.
 *
 * [AdVideoEngine.ensurePrepared] в `remember` — страховка для прямого показа диалога
 * без предзагрузки; в штатном пути это no-op (плеер уже подготовлен движком).
 *
 * Воспроизведение: без паузы, перемотки и жестов, звук включён. Жизненный цикл:
 * `ON_STOP` — пауза, `ON_START` — продолжение с того же места. Конец ролика отдаётся
 * в [onEnded]; как закрывать поп-ап, решает вызывающий (через `requestDismiss`).
 *
 * Освобождение: плеер **не освобождается** при закрытии. `DisposableEffect.onDispose`
 * сначала переключает выход кодека на фиктивную поверхность движка
 * ([AdVideoEngine.parkPlayer]), затем отвязывает `PlayerView`. Плеер остаётся тёплым
 * для следующего показа и освобождается движком только через 60 с простоя
 * или в `MainActivity.onDestroy(isFinishing)` — без нативного teardown в кадре
 * закрытия (в отличие от `MediaPlayer.release()`, который на realme RMX3151
 * держал main 9–20 с).
 */
@Composable
internal fun AdVideoPlayer(
    @AnyRes videoRes: Int,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    // Штатно ролик уже подготовлен: движок вызван из ViewModel до показа диалога.
    val renderer = remember(videoRes) { AdVideoEngine.ensurePrepared(context, videoRes) }
    val player = remember(videoRes, renderer) {
        if (renderer == AdVideoEngine.Renderer.EXOPLAYER) {
            AdVideoEngine.exoPlayerFor(videoRes)
        } else {
            null
        }
    }
    when {
        player != null -> ExoAdVideoPlayer(player = player, onEnded = onEnded, modifier = modifier)
        renderer == AdVideoEngine.Renderer.LEGACY_MEDIA_PLAYER ->
            LegacyAdVideoPlayer(videoRes = videoRes, onEnded = onEnded, modifier = modifier)
        // Ролик не поднялся — вместо индикатора загрузки просто чёрный кадр.
        else -> Box(modifier = modifier.background(Color.Black))
    }
}

/**
 * Основной путь: подготовленный ExoPlayer в `PlayerView`.
 *
 * `useController = false` — ни контролов, ни жестов; `RESIZE_MODE_FIT` — кадр целиком
 * без обрезки; `SHOW_BUFFERING_NEVER` — никаких спиннеров загрузки (пользователь просил
 * без анимаций); чёрный shutter вместо прозрачного первого кадра. `surface_type`
 * не переопределяем: дефолтный `SurfaceView` у ExoPlayer не блокирует main thread.
 */
@OptIn(UnstableApi::class)
@Composable
private fun ExoAdVideoPlayer(
    player: ExoPlayer,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEnded by rememberUpdatedState(onEnded)
    val playerViewRef = remember { PlayerViewRef() }

    // Конец ролика: тот же путь, что у крестика (post + атомарный флаг в диалоге).
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnEnded()
            }
        }
        player.addListener(listener)
        onDispose { runCatching { player.removeListener(listener) } }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                this.player = player
                playerViewRef.view = this
            }
        },
        onRelease = { view ->
            if (playerViewRef.view === view) playerViewRef.view = null
            runCatching { view.player = null }
        }
    )

    // Плеер подготовлен движком до показа: здесь только старт — первый кадр сразу.
    // markVisible() отменяет таймер простоя: тёплый плеер нельзя освободить под показом.
    LaunchedEffect(player) {
        AdVideoEngine.markVisible()
        player.playWhenReady = true
        player.play()
    }

    // Пауза в фоне и продолжение с того же места (звук не глушим).
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> runCatching { player.pause() }
                Lifecycle.Event.ON_START ->
                    if (player.playbackState != Player.STATE_ENDED) {
                        runCatching { player.play() }
                    }
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
        }
    }

    DisposableEffect(player) {
        onDispose {
            // Порядок teardown обязателен: сначала паркуем тёплый плеер
            // (pause + seekTo(0) + выход кодека на фиктивную поверхность),
            // и только потом снимаем PlayerView. Наоборот PlayerView отдал бы
            // кодеку уже освобождённую Surface — `The surface has been released`.
            // Сам release() здесь не вызывается: плеер остаётся тёплым до
            // таймера простоя или выхода из приложения.
            AdVideoEngine.parkPlayer(player)
            runCatching { playerViewRef.view?.player = null }
            playerViewRef.view = null
        }
    }
}

/**
 * Fallback: общий `MediaPlayer` движка + `TextureView` — прежняя схема, но без
 * `release()` в горячем пути. Плеер переживает закрытие поп-апа; между роликами
 * движок делает `pause()` + `seekTo(0)`, а освобождает только на выходе из приложения.
 */
@Composable
private fun LegacyAdVideoPlayer(
    @AnyRes videoRes: Int,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEnded by rememberUpdatedState(onEnded)
    val textureViewRef = remember { TextureViewRef() }

    DisposableEffect(videoRes) {
        AdVideoEngine.bindLegacyEnded { currentOnEnded() }
        onDispose { AdVideoEngine.bindLegacyEnded(null) }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            TextureView(ctx).also { view ->
                textureViewRef.view = view
                AdVideoEngine.attachLegacyView(view)
            }
        },
        onRelease = { view ->
            if (textureViewRef.view === view) textureViewRef.view = null
            AdVideoEngine.detachLegacyView(view)
        }
    )

    DisposableEffect(videoRes, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> AdVideoEngine.pausePlayback()
                Lifecycle.Event.ON_START -> AdVideoEngine.resumePlayback()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
            // Отвязка поверхности; MediaPlayer не освобождаем by design.
            textureViewRef.view?.let { AdVideoEngine.detachLegacyView(it) }
            textureViewRef.view = null
        }
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
