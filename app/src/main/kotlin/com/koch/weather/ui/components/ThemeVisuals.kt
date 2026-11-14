package com.koch.weather.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.ui.theme.kochColors

/**
 * Превью темы: тот же градиент, что и у настоящего фона (единый источник — kochColors),
 * поэтому «Лес» и «Марс» больше не выглядят как один и тот же голубой прямоугольник.
 */
internal fun themePreviewBrush(theme: AppThemeMode): Brush {
    val colors = kochColors(theme)
    return Brush.verticalGradient(
        listOf(colors.bgTop, colors.bgUpper, colors.bgLower, colors.bgBottom)
    )
}

/** Основной цвет темы — для подложки/подсветки, когда нужен один цвет, а не градиент. */
internal fun themeAccentColor(theme: AppThemeMode): Color = kochColors(theme).bgLower

