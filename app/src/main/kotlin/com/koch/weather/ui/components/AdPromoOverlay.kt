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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
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

/** Фото: крестик доступен через 3 секунды после открытия рекламы (ТЗ п. 4). */
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
 * Полноэкранная реклама **оверлеем в окне `MainScreen`** (ТЗ «реклама», п. 4).
 *
 * Больше не `Dialog`: отдельное окно создавалось и уничтожалось на каждый показ,
 * а вместе с ним пересоздавались `TextureView` и выходная поверхность кодека.
 * На realme RMX3151 (Android 12, MediaTek) `CCodec` при **каждой** смене
 * поверхности и при изменении размера буфера `SurfaceTexture` ждёт
 * несуществующий `AtlasService` 5 с, поэтому оверлей живёт в композиции
 * постоянно, а размер его `TextureView` **не меняется никогда**:
 *
 * - контейнер всегда на весь экран; скрытый — `alpha = 0` у самого View, без
 *   фона, без `clickable` и без `pointerInput`, плюс `zIndex = -1` (вне
 *   `isVisible` этого узла нет в hit-test, тапы идут в `MainScreen`);
 * - постоянный `TextureView` ([AdVideoTextureHost]) создаётся один раз и всегда
 *   занимает ту же площадь — весь экран: ни `1×1 dp`, ни смены размера между
 *   скрытым и показанным состоянием. Буфер поверхности не меняется, поэтому
 *   кодек не переконфигурируется (иначе — фриз ~5 с через 0,9 с после первого
 *   кадра);
 * - фон чёрный, индикаторов загрузки нет; fit-матрица (`letterbox`) выставляется
 *   движком до `play()` по заранее известному размеру ролика, поэтому первый же
 *   кадр вписан, а не растянут.
 *
 * Функционал прежний: медиа по центру без обрезки, крестик через 3 с (фото) или
 * 5 с (видео), автозакрытие после конца ролика, системный back закрывает,
 * никаких контролов воспроизведения. Фото-креативы показываются как раньше
 * ([PhotoAdFrame] + подпись «КУПИТЬ СЕЙЧАС!» у трёх фото).
 *
 * @param creative текущий креатив (при скрытом оверлее — последний показанный)
 * @param isVisible открыта ли реклама сейчас (состояние из ViewModel)
 * @param onDismiss закрытие: крестик, back или конец ролика
 */
@Composable
fun AdPromoOverlay(
    creative: AdCreative,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    // Колбэк всегда актуальный: смена лямбды из MainScreen не пересоздаёт оверлей.
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    // «Уже закрываем»: крестик, back и конец ролика не должны закрыть дважды.
    val isDismissing = remember { AtomicBoolean(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val context = LocalContext.current.applicationContext

    // Каждый новый показ — новый цикл закрытия.
    LaunchedEffect(isVisible, creative.id) {
        if (isVisible) isDismissing.set(false)
    }

    // Единственная точка закрытия. post() гарантирует, что колбэк плеера
    // (`STATE_ENDED`) вернётся раньше, чем оверлей скроется и приостановит ролик.
    val requestDismiss: () -> Unit = {
        if (isDismissing.compareAndSet(false, true)) {
            // Только pause(): плеер, кодек и поверхность остаются тёплыми.
            AdVideoEngine.pause()
            mainHandler.post { currentOnDismiss() }
        }
    }

    // Старт ролика: тот же плеер и та же поверхность, что и в прогреве.
    LaunchedEffect(isVisible, creative.id) {
        if (isVisible && creative.type == AdMediaType.VIDEO) {
            AdVideoEngine.start(context, creative) {
                // Автозакрытие после конца ролика — тем же путём, что крестик.
                mainHandler.post { requestDismiss() }
            }
        }
    }

    // Оверлей скрыли (в т.ч. из ViewModel) — ролик на паузу, но не отпускаем.
    LaunchedEffect(isVisible) {
        if (!isVisible) AdVideoEngine.pause()
    }

    // Фон/возврат: звук рекламы не должен идти из кармана.
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> AdVideoEngine.pausePlayback()
                Lifecycle.Event.ON_START -> AdVideoEngine.resumePlayback()
                else -> Unit
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    // Оверлей ушёл из композиции раньше очереди — снимаем запланированное закрытие.
    DisposableEffect(Unit) {
        onDispose { mainHandler.removeCallbacksAndMessages(null) }
    }

    // Системный «назад» закрывает рекламу так же, как крестик. Композируется
    // после контента MainScreen, поэтому перехватывает back раньше его.
    BackHandler(enabled = isVisible) { requestDismiss() }

    val isVideo = creative.type == AdMediaType.VIDEO

    // Корень всегда на весь экран: размер постоянного `TextureView` не меняется
    // между показами, иначе меняется буфер `SurfaceTexture` и кодек
    // переконфигурируется (5 с `AtlasService`). Скрытая реклама — `alpha = 0`
    // у самого View и `zIndex = -1`: вне `isVisible` здесь нет ни фона, ни
    // перехватчика тапов, поэтому касания идут в `MainScreen`.
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Показанная реклама — поверх всех слоёв MainScreen; скрытая — за
            // контентом, чтобы даже не участвовать в обходе hit-test.
            .zIndex(if (isVisible) 10f else -1f)
    ) {
        if (isVisible) {
            // Чёрный фон и перехват касаний: главный экран под рекламой не нажимается.
            // Вне показа этого узла в композиции нет вовсе.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = {}
                    )
            )
        }

        // Постоянная поверхность видео: создаётся один раз, живёт всё время
        // работы приложения и **всегда** занимает весь экран. Скрытая реклама —
        // `alpha = 0`; размер буфера поверхности не меняется никогда.
        AdVideoTextureHost(
            visible = isVisible && isVideo,
            modifier = Modifier.fillMaxSize()
        )

        if (isVisible) {
            if (!isVideo) {
                PhotoAdFrame(
                    creative = creative,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = KochSpace.xl, vertical = KochSpace.x3l)
                )
            }
            AdCloseButton(
                creativeId = creative.id,
                // Фото — 3 с, видео — 5 с; раньше крестика нет.
                visibleAfterMs = if (isVideo) CLOSE_DELAY_VIDEO_MS else CLOSE_DELAY_PHOTO_MS,
                onDismiss = requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(2f)
                    .statusBarsPadding()
                    .padding(KochSpace.l)
            )
        }
    }
}

