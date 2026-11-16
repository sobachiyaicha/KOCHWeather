package com.koch.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TemperatureRangeBar(
    minTemp: Int,
    maxTemp: Int,
    overallMin: Int,
    overallMax: Int,
    currentTemp: Int? = null,
    modifier: Modifier = Modifier,
    barHeight: Dp = 5.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        val totalSpan = (overallMax - overallMin).coerceAtLeast(1).toFloat()
        val startFraction = ((minTemp - overallMin) / totalSpan).coerceIn(0f, 1f)
        val endFraction = ((maxTemp - overallMin) / totalSpan).coerceIn(0f, 1f)
        val rangeFraction = (endFraction - startFraction).coerceAtLeast(0.08f)

        val trackCorner = CornerRadius(size.height / 2, size.height / 2)

        // Track background
        drawRoundRect(
            color = Color(0x30000000),
            topLeft = Offset(0f, 0f),
            size = size,
            cornerRadius = trackCorner
        )

        // Gradient segment
        val barStartX = size.width * startFraction
        val barWidth = (size.width * rangeFraction).coerceAtMost(size.width - barStartX)

        // Apple weather style gradient: Cyan/Green to Yellow to Orange
        val gradientBrush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF81C784), // Light green
                Color(0xFFAED581), // Lime green
                Color(0xFFFFD54F), // Yellow/amber
                Color(0xFFFFB74D)  // Orange
            ),
            startX = 0f,
            endX = size.width
        )

        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(barStartX, 0f),
            size = Size(barWidth, size.height),
            cornerRadius = trackCorner
        )

        // Current temperature dot for "Сегодня"
        if (currentTemp != null) {
            val currentFraction = ((currentTemp - overallMin) / totalSpan).coerceIn(0f, 1f)
            val dotX = (size.width * currentFraction).coerceIn(size.height, size.width - size.height)
            val dotCenter = Offset(dotX, size.height / 2)
            val dotRadius = size.height * 0.8f

            // Dark outline ring
            drawCircle(
                color = Color(0x80000000),
                radius = dotRadius + 1.2f,
                center = dotCenter,
                style = Stroke(width = 1.5f)
            )
            // White center circle
            drawCircle(
                color = Color.White,
                radius = dotRadius,
                center = dotCenter
            )
        }
    }
}
