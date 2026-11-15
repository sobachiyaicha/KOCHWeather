package com.koch.weather.data.provider

/**
 * 7timer weather → WMO (для существующего WmoHelper).
 * Поддерживаются суффиксы day/night и thunder-варианты.
 */
object SevenTimerSymbolMapper {

    fun toWmo(weather: String?): Int? {
        val base = weather
            ?.trim()
            ?.lowercase()
            ?.removeSuffix("day")
            ?.removeSuffix("night")
            ?: return null
        if (base.isEmpty()) return null
        return when (base) {
            "clear" -> 0
            "pcloudy" -> 2
            "mcloudy", "cloudy" -> 3
            "fog" -> 45
            "lightrain" -> 61
            "rain" -> 63
            "heavyrain" -> 65
            "rainshowers", "lightrainshowers" -> 80
            "heavyrainshowers" -> 82
            "lightsnow" -> 71
            "snow" -> 73
            "heavysnow" -> 75
            "snowshowers", "lightsnowshowers" -> 85
            "ts", "tsrain" -> 95
            else -> null
        }
    }

    /** 7timer direction (N/NE/...) → градусы; неизвестное → null. */
    fun directionToDegrees(direction: String?): Double? = when (direction?.trim()?.uppercase()) {
        "N" -> 0.0
        "NNE" -> 22.5
        "NE" -> 45.0
        "ENE" -> 67.5
        "E" -> 90.0
        "ESE" -> 112.5
        "SE" -> 135.0
        "SSE" -> 157.5
        "S" -> 180.0
        "SSW" -> 202.5
        "SW" -> 225.0
        "WSW" -> 247.5
        "W" -> 270.0
        "WNW" -> 292.5
        "NW" -> 315.0
        "NNW" -> 337.5
        else -> null
    }

    /**
     * 7timer speed — баллы Бофорта (1..8). Переводим в м/с по стандартным
     * средним значениям, чтобы домен оставался в м/с и не выдавал «баллы» за м/с.
     */
    fun beaufortToMetersPerSecond(speed: Int?): Double? {
        if (speed == null) return null
        return when (speed) {
            0 -> 0.0
            1 -> 0.8
            2 -> 2.0
            3 -> 3.6
            4 -> 5.7
            5 -> 8.0
            6 -> 10.8
            7 -> 13.9
            8 -> 17.2
            9 -> 20.8
            10 -> 24.5
            11 -> 28.5
            else -> 32.7
        }
    }
}
