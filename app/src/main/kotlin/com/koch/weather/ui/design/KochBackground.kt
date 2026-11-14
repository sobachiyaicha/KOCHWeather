package com.koch.weather.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.koch.weather.ui.components.wikimediaImageLoader
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Фон приложения.
 *
 * 1) База — градиент темы (всегда видим, в том числе пока фото грузится и когда сети нет).
 * 2) Если тема даёт фото неба ([imageUrl] из `uiState.sky*ImageUrl`) — оно рисуется поверх
 *    градиента через `AsyncImage` с `Crop`; при загрузке/ошибке плейсхолдера нет, поэтому
 *    остаётся градиент (никаких серых боксов офлайн).
 * 3) Над фото — скрим 20–35 %, чтобы белый текст и стекло читались.
 * 4) Без фото — процедурные облака (дешёвые овалы, без blur).
 */
@Composable
fun KochSkyBackground(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    showClouds: Boolean = true,
    onPhotoLoaded: ((Boolean) -> Unit)? = null
) {
    val colors = LocalKochColors.current
    val context = LocalContext.current
    val loader = remember(context) { wikimediaImageLoader(context) }
    var photoLoaded by remember(imageUrl) { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(colors.gradient)) {
        // Облака — фолбэк: рисуем их, пока фото не загрузилось (в том числе офлайн).
        if (!photoLoaded && showClouds) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCloudLayer(colors.bgTop, colors.bgBottom)
            }
        }

        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                imageLoader = loader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                // Плейсхолдеров у AsyncImage нет намеренно: пока фото нет (или офлайн) —
                // сквозь него виден градиент, а не серый бокс.
                onState = { state ->
                    val loaded = state is AsyncImagePainter.State.Success
                    photoLoaded = loaded
                    onPhotoLoaded?.invoke(loaded)
                }
            )

            if (photoLoaded) {
                // Скрим поверх фото под белый текст: 40 % сверху, 30 % в середине, 48 % снизу.
                // Раньше было 30/20/35 — подпись «Температура (°C)» не добирала контраст.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x66000000),
                                    Color(0x4D000000),
                                    Color(0x7A000000)
                                )
                            )
                        )
                )
                // Отдельный верхний скрим под статус-бар: белые иконки читаются на любом фото.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x73000000), Color(0x00000000))
                            )
                        )
                )
            }
        }
    }
}

private fun DrawScope.drawCloudLayer(top: Color, bottom: Color) {
    val w = size.width
    val h = size.height
    val topLum = top.luminanceLike()
    val softTop = Color.White.copy(alpha = if (topLum > 0.6f) 0.10f else 0.06f)
    val softMid = Color.White.copy(alpha = 0.05f)
    val shade = Color.Black.copy(alpha = if (bottom.luminanceLike() > 0.5f) 0.03f else 0.05f)

    fun puff(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = Offset(cx, cy),
                radius = maxOf(rx, ry)
            ),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f)
        )
    }

    // Тёплый «солнечный» блик сверху: фото-подобный перепад вместо плоской заливки.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF4E0).copy(alpha = if (topLum > 0.6f) 0.10f else 0.22f),
                Color.Transparent
            ),
            center = Offset(w * 0.72f, h * 0.06f),
            radius = w * 0.75f
        ),
        topLeft = Offset(0f, -h * 0.20f),
        size = Size(w * 1.5f, h * 0.55f)
    )

    puff(w * 0.18f, h * 0.10f, w * 0.62f, h * 0.16f, softTop)
    puff(w * 0.78f, h * 0.16f, w * 0.55f, h * 0.13f, softMid)
    puff(w * 0.46f, h * 0.30f, w * 0.80f, h * 0.18f, shade)
    puff(w * 0.12f, h * 0.42f, w * 0.50f, h * 0.12f, softMid)
    puff(w * 0.66f, h * 0.55f, w * 0.60f, h * 0.10f, shade)

    drawRect(
        brush = Brush.verticalGradient(
            0.62f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.20f),
            startY = 0f,
            endY = h
        )
    )
}

private fun Color.luminanceLike(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
