package com.koch.weather.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.koch.weather.data.model.AppThemeMode

/** Градиент превью темы: общий для рулетки и полноэкранного экрана «Вид» (R15). */
internal fun themePreviewBrush(theme: AppThemeMode): Brush = when (theme) {
    AppThemeMode.BLUE_SKY -> Brush.verticalGradient(listOf(Color(0xFF2196F3), Color(0xFF64B5F6)))
    AppThemeMode.COPPER_SUNSET ->
        Brush.verticalGradient(listOf(Color(0xFF7A351D), Color(0xFFBF5B2B), Color(0xFFE8985E)))
    AppThemeMode.FOREST -> Brush.verticalGradient(listOf(Color(0xFF1B4D3E), Color(0xFF2E7D32)))
    AppThemeMode.MARS -> Brush.verticalGradient(listOf(Color(0xFF8B2500), Color(0xFFD84315)))
    AppThemeMode.CYBERPUNK -> Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF00E5FF)))
    AppThemeMode.BLOODY_SUNSET ->
        Brush.verticalGradient(listOf(Color(0xFFB71C1C), Color(0xFFFF5252)))
    AppThemeMode.AURORA_BOREALIS ->
        Brush.verticalGradient(listOf(Color(0xFF004D40), Color(0xFF00E676)))
}

/** Основной цвет темы — для подложки/подсветки, когда нужен один цвет, а не градиент. */
internal fun themeAccentColor(theme: AppThemeMode): Color = when (theme) {
    AppThemeMode.BLUE_SKY -> Color(0xFF2196F3)
    AppThemeMode.COPPER_SUNSET -> Color(0xFFC78265)
    AppThemeMode.FOREST -> Color(0xFF2E7D32)
    AppThemeMode.MARS -> Color(0xFFD84315)
    AppThemeMode.CYBERPUNK -> Color(0xFF00E5FF)
    AppThemeMode.BLOODY_SUNSET -> Color(0xFFFF5252)
    AppThemeMode.AURORA_BOREALIS -> Color(0xFF00E676)
}
