package com.koch.weather.ui.theme

import androidx.compose.material3.Typography

/**
 * Материальная типографика собирается из тех же токенов, что и экраны ([KochType]),
 * чтобы `Text` без явного style не «врал» размерами и весами.
 *
 * Отдельных кеглей здесь нет: 8 размеров из [KochType] — единственный источник правды.
 */
val KochMaterialTypography = Typography(
    displayLarge = KochType.displayTemp,
    headlineLarge = KochType.titlePage,
    headlineMedium = KochType.titleSheet,
    titleLarge = KochType.titleCard,
    titleMedium = KochType.titleSection,
    bodyLarge = KochType.body,
    bodyMedium = KochType.body,
    bodySmall = KochType.sub,
    labelLarge = KochType.button,
    labelMedium = KochType.chip,
    labelSmall = KochType.microCaps
)
