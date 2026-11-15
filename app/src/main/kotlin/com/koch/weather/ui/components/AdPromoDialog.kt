package com.koch.weather.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.VideoView
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
 * закрывается тем же колбэком, что и крестик ([onDismiss]). Если ролик короче
 * задержки крестика, диалог закрывается раньше, и крестик уже не нужен.
 * Фото не закрывается само: пользователь жмёт крестик.
 * Видео-ролик проигрывается целиком через `VideoView`/`MediaPlayer`: без контролов,
 * без паузы и перемотки, со звуком.
 */
@Composable
fun AdPromoDialog(
    creative: AdCreative,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Закрыть рекламу можно только крестиком (или концом видео-ролика):
            // системный «назад» и тап мимо диалог не закрывают.
            dismissOnBackPress = false,
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
            // Системный «назад» не закрывает рекламу и не роняет активити:
            // выход — крестик или автозакрытие после конца видео.
            BackHandler(enabled = true) { }
            AdMediaFrame(
                creative = creative,
                // Видео доиграло — закрываем поп-ап тем же колбэком, что и крестик.
                onVideoEnded = onDismiss,
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
                onDismiss = onDismiss,
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
 * метаданные MP4 — без декодирования полного кадра. До ответа — [DEFAULT_MEDIA_ASPECT];
 * при ошибке остаётся он же, поэтому поп-ап не «прыгает» и не ломается.
 */
@Composable
private fun rememberMediaAspect(creative: AdCreative): Float {
    val context = LocalContext.current
    val aspect by produceState(
        initialValue = DEFAULT_MEDIA_ASPECT,
        key1 = creative.id
    ) {
        value = withContext(Dispatchers.IO) {
            when (creative.type) {
                AdMediaType.PHOTO -> readPhotoAspect(context, creative.mediaRes)
                AdMediaType.VIDEO -> readVideoAspect(context, creative.mediaRes)
            } ?: DEFAULT_MEDIA_ASPECT
        }
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
 * Ролик рекламы: `VideoView` без контролов и жестов.
 *
 * Жизненный цикл: при уходе приложения в фон — пауза, при возврате — продолжение
 * с того же места (звук не глушим); плеер освобождается в `DisposableEffect`
 * при закрытии поп-апа. Когда ролик доиграл до конца, вызывается [onEnded] —
 * поп-ап закрывается сам; повторного запуска нет.
 */
@Composable
internal fun AdVideoPlayer(
    @AnyRes videoRes: Int,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Сессия создаётся один раз на ролик, а колбэк всегда читается актуальный:
    // смена лямбды между рекомпозициями не пересоздаёт плеер.
    val currentOnEnded = rememberUpdatedState(onEnded)
    val player = remember(videoRes) {
        AdVideoSession(videoRes) { currentOnEnded.value() }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context -> player.obtainView(context) },
        modifier = modifier,
        onRelease = { player.release() }
    )

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> player.resume()
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
            player.release()
        }
    }
}

/**
 * Сессия проигрывания одного ролика. Отдельный класс, чтобы `VideoView` переживал
 * рекомпозиции и создавался/освобождался ровно один раз на поп-ап.
 *
 * [onEnded] зовётся один раз, когда ролик доиграл до конца: поп-ап закрывается.
 * После [release] колбэк не срабатывает — слушатели снимаются.
 */
private class AdVideoSession(
    @param:AnyRes private val videoRes: Int,
    private val onEnded: () -> Unit
) {

    private var videoView: VideoView? = null
    private var isCompleted = false
    private var isReleased = false

    fun obtainView(context: Context): VideoView {
        videoView?.let { existing ->
            if (!isReleased) return existing
        }
        val view = VideoView(context).apply {
            // Никаких контролов: панель не ставим, пауза/перемотка жестами не нужны.
            setMediaController(null)
            setOnTouchListener { _, _ -> true }
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = false
                // Звук не глушим: ролик звучит как в оригинале.
                mediaPlayer.setVolume(1f, 1f)
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                start()
            }
            setOnCompletionListener {
                if (isReleased) return@setOnCompletionListener
                // Ролик доиграл: поп-ап закрывается сам (для фото автозакрытия нет).
                isCompleted = true
                onEnded()
            }
            setOnErrorListener { _, _, _ -> true }
            setVideoURI(resourceUri(context, videoRes))
        }
        videoView = view
        isCompleted = false
        isReleased = false
        return view
    }

    /** Пауза при уходе в фон: ролик не должен звучать из кармана. */
    fun pause() {
        val view = videoView ?: return
        if (!isCompleted && view.isPlaying) view.pause()
    }

    /** Возврат из фона: продолжаем с того же места, но не перезапускаем законченный ролик. */
    fun resume() {
        val view = videoView ?: return
        if (!isCompleted && !view.isPlaying) view.start()
    }

    /** Освобождение плеера (закрытие поп-апа/выход из композиции). Идемпотентно. */
    fun release() {
        if (isReleased) return
        isReleased = true
        videoView?.let { view ->
            view.setOnPreparedListener(null)
            view.setOnCompletionListener(null)
            view.setOnErrorListener(null)
            view.stopPlayback()
        }
        videoView = null
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
