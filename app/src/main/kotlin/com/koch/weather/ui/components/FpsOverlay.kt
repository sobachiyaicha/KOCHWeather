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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FpsOverlay(
    modifier: Modifier = Modifier
) {
    var fps by remember { mutableStateOf<Int?>(null) }
    var fpsColor by remember { mutableStateOf(Color(0xFF4CAF50)) }

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
                        currentFps >= 55 -> Color(0xFF4CAF50)
                        currentFps >= 30 -> Color(0xFFFFD54F)
                        else -> Color(0xFFEF5350)
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
            .background(Color(0x99000000), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$measuredFps FPS",
            color = fpsColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
