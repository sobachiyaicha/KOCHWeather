package com.koch.weather.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Фон приложения: градиент темы + процедурные облака (план §2.7).
 *
 * Облака рисуются один раз на кадр через мягкие радиальные градиенты — без blur и без
 * анимации, поэтому не стоят лишних миллисекунд в p50 (R18).
 */
@Composable
fun KochSkyBackground(
    modifier: Modifier = Modifier,
    showClouds: Boolean = true
) {
    val colors = LocalKochColors.current
    Box(modifier = modifier.fillMaxSize().background(colors.gradient)) {
        if (showClouds) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCloudLayer(colors.bgTop, colors.bgBottom)
            }
        }
    }
}

private fun DrawScope.drawCloudLayer(top: Color, bottom: Color) {
    val w = size.width
    val h = size.height
    val softTop = Color.White.copy(alpha = if (top.luminanceLike() > 0.6f) 0.10f else 0.06f)
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

    puff(w * 0.18f, h * 0.10f, w * 0.62f, h * 0.16f, softTop)
    puff(w * 0.78f, h * 0.16f, w * 0.55f, h * 0.13f, softMid)
    puff(w * 0.46f, h * 0.30f, w * 0.80f, h * 0.18f, shade)
    puff(w * 0.12f, h * 0.42f, w * 0.50f, h * 0.12f, softMid)
    puff(w * 0.66f, h * 0.55f, w * 0.60f, h * 0.10f, shade)
}

private fun Color.luminanceLike(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
