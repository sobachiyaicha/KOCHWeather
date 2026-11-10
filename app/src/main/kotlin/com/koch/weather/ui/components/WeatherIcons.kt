package com.koch.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.WeatherIconType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherIcon(
    iconType: WeatherIconType,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            when (iconType) {
                WeatherIconType.CLEAR_DAY -> drawClearDay()
                WeatherIconType.CLEAR_NIGHT -> drawClearNight()
                WeatherIconType.PARTLY_CLOUDY_DAY -> drawPartlyCloudyDay()
                WeatherIconType.PARTLY_CLOUDY_NIGHT -> drawPartlyCloudyNight()
                WeatherIconType.CLOUDY -> drawCloudy()
                WeatherIconType.FOG -> drawFog()
                WeatherIconType.DRIZZLE -> drawRain(isHeavy = false)
                WeatherIconType.RAIN -> drawRain(isHeavy = false)
                WeatherIconType.HEAVY_RAIN -> drawRain(isHeavy = true)
                WeatherIconType.SNOW -> drawSnow()
                WeatherIconType.THUNDERSTORM -> drawThunderstorm()
            }
        }
    }
}

private fun DrawScope.drawClearDay() {
    val center = Offset(size.width * 0.5f, size.height * 0.5f)
    val r = size.width * 0.30f

    // Outer glow
    drawCircle(
        color = Color(0x35FFA726),
        radius = r * 1.45f,
        center = center
    )

    // Sun body with gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF176), Color(0xFFFFCA28), Color(0xFFFF9800)),
            center = center,
            radius = r * 1.1f
        ),
        radius = r,
        center = center
    )

    // Sun rays
    val rayCount = 8
    val rayInner = r * 1.2f
    val rayOuter = r * 1.48f
    for (i in 0 until rayCount) {
        val angle = (i * (2 * Math.PI / rayCount)).toFloat()
        val start = Offset(
            center.x + rayInner * cos(angle),
            center.y + rayInner * sin(angle)
        )
        val end = Offset(
            center.x + rayOuter * cos(angle),
            center.y + rayOuter * sin(angle)
        )
        drawLine(
            color = Color(0xFFFFCA28),
            start = start,
            end = end,
            strokeWidth = size.width * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawClearNight() {
    val center = Offset(size.width * 0.5f, size.height * 0.5f)
    val r = size.width * 0.36f

    // Crescent moon
    val moonPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(center.x - r, center.y - r, center.x + r, center.y + r))
    }
    drawPath(
        path = moonPath,
        color = Color(0xFFFFF9C4)
    )
    // Cutout
    val cutPath = Path().apply {
        val cutR = r * 0.88f
        val cutCenter = Offset(center.x + r * 0.42f, center.y - r * 0.35f)
        addOval(androidx.compose.ui.geometry.Rect(cutCenter.x - cutR, cutCenter.y - cutR, cutCenter.x + cutR, cutCenter.y + cutR))
    }
    // Blend mode simulation or draw crescent shape
    // Instead of destructive blend, draw soft crescent with stars
    drawCircle(
        color = Color(0x60FFF9C4),
        radius = r * 1.25f,
        center = center
    )
}

private fun DrawScope.drawPartlyCloudyDay() {
    // 1. Sun at upper right
    val sunCenter = Offset(size.width * 0.68f, size.height * 0.36f)
    val sunR = size.width * 0.25f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFCA28), Color(0xFFFB8C00)),
            center = sunCenter,
            radius = sunR * 1.1f
        ),
        radius = sunR,
        center = sunCenter
    )

    // Sun rays
    for (angleDeg in listOf(315f, 0f, 45f, 90f, 270f)) {
        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val p1 = Offset(sunCenter.x + sunR * 1.15f * cos(rad), sunCenter.y + sunR * 1.15f * sin(rad))
        val p2 = Offset(sunCenter.x + sunR * 1.45f * cos(rad), sunCenter.y + sunR * 1.45f * sin(rad))
        drawLine(
            color = Color(0xFFFFD54F),
            start = p1,
            end = p2,
            strokeWidth = size.width * 0.07f,
            cap = StrokeCap.Round
        )
    }

    // 2. Fluffy white cloud overlapping at bottom-left
    drawFluffyCloud(
        offsetX = size.width * 0.05f,
        offsetY = size.height * 0.38f,
        width = size.width * 0.82f,
        height = size.height * 0.52f,
        cloudColor = Color(0xFFFFFFFF),
        shadowColor = Color(0x40B0BEC5)
    )
}

private fun DrawScope.drawPartlyCloudyNight() {
    val moonCenter = Offset(size.width * 0.65f, size.height * 0.35f)
    drawCircle(
        color = Color(0xFFFFF9C4),
        radius = size.width * 0.22f,
        center = moonCenter
    )
    drawFluffyCloud(
        offsetX = size.width * 0.05f,
        offsetY = size.height * 0.38f,
        width = size.width * 0.82f,
        height = size.height * 0.52f,
        cloudColor = Color(0xFFECEFF1),
        shadowColor = Color(0x5078909C)
    )
}

