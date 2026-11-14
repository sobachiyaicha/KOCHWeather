package com.koch.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.KochType
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val MAX_ERASE_MARKS = 96
private const val MARK_EVERY_NTH = 2

/**
 * Облачная вуаль (refs/cloud_swipe.png, 2:40; кадр f_076).
 *
 * Производительность (отчёт perf-qa):
 *  - offscreen-слой только по облачной зоне (45 % высоты), а не на весь экран;
 *  - стирание — ОДИН `drawPath` с `BlendMode.Clear` вместо 220 `drawCircle` за кадр;
 *  - метки штампа хранятся в обычном ArrayList, снимок состояния не аллоцируется на каждый
 *    pointer-event; перерисовку будит один int-счётчик;
 *  - `wipeProgress` больше не состояние (в UI его никто не читает) — обычная переменная.
 *
 * Жест полностью поглощает drag внутри облачной зоны, поэтому карусель городов не листается.
 */
@Composable
fun CloudVeilOverlay(
    onCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eraseMarks = remember { ArrayList<Offset>(MAX_ERASE_MARKS) }
    var redrawVersion by remember { mutableIntStateOf(0) }
    var isCleared by remember { mutableStateOf(false) }
    // Подсказка гаснет, как только начали стирать: иначе она остаётся поверх стёртой зоны
    // и наезжает на капсулу (замечание визуального критика).
    var hintVisible by remember { mutableStateOf(true) }
    val holePath = remember { Path() }
    val hintColor = Color(0xFF1F2A33)

    Box(modifier = modifier.fillMaxSize()) {
        // Пока облака не стёрты, тапы не проваливаются в карточки под ними.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* блокируем сквозные тапы */ }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Только шапка: контейнер вуали уже укорочен на высоту нижнего бара, поэтому
                // 0.35 ≈ 197 dp — накрывает капсулу (117…186 dp) целиком, но не пустое
                // состояние и не «Повторить».
                .fillMaxHeight(0.35f)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .pointerInput(Unit) {
                    var travelled = 0f
                    var lastPoint: Offset? = null
                    val requiredDistance = 650.dp.toPx()
                    val stampStep = 26.dp.toPx()

                    detectDragGestures(
                        onDragStart = { start ->
                            hintVisible = false
                            lastPoint = start
                            eraseMarks.add(start)
                            redrawVersion++
                        },
                        onDrag = { change, dragAmount ->
                            // Полностью забираем жест: до родительского pager он не доходит.
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
                            if (eraseMarks.size > MAX_ERASE_MARKS) {
                                eraseMarks.subList(0, eraseMarks.size - MAX_ERASE_MARKS).clear()
                            }
                            lastPoint = current
                            redrawVersion++

                            travelled += hypot(dragAmount.x, dragAmount.y)
                            if (travelled >= requiredDistance && !isCleared) {
                                isCleared = true
                                onCleared()
                            }
                        },
                        onDragEnd = {
                            lastPoint = null
                            if (!isCleared) hintVisible = true
                        },
                        onDragCancel = {
                            lastPoint = null
                            if (!isCleared) hintVisible = true
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Читаем счётчик — это единственная причина перерисовки.
                @Suppress("UNUSED_EXPRESSION")
                redrawVersion

                val veilBottom = size.height
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
                cloudPuff(size.width * 0.22f, veilBottom * 0.28f, size.width * 0.62f, veilBottom * 0.26f, Color(0x66FFFFFF))
                cloudPuff(size.width * 0.82f, veilBottom * 0.40f, size.width * 0.55f, veilBottom * 0.22f, Color(0x59FFFFFF))
                cloudPuff(size.width * 0.45f, veilBottom * 0.62f, size.width * 0.75f, veilBottom * 0.20f, Color(0x4DFFFFFF))
                cloudPuff(size.width * 0.10f, veilBottom * 0.78f, size.width * 0.45f, veilBottom * 0.16f, Color(0x40DCE7EE))

                // Стирание: один путь из всех меток → один drawPath с Clear.
                if (eraseMarks.isNotEmpty()) {
                    holePath.reset()
                    val radius = 76.dp.toPx()
                    var index = 0
                    while (index < eraseMarks.size) {
                        val point = eraseMarks[index]
                        holePath.addOval(
                            androidx.compose.ui.geometry.Rect(
                                center = point,
                                radius = radius
                            )
                        )
                        index += MARK_EVERY_NTH
                    }
                    drawPath(path = holePath, color = Color.Transparent, blendMode = BlendMode.Clear)
                }
            }

            // Подсказка видна только до первого касания: как только начали стирать — гаснет.
            if (hintVisible) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 40.dp, start = 32.dp, end = 32.dp)
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
        size = Size(rx * 2f, ry * 2f)
    )
}
