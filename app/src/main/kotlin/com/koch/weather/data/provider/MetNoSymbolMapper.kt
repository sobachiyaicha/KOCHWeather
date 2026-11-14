package com.koch.weather.data.provider

/**
 * symbol_code MET Norway → WMO-код Open-Meteo, который уже понимает WmoHelper.
 * Поддерживаются суффиксы _day, _night, _polartwilight.
 */
object MetNoSymbolMapper {

    fun toWmo(symbolCode: String?): Int? {
        val base = symbolCode
            ?.trim()
            ?.lowercase()
            ?.removeSuffix("_polartwilight")
            ?.removeSuffix("_day")
            ?.removeSuffix("_night")
            ?: return null
        if (base.isEmpty()) return null
        return when (base) {
            "clearsky" -> 0
            "fair" -> 1
            "partlycloudy" -> 2
            "cloudy" -> 3
            "fog" -> 45
            "lightrain" -> 61
            "rain" -> 63
            "heavyrain" -> 65
            "lightrainshowers" -> 80
            "rainshowers" -> 81
            "heavyrainshowers" -> 82
            "lightrainandthunder", "rainandthunder", "heavyrainandthunder" -> 95
            "lightsleet", "sleet" -> 66
            "heavysleet" -> 67
            "lightsnow" -> 71
            "snow" -> 73
            "heavysnow" -> 75
            "lightsnowshowers" -> 85
            "snowshowers" -> 86
            "sleetandthunder", "snowandthunder", "lightsleetandthunder", "heavysleetandthunder" -> 95
            else -> null
        }
    }
}
