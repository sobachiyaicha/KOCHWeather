package com.koch.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Full-screen cloud lock (refs/cloud_swipe.png):
 * a dense cloud veil covers the WHOLE screen, the finger wipes it away and reveals the weather
 * underneath. While the veil is up it also swallows drags, so the city carousel is not switched
 * accidentally (bug from the device audit: swipes over the clouds changed the city).
 */
@Composable
fun CloudVeilOverlay(
    onCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    var wipeProgress by remember { mutableFloatStateOf(0f) }
    val eraseMarks = remember { mutableStateListOf<Offset>() }
    var isCleared by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(Unit) {
                var travelled = 0f
                var lastPoint: Offset? = null
                val requiredDistance = 900.dp.toPx()
                val stampStep = 26.dp.toPx()

                detectDragGestures(
                    onDragStart = { start ->
                        lastPoint = start
                        eraseMarks.add(start)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val previous = lastPoint ?: change.position
                        val current = change.position
                        val segment = hypot(current.x - previous.x, current.y - previous.y)
                        val stamps = (segment / stampStep).roundToInt().coerceIn(1, 16)
                        for (i in 1..stamps) {
                            val t = i / stamps.toFloat()
                            eraseMarks.add(
                                Offset(
                                    x = previous.x + (current.x - previous.x) * t,
                                    y = previous.y + (current.y - previous.y) * t
                                )
                            )
                        }
                        while (eraseMarks.size > 220) {
                            eraseMarks.removeAt(0)
                        }
                        lastPoint = current

                        travelled += hypot(dragAmount.x, dragAmount.y)
                        wipeProgress = (travelled / requiredDistance).coerceIn(0f, 1f)

                        if (wipeProgress >= 1f && !isCleared) {
                            isCleared = true
                            onCleared()
                        }
                    },
                    onDragEnd = { lastPoint = null },
                    onDragCancel = { lastPoint = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCloudVeil()

            // Finger wipes transparent holes through the cloud layer.
            val holeRadius = 76.dp.toPx()
            eraseMarks.forEach { point ->
                drawCircle(
                    color = Color.Transparent,
                    radius = holeRadius,
                    center = point,
                    blendMode = BlendMode.Clear
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0x40FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                shadowElevation = 6.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Прогоните облака пальцем,",
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "чтобы увидеть погоду",
                        color = Color(0xFF334155),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = { wipeProgress },
                modifier = Modifier
                    .width(210.dp)
                    .height(6.dp),
                color = Color(0xFF1E88E5),
                trackColor = Color(0x44000000)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Сотрите облака: ${(wipeProgress * 100).roundToInt()}%",
                color = Color(0xFF334155),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "Погода спрятана за облаками",
            color = Color(0xFF475569),
            fontSize = 10.5.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
        )
    }
}

/**
 * Dense, full-bleed cloud field. Drawn on an offscreen layer so [BlendMode.Clear] can punch
 * finger holes through it and reveal the real screen below.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudVeil() {
    val w = size.width
    val h = size.height

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FAFC),
                Color(0xFFE8EEF5),
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1)
            )
        )
    )

    // Cool under-shadows give the veil volume instead of a flat white sheet.
    drawCircle(color = Color(0x55B0BEC5), radius = w * 0.38f, center = Offset(w * 0.18f, h * 0.24f))
    drawCircle(color = Color(0x44B0BEC5), radius = w * 0.42f, center = Offset(w * 0.82f, h * 0.18f))
    drawCircle(color = Color(0x44B0BEC5), radius = w * 0.40f, center = Offset(w * 0.50f, h * 0.55f))
    drawCircle(color = Color(0x40B0BEC5), radius = w * 0.36f, center = Offset(w * 0.20f, h * 0.82f))
    drawCircle(color = Color(0x40B0BEC5), radius = w * 0.34f, center = Offset(w * 0.85f, h * 0.78f))

    // Overlapping puffy cloud tops.
    drawCircle(color = Color(0xFFF8FAFC), radius = w * 0.34f, center = Offset(w * 0.10f, h * 0.12f))
    drawCircle(color = Color(0xFFF1F5F9), radius = w * 0.40f, center = Offset(w * 0.42f, h * 0.08f))
    drawCircle(color = Color(0xFFF8FAFC), radius = w * 0.36f, center = Offset(w * 0.78f, h * 0.10f))
    drawCircle(color = Color(0xFFF1F5F9), radius = w * 0.44f, center = Offset(w * 0.16f, h * 0.42f))
    drawCircle(color = Color(0xFFF8FAFC), radius = w * 0.48f, center = Offset(w * 0.52f, h * 0.36f))
    drawCircle(color = Color(0xFFF1F5F9), radius = w * 0.42f, center = Offset(w * 0.88f, h * 0.40f))
    drawCircle(color = Color(0xFFF8FAFC), radius = w * 0.46f, center = Offset(w * 0.22f, h * 0.70f))
    drawCircle(color = Color(0xFFF1F5F9), radius = w * 0.44f, center = Offset(w * 0.60f, h * 0.64f))
    drawCircle(color = Color(0xFFF8FAFC), radius = w * 0.40f, center = Offset(w * 0.90f, h * 0.72f))
    drawCircle(color = Color(0xFFE2E8F0), radius = w * 0.52f, center = Offset(w * 0.46f, h * 0.92f))
    drawCircle(color = Color(0xFFF8FAFC), radius = w * 0.34f, center = Offset(w * 0.06f, h * 0.96f))
    drawCircle(color = Color(0xFFF1F5F9), radius = w * 0.36f, center = Offset(w * 0.94f, h * 0.96f))
}
