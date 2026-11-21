package com.koch.weather.data.api

import kotlin.math.abs

/**
 * Замена String.format(Locale.US, "%.<decimals>f", value) — формат координат в
 * URL провайдеров и ключах кэша: фиксированное число знаков, разделитель «.»,
 * округление HALF_UP (от нуля на границе .5), без экспоненты.
 *
 * Реализация без BigDecimal/java.util: разбор Decimal-формы Double.toString
 * (как в DeepLinks.format) + целочисленное округление. На входах провайдеров
 * (координаты |v| < 1000) результат совпадает с %f символ-в-символ.
 */
fun formatFixed(value: Double, decimals: Int): String {
    if (!value.isFinite()) return value.toString()
    require(decimals >= 0) { "decimals must be >= 0, got $decimals" }

    // Работаем с |value|: HALF_UP — округление от нуля, знак возвращаем в конце.
    val body = abs(value).toString()
    var exponent = 0
    var mantissa = body
    val eIndex = body.indexOf('E')
    if (eIndex >= 0) {
        exponent = body.substring(eIndex + 1).toInt()
        mantissa = body.substring(0, eIndex)
    }
    val dotIndex = mantissa.indexOf('.')
    val intPart = if (dotIndex >= 0) mantissa.substring(0, dotIndex) else mantissa
    val fracPart = if (dotIndex >= 0) mantissa.substring(dotIndex + 1) else ""

    // |value| = digits * 10^-scale, digits — десятичное число без ведущих нулей.
    var digits = (intPart + fracPart).trimStart('0')
    if (digits.isEmpty()) digits = "0"
    val scale = fracPart.length - exponent

    // rounded — абсолютное значение с РОВНО [decimals] знаками дробной части
    // (последние [decimals] цифр — дробная часть, до них — целая).
    val rounded: String = if (scale <= decimals) {
        // Дробных разрядов меньше нужного — добираем нулями, округлять нечего.
        digits + "0".repeat(decimals - scale)
    } else {
        val drop = scale - decimals
        val digitsValue = digits.toLongOrNull()
        if (digitsValue != null && drop <= 18) {
            // Точный путь: digits <= 1e17 и divisor <= 1e18 влезают в Long.
            val divisor = pow10(drop)
            val quotient = digitsValue / divisor
            val remainder = digitsValue % divisor
            val result = if (remainder * 2 >= divisor) quotient + 1 else quotient
            result.toString().padStart(decimals + 1, '0')
        } else if (drop > digits.length) {
            // Значение меньше половины последней дробной единицы — всегда вниз.
            "0".repeat(decimals + 1)
        } else {
            // drop == digits.length (все цифры уходят в остаток): HALF_UP решает
            // первая (она же старшая) цифра, остальное Long-ом не влезает.
            if (digits.first() >= '5') "1" + "0".repeat(decimals) else "0".repeat(decimals + 1)
        }
    }

    val plain = rounded.dropLast(decimals) + "." + rounded.takeLast(decimals)
    // %f сохраняет знак входа даже при округлении до нуля ("-0.0000").
    val negative = value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)
    return if (negative) "-" + plain else plain
}

/** 10^k без переполнения (k <= 18). */
private fun pow10(k: Int): Long {
    var result = 1L
    repeat(k) { result *= 10L }
    return result
}
