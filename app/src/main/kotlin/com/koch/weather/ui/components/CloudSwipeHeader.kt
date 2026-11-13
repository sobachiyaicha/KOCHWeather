package com.koch.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun CloudSwipeHeader(
    onCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swipeDistance by remember { mutableFloatStateOf(0f) }
    val maxDistance = 240f
    val cloudProgress = (swipeDistance / maxDistance).coerceIn(0f, 1f)
    val remainingAlpha = (1f - cloudProgress).coerceIn(0f, 1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    swipeDistance += abs(dragAmount.x) + abs(dragAmount.y)
                    if (swipeDistance >= maxDistance) {
                        onCleared()
                    }
                }
            },
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Color.White.copy(alpha = remainingAlpha * 0.7f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Organic puffy cloud layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val alpha = remainingAlpha
                val w = size.width
                val h = size.height

                // Soft background haze
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = alpha * 0.96f),
                            Color(0xFFE2E8F0).copy(alpha = alpha * 0.92f),
                            Color(0xFFCBD5E1).copy(alpha = alpha * 0.88f)
                        )
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
                )

                // Overlapping organic cloud puffs
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.95f),
                    radius = h * 0.55f,
                    center = Offset(w * 0.22f, h * 0.45f)
                )
                drawCircle(
                    color = Color(0xFFF8FAFC).copy(alpha = alpha * 0.92f),
                    radius = h * 0.65f,
                    center = Offset(w * 0.5f, h * 0.38f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.95f),
                    radius = h * 0.55f,
                    center = Offset(w * 0.78f, h * 0.45f)
                )
                drawCircle(
                    color = Color(0xFFF1F5F9).copy(alpha = alpha * 0.85f),
                    radius = h * 0.45f,
                    center = Offset(w * 0.35f, h * 0.65f)
                )
                drawCircle(
                    color = Color(0xFFF1F5F9).copy(alpha = alpha * 0.85f),
                    radius = h * 0.45f,
                    center = Offset(w * 0.65f, h * 0.65f)
                )
            }

            // Central Instruction Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x35000000).copy(alpha = (remainingAlpha * 0.25f)),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        Color.White.copy(alpha = remainingAlpha * 0.6f)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "👆 Прогоните облака пальцем,",
                            color = Color(0xFF1E293B).copy(alpha = (remainingAlpha * 1.2f).coerceIn(0.3f, 1f)),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "чтобы увидеть погоду",
                            color = Color(0xFF334155).copy(alpha = (remainingAlpha * 1.2f).coerceIn(0.3f, 1f)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress indicator
                LinearProgressIndicator(
                    progress = { cloudProgress },
                    modifier = Modifier
                        .width(140.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF1E88E5).copy(alpha = remainingAlpha),
                    trackColor = Color(0x30000000).copy(alpha = remainingAlpha * 0.3f)
                )
            }
        }
    }
}
