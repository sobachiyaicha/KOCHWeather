package com.koch.weather.ui.theme

import androidx.compose.ui.graphics.Color

// Палитра KOCHWeather живёт в Theme.kt как семантические токены KochColors
// (surface / border / accent / nav…), см. _history/reports/phase-4/ui-design-plan.md §4.2–4.3.
//
// Этот файл оставлен как точка входа для новых цветов: добавлять оттенки можно только здесь,
// и только если роль не выражается существующим токеном. Прямые Color(0x…) в экранах запрещены.

/**
 * Реклама (ТЗ п. 3): зелёная подложка плашки баннера — цвет подложки самого фото.
 * Градиент `#0E7A3C → #12803F`, одинаковый во всех темах: это цвет креатива, а не темы.
 */
val AdPlateGreenTop = Color(0xFF0E7A3C)
val AdPlateGreenBottom = Color(0xFF12803F)

/** Реклама (ТЗ п. 2): красный цвет подписи «КУПИТЬ СЕЙЧАС!» на чёрном фоне поп-апа. */
val AdBuyRed = Color(0xFFFF3B30)