/**
 * Постоянный `TextureView` — одна выходная поверхность на процесс.
 *
 * `AndroidView` создаётся ровно один раз ([AdVideoEngine.attachHostView]) и
 * никогда не отвязывается: при скрытой рекламе View остаётся в композиции и
 * **сохраняет тот же размер — весь экран**. Менять размер нельзя: `TextureView`
 * пересчитывает размер буфера `SurfaceTexture` (`onSurfaceTextureSizeChanged`),
 * видеорендерер из-за этого переконфигурирует кодек, и на realme RMX3151
 * (Android 12, MediaTek) это фриз ~5 с ожидания `AtlasService` через 0,9 с
 * после первого кадра. Поэтому «скрыть» рекламу можно только `alpha = 0`,
 * но не размером.
 *
 * Сам View не интерактивен (`isClickable`/`isFocusable`/`isEnabled` = false):
 * он только рисует кадр и не должен забирать жесты у крестика и фона.
 */
@Composable
private fun AdVideoTextureHost(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).also { view ->
                view.isClickable = false
                view.isFocusable = false
                view.isFocusableInTouchMode = false
                view.isEnabled = false
                view.setOnTouchListener { _, _ -> false }
                view.alpha = if (visible) 1f else 0f
                AdVideoEngine.attachHostView(view)
            }
        },
        // Поверхность живёт до MainActivity.onDestroy(isFinishing): при уходе
        // View из композиции ничего не отвязываем и не освобождаем.
        onRelease = { view ->
            view.alpha = 0f
        },
        update = { view ->
            view.alpha = if (visible) 1f else 0f
        }
    )
}

/**
 * Фото-креатив: укладывается в экран по `Fit` (никакой обрезки), по центру.
 * Под ним — подпись «КУПИТЬ СЕЙЧАС!» у трёх фото; у «сына» надписей нет.
 * Видео здесь больше не рисуется: кадр идёт через постоянный [AdVideoTextureHost].
 */
@Composable
private fun PhotoAdFrame(
    creative: AdCreative,
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
            Image(
                painter = painterResource(id = creative.mediaRes),
                contentDescription = "Реклама",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(fitted.width, fitted.height)
            )
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
 * Реальный аспект фото (ширина / высота). Читается в IO: заголовок JPEG и
 * метаданные MP4 — без декодирования полного кадра. `produceState` привязан
 * к `creative.id`: обычные рекомпозиции оверлея не перезапускают чтение
 * (`MediaMetadataRetriever` и `BitmapFactory` — дорогие нативные вызовы, им
 * нечего делать на главном потоке). До ответа — [DEFAULT_MEDIA_ASPECT];
 * при ошибке остаётся он же, поэтому реклама не «прыгает» и не ломается.
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
                // Кадр видео вписывает сам движок по `VideoSize`; здесь — запас.
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
 * Крестик рекламы в правом верхнем углу. Появляется через [visibleAfterMs]
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