private fun DrawScope.drawCloudy() {
    // Back cloud
    drawFluffyCloud(
        offsetX = size.width * 0.22f,
        offsetY = size.height * 0.22f,
        width = size.width * 0.72f,
        height = size.height * 0.46f,
        cloudColor = Color(0xFFCFD8DC),
        shadowColor = Color(0x4090A4AE)
    )
    // Front cloud
    drawFluffyCloud(
        offsetX = size.width * 0.06f,
        offsetY = size.height * 0.42f,
        width = size.width * 0.82f,
        height = size.height * 0.52f,
        cloudColor = Color(0xFFFFFFFF),
        shadowColor = Color(0x3578909C)
    )
}

private fun DrawScope.drawRain(isHeavy: Boolean) {
    drawFluffyCloud(
        offsetX = size.width * 0.08f,
        offsetY = size.height * 0.18f,
        width = size.width * 0.84f,
        height = size.height * 0.50f,
        cloudColor = if (isHeavy) Color(0xFFB0BEC5) else Color(0xFFECEFF1),
        shadowColor = Color(0x50546E7A)
    )

    // Raindrops
    val rainColor = Color(0xFF4FC3F7)
    val dropCount = if (isHeavy) 4 else 3
    val startX = size.width * 0.25f
    val stepX = (size.width * 0.5f) / (dropCount - 1).coerceAtLeast(1)

    for (i in 0 until dropCount) {
        val x = startX + i * stepX
        val y1 = size.height * 0.72f + (if (i % 2 == 1) size.height * 0.06f else 0f)
        val y2 = y1 + size.height * 0.18f
        drawLine(
            color = rainColor,
            start = Offset(x, y1),
            end = Offset(x - size.width * 0.07f, y2),
            strokeWidth = size.width * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSnow() {
    drawFluffyCloud(
        offsetX = size.width * 0.08f,
        offsetY = size.height * 0.18f,
        width = size.width * 0.84f,
        height = size.height * 0.50f,
        cloudColor = Color(0xFFECEFF1),
        shadowColor = Color(0x4090A4AE)
    )

    // Snowflake dots
    val snowColor = Color.White
    val points = listOf(
        Offset(size.width * 0.28f, size.height * 0.76f),
        Offset(size.width * 0.50f, size.height * 0.86f),
        Offset(size.width * 0.72f, size.height * 0.76f)
    )
    for (p in points) {
        drawCircle(color = snowColor, radius = size.width * 0.055f, center = p)
    }
}

private fun DrawScope.drawThunderstorm() {
    drawFluffyCloud(
        offsetX = size.width * 0.08f,
        offsetY = size.height * 0.14f,
        width = size.width * 0.84f,
        height = size.height * 0.50f,
        cloudColor = Color(0xFF78909C),
        shadowColor = Color(0x6037474F)
    )

    // Lightning bolt
    val boltPath = Path().apply {
        moveTo(size.width * 0.54f, size.height * 0.58f)
        lineTo(size.width * 0.40f, size.height * 0.74f)
        lineTo(size.width * 0.52f, size.height * 0.74f)
        lineTo(size.width * 0.36f, size.height * 0.96f)
        lineTo(size.width * 0.60f, size.height * 0.70f)
        lineTo(size.width * 0.47f, size.height * 0.70f)
        close()
    }
    drawPath(path = boltPath, color = Color(0xFFFFD54F))
}

private fun DrawScope.drawFog() {
    val fogColor = Color(0xD0ECEFF1)
    val barHeight = size.height * 0.10f
    val r = barHeight * 0.5f

    for (i in 0..2) {
        val y = size.height * (0.35f + i * 0.20f)
        val inset = if (i == 1) size.width * 0.08f else size.width * 0.14f
        drawRoundRect(
            color = fogColor,
            topLeft = Offset(inset, y),
            size = Size(size.width - inset * 2, barHeight),
            cornerRadius = CornerRadius(r, r)
        )
    }
}

private fun DrawScope.drawFluffyCloud(
    offsetX: Float,
    offsetY: Float,
    width: Float,
    height: Float,
    cloudColor: Color,
    shadowColor: Color
) {
    val rBase = height * 0.40f
    val bottomY = offsetY + height - rBase

    // Bottom pill
    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(offsetX, bottomY - rBase),
        size = Size(width, rBase * 2),
        cornerRadius = CornerRadius(rBase, rBase)
    )

    // Left puff circle
    drawCircle(
        color = cloudColor,
        radius = height * 0.38f,
        center = Offset(offsetX + width * 0.32f, offsetY + height * 0.48f)
    )

    // Right larger puff circle
    drawCircle(
        color = cloudColor,
        radius = height * 0.46f,
        center = Offset(offsetX + width * 0.62f, offsetY + height * 0.42f)
    )

    // Subtle bottom shadow outline
    drawRoundRect(
        color = shadowColor,
        topLeft = Offset(offsetX, bottomY - rBase),
        size = Size(width, rBase * 2),
        cornerRadius = CornerRadius(rBase, rBase),
        style = Stroke(width = 1.2f)
    )
}
