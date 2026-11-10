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

    fun getClothingAdvice(temp: Int, weatherCode: Int): String {
        val baseAdvice = when {
            temp < -10 -> "Сильный мороз! Пуховик, теплая шапка, шарф и варежки."
            temp in -10..-1 -> "Морозно. Зимняя куртка, теплая шапка и перчатки."
            temp in 0..8 -> "Холодно. Демисезонное пальто или теплая куртка, шарф."
            temp in 9..15 -> "Прохладно. Ветровка, худи или легкая куртка."
            temp in 16..22 -> "Комфортно. Лонгслив, легкая кофта или футболка с джинсами."
            temp in 23..28 -> "Тепло. Футболка, шорты или легкие летние брюки."
            else -> "Жара! Легкая светлая одежда, головной убор и солнцезащитные очки."
        }

        val extraAdvice = when (weatherCode) {
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> " Обязательно захватите зонт!"
            66, 67 -> " На улице ледяной дождь, надевайте обувь с цепкой подошвой!"
            71, 73, 75, 77, 85, 86 -> " Идет снег, берегите тепло!"
            95, 96, 99 -> " Ожидается гроза, будьте осторожны!"
            else -> ""
        }

        return baseAdvice + extraAdvice
    }

    fun getTrafficAdvice(weatherCode: Int, temp: Int = 10): Pair<Int, String> {
        if (temp <= 0 && weatherCode in listOf(51, 53, 55, 61, 63, 65, 66, 67)) {
            return 8 to "Осторожно, гололедица! Пробки 8/10, держите дистанцию"
        }
        return when (weatherCode) {
            0, 1 -> 3 to "Дороги сухие. Движение свободное (3/10)"
            2, 3 -> 5 to "Стандартный трафик. Пробки 5/10"
            51, 53, 55 -> 6 to "Влажный асфальт. Пробки 6/10, держите дистанцию"
            61, 63 -> 7 to "Мокрое покрытие, видимость снижена. Пробки 7/10"
            65, 66, 67, 80, 81, 82 -> 8 to "Ливни и лужи, пробки 8/10. Выезжайте заранее"
            71, 73, 75, 77, 85, 86 -> 9 to "Снегопад и гололедица! Пробки 9/10, лучше на метро"
            95, 96, 99 -> 9 to "Штормовое предупреждение, гроза. Пробки 9/10"
            else -> 5 to "Средняя загрузка дорог (5/10)"
        }
    }
}
