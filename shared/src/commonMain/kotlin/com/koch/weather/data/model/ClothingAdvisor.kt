package com.koch.weather.data.model

/**
 * «Совет на сегодня» (item 12 фазы 5).
 *
 * Раньше на весь день был один пришитый текст, поэтому совет «не менялся».
 * Теперь на каждый температурный диапазон есть минимум 3 варианта, плюс
 * модификаторы по ветру (> 8 м/с), осадкам и солнцу. Выбор детерминирован
 * от города и дня года: при рекомпозиции совет не дёргается, но меняется
 * по дням и между городами.
 */
object ClothingAdvisor {

    private data class TempBand(
        val minTemp: Int,
        val maxTemp: Int,
        val variants: List<String>
    )

    private val bands: List<TempBand> = listOf(
        TempBand(
            minTemp = Int.MIN_VALUE,
            maxTemp = -11,
            variants = listOf(
                "Сильный мороз. Пуховик, тёплая шапка, шарф и варежки.",
                "Мороз крепчает: без пуховика и термобелья лучше не выходить.",
                "На улице мороз. Многослойная одежда и закрытые уши обязательны."
            )
        ),
        TempBand(
            minTemp = -10,
            maxTemp = -1,
            variants = listOf(
                "Морозно. Зимняя куртка, тёплая шапка и перчатки.",
                "Морозно и сухо: куртка на синтепоне, шапка и шарф.",
                "Морозно. Утеплитесь: зимняя куртка, перчатки, тёплые носки."
            )
        ),
        TempBand(
            minTemp = 0,
            maxTemp = 8,
            variants = listOf(
                "Холодно. Демисезонное пальто или тёплая куртка, шарф.",
                "Холодно. Куртка с капюшоном и плотные брюки.",
                "Холодно. Пальто, свитер и закрытая обувь."
            )
        ),
        TempBand(
            minTemp = 9,
            maxTemp = 15,
            variants = listOf(
                "Прохладно. Ветровка, худи или лёгкая куртка.",
                "Прохладно. Куртка поверх свитера — самый надёжный вариант.",
                "Прохладно. Легкая куртка и джинсы, утром свежо."
            )
        ),
        TempBand(
            minTemp = 16,
            maxTemp = 22,
            variants = listOf(
                "Комфортно. Лонгслив, лёгкая кофта или футболка с джинсами.",
                "Комфортно. Футболка и лёгкая рубашка поверх.",
                "Комфортно. Худи или лёгкая кофта — по вкусу."
            )
        ),
        TempBand(
            minTemp = 23,
            maxTemp = 28,
            variants = listOf(
                "Тепло. Футболка, шорты или лёгкие летние брюки.",
                "Тепло. Лёгкая одежда и открытая обувь.",
                "Тепло. Футболка и лёгкие брюки, головной убор не помешает."
            )
        ),
        TempBand(
            minTemp = 29,
            maxTemp = Int.MAX_VALUE,
            variants = listOf(
                "Жарко. Лёгкая светлая одежда, головной убор и солнцезащитные очки.",
                "Жарко. Минимум слоёв, светлые ткани и вода с собой.",
                "Жарко. Панама, лёгкая футболка и тень по возможности."
            )
        )
    )

    private val windVariants = listOf(
        " Ветер сильный: возьмите куртку с капюшоном.",
        " Ветер больше 8 м/с: застегнитесь и держите зонт крепче.",
        " Сильный ветер продувает насквозь — одевайтесь плотнее."
    )

    private val rainVariants = listOf(
        " Обязательно захватите зонт.",
        " Дождь: зонт и непромокаемая обувь.",
        " Возьмите зонт — иначе промокнете быстрее, чем дойдёте."
    )

    private val snowVariants = listOf(
        " Идёт снег, берегите тепло.",
        " Снег: тёплая куртка и обувь с нескользящей подошвой.",
        " На улице снег: шапка и перчатки не лишние."
    )

    private val thunderVariants = listOf(
        " Ожидается гроза, будьте осторожны.",
        " Гроза: не стойте под деревьями.",
        " Гроза — лучше переждать в помещении."
    )

    private val sunVariants = listOf(
        " Солнце яркое: возьмите солнцезащитные очки.",
        " Солнечно: крем от загара и очки.",
        " Солнце: головной убор защитит от перегрева."
    )

    private val iceVariants = listOf(
        " Ледяной дождь: обувь с цепкой подошвой.",
        " На дороге лёд — шаг короче, подошва цепче.",
        " Ледяная корка: держитесь подальше от края тротуара."
    )

    /** Детерминированный совет для города и календарного дня. */
    fun adviceFor(
        tempC: Int,
        weatherCode: Int,
        windSpeedMps: Double? = null,
        cityName: String = "",
        dayOfYear: Int = 0
    ): String {
        val seed = seedFor(cityName, dayOfYear)
        val band = bands.firstOrNull { tempC in it.minTemp..it.maxTemp } ?: bands.last()
        val base = band.variants[seed.mod(band.variants.size)]

        val builder = StringBuilder(base)
        if (windSpeedMps != null && windSpeedMps > WIND_THRESHOLD_MPS) {
            builder.append(windVariants[(seed + 7).mod(windVariants.size)])
        }
        when {
            weatherCode == 95 || weatherCode == 96 || weatherCode == 99 ->
                builder.append(thunderVariants[(seed + 11).mod(thunderVariants.size)])

            weatherCode == 66 || weatherCode == 67 ->
                builder.append(iceVariants[(seed + 11).mod(iceVariants.size)])
        }
        if (isRainCode(weatherCode)) {
            builder.append(rainVariants[(seed + 13).mod(rainVariants.size)])
        }
        if (isSnowCode(weatherCode)) {
            builder.append(snowVariants[(seed + 17).mod(snowVariants.size)])
        }
        if (isSunny(weatherCode) && tempC >= SUN_ADVICE_MIN_TEMP) {
            builder.append(sunVariants[(seed + 19).mod(sunVariants.size)])
        }
        return builder.toString()
    }

    fun variantsFor(tempC: Int): List<String> =
        (bands.firstOrNull { tempC in it.minTemp..it.maxTemp } ?: bands.last()).variants

    fun isRainCode(code: Int): Boolean = code in setOf(
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82
    )

    fun isSnowCode(code: Int): Boolean = code in setOf(71, 73, 75, 77, 85, 86)

    fun isSunny(code: Int): Boolean = code == 0 || code == 1

    /** Стабильный seed: имя города + день года. String.hashCode в JVM стабилен. */
    fun seedFor(cityName: String, dayOfYear: Int): Int =
        cityName.trim().lowercase().hashCode() + dayOfYear

    const val WIND_THRESHOLD_MPS: Double = 8.0

    const val SUN_ADVICE_MIN_TEMP: Int = 18
}
