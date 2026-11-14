package com.koch.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.KochType
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Облачная вуаль (refs/cloud_swipe.png, 2:40; кадр f_076).
 *
 * Облака закрывают **только шапку** (верхние ~45 % экрана) с мягким нижним краем; ниже —
 * лёгкая дымка. Подсказка — две строки без плашки, тёмным по светлому. Ни процентов,
 * ни прогресс-бара, ни нижней подписи: в референсе их нет.
 * Жест по-прежнему перехватывает drag над шапкой, чтобы не переключался город.
 */
@Composable
fun CloudVeilOverlay(
    onCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    var wipeProgress by remember { mutableFloatStateOf(0f) }
    val eraseMarks = remember { mutableStateListOf<Offset>() }
    var isCleared by remember { mutableStateOf(false) }
    val hintColor = Color(0xFF1F2A33)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(Unit) {
                var travelled = 0f
                var lastPoint: Offset? = null
                val requiredDistance = 700.dp.toPx()
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
            val veilBottom = size.height * 0.45f
            clipRect(top = 0f, left = 0f, right = size.width, bottom = veilBottom) {
                // Плотное облако сверху, растворяется к нижней границе вуали.
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color(0xFFF6FAFC),
                        0.55f to Color(0xFFF1F6F9),
                        0.85f to Color(0xE6F1F6F9),
                        1f to Color(0x00F1F6F9),
                        startY = 0f,
                        endY = veilBottom
                    )
                )
                // Мягкие объёмы: 4 овала, а не «плоские круги» на весь экран.
                cloudPuff(
                    cx = size.width * 0.22f,
                    cy = veilBottom * 0.28f,
                    rx = size.width * 0.62f,
                    ry = veilBottom * 0.26f,
                    color = Color(0x66FFFFFF)
                )
                cloudPuff(
                    cx = size.width * 0.82f,
                    cy = veilBottom * 0.40f,
                    rx = size.width * 0.55f,
                    ry = veilBottom * 0.22f,
                    color = Color(0x59FFFFFF)
                )
                cloudPuff(
                    cx = size.width * 0.45f,
                    cy = veilBottom * 0.62f,
                    rx = size.width * 0.75f,
                    ry = veilBottom * 0.20f,
                    color = Color(0x4DFFFFFF)
                )
                cloudPuff(
                    cx = size.width * 0.10f,
                    cy = veilBottom * 0.78f,
                    rx = size.width * 0.45f,
                    ry = veilBottom * 0.16f,
                    color = Color(0x40DCE7EE)
                )

                // Стирание пальцем: только внутри облачной зоны.
                val holeRadius = 76.dp.toPx()
                eraseMarks.forEach { point ->
                    if (point.y <= veilBottom + holeRadius) {
                        drawCircle(
                            color = Color.Transparent,
                            radius = holeRadius,
                            center = point,
                            blendMode = BlendMode.Clear
                        )
                    }
                }
            }
        }

        // Подсказка: рука + две строки, тёмным по облаку, без плашки и без счётчиков.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 44.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = hintColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Прогоните облака пальцем,",
                style = KochType.titleSection,
                color = hintColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "чтобы увидеть погоду",
                style = KochType.titleSection,
                color = hintColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.cloudPuff(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    color: Color
) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(cx, cy),
            radius = maxOf(rx, ry)
        ),
        topLeft = Offset(cx - rx, cy - ry),
        size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f)
    )
}
