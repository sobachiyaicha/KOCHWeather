package com.koch.weather.ui.components

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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.koch.weather.ui.design.decodeLocalDrawable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

/** Пропорции видеороликов рекламы, захардкоженные (port plan §6): 540×720 и 720×720. */
private const val AD_LION_VIDEO_ASPECT = 0.75f
private const val AD_SLIPPERS_VIDEO_ASPECT = 1.0f

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
 * Видео-ролик проигрывается целиком через [PlatformVideo] (Android — тот же VideoView/
 * MediaPlayer-стек, iOS — AVPlayer): без контролов, без паузы и перемотки, со звуком.
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
            // decorFitsSystemWindows убран (фаза 2): параметра нет в общем
            // конструкторе DialogProperties (port plan §13); edge-to-edge даёт хост.
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Системный «назад» не закрывает рекламу и не роняет активити:
            // выход — крестик или автозакрытие после конца видео.
            // Общий BackHandler (CMP): на iOS нет системного back — no-op.
            SystemBackHandler(enabled = true) { }
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
                // Замена painterResource(androidMediaRes()): декод через шов
                // [decodeLocalDrawable] (ключ 1:1 с mediaKey); пока кадр декодируется,
                // остаётся чёрный фон поп-апа.
                AdMediaType.PHOTO -> {
                    val photo by produceState<ImageBitmap?>(
                        initialValue = null,
                        key1 = creative.mediaKey
                    ) {
                        value = runCatching {
                            decodeLocalDrawable(creative.mediaKey)
                        }.getOrNull()
                    }
                    val creativePhoto = photo
                    if (creativePhoto != null) {
                        Image(
                            bitmap = creativePhoto,
                            contentDescription = "Реклама",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(fitted.width, fitted.height)
                        )
                    }
                }

                // Замена VideoView: платформенный шов [PlatformVideo]
                // (Android — тот же VideoView-стек; iOS — AVPlayer, port plan §6).
                // Ключ с расширением: mediaUrl на iOS ждёт полное имя файла.
                AdMediaType.VIDEO -> PlatformVideo(
                    mediaKey = creative.mediaKey + ".mp4",
                    aspect = aspect,
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
 * Аспект медиа (ширина / высота).
 *
 * Фаза 2 (KMP, port plan §6): метаданные MP4 больше не читаются
 * (MediaMetadataRetriever нет в KMP, а вычисление через AVAsset async запрещено) —
 * пропорции видеороликов захардкожены: `ad_lion` 540×720 → 0.75,
 * `ad_slippers` 720×720 → 1.0. У фото аспект считается из декодированного
 * кадра (ширина/высота ImageBitmap; декод полного кадра вместо
 * `inJustDecodeBounds` — краткое ожидание вместо мгновенного ответа).
 * До ответа и при ошибке — [DEFAULT_MEDIA_ASPECT]: поп-ап не «прыгает».
 */
@Composable
private fun rememberMediaAspect(creative: AdCreative): Float {
    val aspect by produceState(
        initialValue = DEFAULT_MEDIA_ASPECT,
        key1 = creative.id
    ) {
        value = withContext(Dispatchers.Default) {
            when (creative.type) {
                AdMediaType.PHOTO -> runCatching {
                    decodeLocalDrawable(creative.mediaKey)
                }.getOrNull()?.let { bitmap ->
                    if (bitmap.width > 0 && bitmap.height > 0) {
                        bitmap.width.toFloat() / bitmap.height
                    } else {
                        null
                    }
                }

                AdMediaType.VIDEO -> knownVideoAspect(creative.mediaKey)
            } ?: DEFAULT_MEDIA_ASPECT
        }
    }
    return aspect
}

/** Пропорции роликов из пула рекламы, захардкоженные (port plan §6). */
private fun knownVideoAspect(mediaKey: String): Float? = when (mediaKey) {
    "ad_lion" -> AD_LION_VIDEO_ASPECT
    "ad_slippers" -> AD_SLIPPERS_VIDEO_ASPECT
    else -> null
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
