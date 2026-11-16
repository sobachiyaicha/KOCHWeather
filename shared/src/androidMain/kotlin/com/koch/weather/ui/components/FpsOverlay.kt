package com.koch.weather.ui.components

import android.view.Choreographer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Debug-only FPS-метр.
 *
 * Показывается только когда родитель решил, что он нужен (См. MainScreen: `BuildConfig.DEBUG`
 * + скрытый жест). Компактный, в левом нижнем углу над навбаром, не перекрывает чипы и баннеры.
 */
@Composable
fun FpsOverlay(
    modifier: Modifier = Modifier
) {
    var fps by remember { mutableStateOf<Int?>(null) }
    var fpsColor by remember { mutableStateOf(Color(0xFFA5D6A7)) }

    DisposableEffect(Unit) {
        var frameCount = 0
        var lastTimeNanos = System.nanoTime()
        val choreographer = Choreographer.getInstance()

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val elapsed = frameTimeNanos - lastTimeNanos
                if (elapsed >= 1_000_000_000L) {
                    val currentFps = (frameCount * 1_000_000_000L / elapsed).toInt()
                    fps = currentFps
                    fpsColor = when {
                        currentFps >= 55 -> Color(0xFFA5D6A7)
                        currentFps >= 30 -> Color(0xFFFFE082)
                        else -> Color(0xFFEF9A9A)
                    }
                    frameCount = 0
                    lastTimeNanos = frameTimeNanos
                }
                choreographer.postFrameCallback(this)
            }
        }

        choreographer.postFrameCallback(callback)

        onDispose {
            choreographer.removeFrameCallback(callback)
        }
    }

    // Do not show a fake "0 FPS"/"60 FPS" on the first frames: wait for a real measurement.
    val measuredFps = fps ?: return

    Box(
        modifier = modifier
            .background(Color(0x66000000), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$measuredFps FPS",
            color = fpsColor.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
