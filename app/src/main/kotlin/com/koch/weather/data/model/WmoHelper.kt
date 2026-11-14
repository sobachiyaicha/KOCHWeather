package com.koch.weather.data.model

object WmoHelper {

    fun getDescriptionRu(code: Int): String {
        return when (code) {
            0 -> "Ясно"
            1 -> "Преимущественно ясно"
            2 -> "Переменная облачность"
            3 -> "Облачно"
            45, 48 -> "Туман"
            51, 53, 55 -> "Морось"
            56, 57 -> "Ледяная морось"
            61 -> "Небольшой дождь"
            63 -> "Умеренный дождь"
            65 -> "Сильный дождь"
            66, 67 -> "Ледяной дождь"
            71 -> "Небольшой снегопад"
            73 -> "Снегопад"
            75 -> "Сильный снегопад"
            77 -> "Снежные зерна"
            80, 81 -> "Ливневый дождь"
            82 -> "Сильный ливень"
            85, 86 -> "Ливневый снег"
            95 -> "Гроза"
            96, 99 -> "Гроза с градом"
            else -> "Облачно с прояснениями"
        }
    }

    fun getIconType(code: Int, isDay: Boolean = true): WeatherIconType {
        return when (code) {
            0 -> if (isDay) WeatherIconType.CLEAR_DAY else WeatherIconType.CLEAR_NIGHT
            1, 2 -> if (isDay) WeatherIconType.PARTLY_CLOUDY_DAY else WeatherIconType.PARTLY_CLOUDY_NIGHT
            3 -> WeatherIconType.CLOUDY
            45, 48 -> WeatherIconType.FOG
            51, 53, 55 -> WeatherIconType.DRIZZLE
            56, 57, 61, 63 -> WeatherIconType.RAIN
            65, 66, 67, 80, 81, 82 -> WeatherIconType.HEAVY_RAIN
            71, 73, 75, 77, 85, 86 -> WeatherIconType.SNOW
            95, 96, 99 -> WeatherIconType.THUNDERSTORM
            else -> if (isDay) WeatherIconType.PARTLY_CLOUDY_DAY else WeatherIconType.PARTLY_CLOUDY_NIGHT
        }
    }

    /**
     * Совет по одежде. Единственная реализация — [ClothingAdvisor]: 3+ варианта
     * на диапазон температур и модификаторы по ветру/осадкам/солнцу.
     * Этот метод оставлен для вызовов без города/дня (детали дня, старые тесты).
     */
    fun getClothingAdvice(temp: Int, weatherCode: Int): String =
        ClothingAdvisor.adviceFor(temp, weatherCode)

}
