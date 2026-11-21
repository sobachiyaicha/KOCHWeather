package com.koch.weather.data.api

/**
 * KMP-замена java.net.URLEncoder.encode(value, "UTF-8").
 *
 * Правила 1:1 с URLEncoder:
 *  - [a-zA-Z0-9], ".", "-", "*", "_" — без изменений;
 *  - пробел → '+';
 *  - всё остальное → %XX (заглавные hex) по байтам UTF-8.
 *
 * Используется в query-параметрах Open-Meteo geocoding, Photon, Nominatim и
 * deep links (DeepLinks.encode) — байты совпадают с Android-версией байт-в-байт.
 */
fun urlEncodeUtf8(value: String): String {
    val bytes = value.encodeToByteArray()
    val hex = "0123456789ABCDEF"
    return buildString(bytes.size) {
        for (byte in bytes) {
            val c = byte.toInt() and 0xFF
            when {
                c in 'a'.code..'z'.code ||
                    c in 'A'.code..'Z'.code ||
                    c in '0'.code..'9'.code ||
                    c == '.'.code || c == '-'.code || c == '*'.code || c == '_'.code ->
                    append(c.toChar())

                c == ' '.code -> append('+')

                else -> {
                    append('%')
                    append(hex[c shr 4])
                    append(hex[c and 0x0F])
                }
            }
        }
    }
}
