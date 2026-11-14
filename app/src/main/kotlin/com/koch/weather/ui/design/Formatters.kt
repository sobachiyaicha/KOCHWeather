package com.koch.weather.ui.design

import java.util.Locale

/** "34 500" — разряды через пробел, без копеек. Для цен в рублях. */
fun formatRuAmount(value: Int): String =
    String.format(Locale.US, "%,d", value).replace(',', ' ')

/** "10.50" — два знака для BYN (мелкие суммы); "1 120" — если значение целое и большое. */
fun formatByn(value: Double): String =
    if (value >= 100.0 && value % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", value).replace(',', ' ')
    } else {
        String.format(Locale.US, "%.2f", value)
    }

/**
 * Единый формат диапазона температур для шапки, списка городов и деталей дня:
 * ровно один пробел после двоеточия — «Макс.: 4°, мин.: 2°».
 */
fun formatMaxMinRu(max: Int, min: Int): String = "Макс.: $max°, мин.: $min°"
