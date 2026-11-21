package com.koch.weather.ui.design

import com.koch.weather.data.api.formatFixed

/** "34 500" — разряды через пробел, без копеек. Для цен в российских рублях. */
fun formatRuAmount(value: Int): String {
    // Замена String.format(Locale.US, "%,d", value).replace(',', ' '):
    // группировка по 3 разряда, знак сохраняется, 0 — «0».
    val sign = if (value < 0) "-" else ""
    val digits = (if (value < 0) -value.toLong() else value.toLong()).toString()
    val grouped = StringBuilder()
    for (i in digits.indices) {
        if (i > 0 && (digits.length - i) % 3 == 0) grouped.append(' ')
        grouped.append(digits[i])
    }
    return sign + grouped.toString()
}

/**
 * Единый формат диапазона температур для шапки, списка городов и деталей дня:
 * ровно один пробел после двоеточия — «Макс.: 4°, мин.: 2°».
 */
fun formatMaxMinRu(max: Int, min: Int): String = "Макс.: $max°, мин.: $min°"

/**
 * «2,5» — миллиметры осадков с запятой, один знак после неё. Интерфейс русский,
 * поэтому «Σ 2.5 мм» на графике осадков читалось как опечатка (кадр r14_10).
 * Формат детерминирован (не зависит от локали телефона): округление HALF_UP
 * как у "%.1f", затем точка заменяется запятой.
 */
fun formatMmRu(value: Double): String =
    formatFixed(value, 1).replace('.', ',')
