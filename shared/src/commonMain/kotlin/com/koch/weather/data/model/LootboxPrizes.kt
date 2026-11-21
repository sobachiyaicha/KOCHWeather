package com.koch.weather.data.model

/**
 * Вид погоды, который выпадает в лутбоксе (item 14 фазы 5). Классификация
 * опирается на уже существующий [WmoHelper] — отдельная таблица WMO-кодов
 * здесь не заводится, чтобы не было двух источников правды.
 */
enum class LootWeatherKind(
    val wordRu: String,
    val verbRu: String,
    val adviceRu: String,
    val gender: LootGender
) {
    RAIN("дождь", "выпал", "Возьмите зонт: без него прогулка станет короче.", LootGender.MASCULINE),
    SNOW("снег", "выпал", "Наденьте тёплую куртку и обувь с нескользящей подошвой.", LootGender.MASCULINE),
    THUNDERSTORM("гроза", "выпала", "Грозу лучше переждать в помещении.", LootGender.FEMININE),
    FOG("туман", "выпал", "Включите фары и держите дистанцию.", LootGender.MASCULINE),
    SUN("солнце", "выпало", "Возьмите солнцезащитные очки.", LootGender.NEUTER),
    CLOUDS("облачная погода", "выпала", "Зонт не помешает на всякий случай.", LootGender.FEMININE);

    companion object {
        fun fromIcon(icon: WeatherIconType): LootWeatherKind = when (icon) {
            WeatherIconType.DRIZZLE,
            WeatherIconType.RAIN,
            WeatherIconType.HEAVY_RAIN -> RAIN

            WeatherIconType.SNOW -> SNOW
            WeatherIconType.THUNDERSTORM -> THUNDERSTORM
            WeatherIconType.FOG -> FOG
            WeatherIconType.CLEAR_DAY,
            WeatherIconType.CLEAR_NIGHT -> SUN

            WeatherIconType.CLOUDY,
            WeatherIconType.PARTLY_CLOUDY_DAY,
            WeatherIconType.PARTLY_CLOUDY_NIGHT -> CLOUDS
        }

        /** Вид приза по WMO-коду реального прогноза. */
        fun fromWeatherCode(code: Int): LootWeatherKind =
            fromIcon(WmoHelper.getIconType(code, isDay = true))

        /** Осадки по WMO: коды 51–99 (морось, дождь, снег, ливни, гроза). */
        fun hasPrecipitation(code: Int): Boolean = code in 51..99
    }
}

/**
 * Чистая фабрика наград лутбокса. Отделена от [LootboxRepository], чтобы
 * сценарий первых дропов и веса проверялись JVM-тестами без DataStore.
 *
 * Первые четыре дропа — сценарные, индекс = числу уже выпавших наград
 * (`history.size`): «мать с окна» (100 %, без случайности), затем контракт,
 * сын Мияги и шанс на додеп. Начиная с пятого дропа работает взвешенный пул:
 * iPhone 0.001, погодный приз 35 % при осадках в прогнозе на завтра и 20 %
 * при сухой погоде, темы/зонт/сапоги и придуманные призы. Веса относительные:
 * выбор нормализуется на их сумму, поэтому ни один приз не «забирает остаток».
 * Если прогноза нет (`tomorrow == null`), погодный приз не выпадает вовсе.
 */
object LootboxPrizeFactory {

    const val IPHONE_WEIGHT: Double = 0.001
    const val WEATHER_WEIGHT_WET: Double = 35.0
    const val WEATHER_WEIGHT_DRY: Double = 20.0
    const val THEME_WEIGHT: Double = 15.0
    const val UMBRELLA_WEIGHT: Double = 20.0
    const val BOOTS_WEIGHT: Double = 18.0

    // «Мои» призы пула.
    const val RAIN_PASS_WEIGHT: Double = 8.0
    const val GOOD_DAY_WEIGHT: Double = 8.0
    const val HAT_WEIGHT: Double = 8.0
    const val PUDDLE_WEIGHT: Double = 11.0
    const val CLOUD_WEIGHT: Double = 7.0

    /** Сколько первых дропов идёт строго по сценарию. */
    const val SCRIPTED_COUNT: Int = 4

    fun weatherPrizeWeight(tomorrow: DailyItem?): Double = when {
        tomorrow == null -> 0.0
        LootWeatherKind.hasPrecipitation(tomorrow.weatherCode) -> WEATHER_WEIGHT_WET
        else -> WEATHER_WEIGHT_DRY
    }

    /**
     * Сценарный приз по номеру дропа (0-based) или null, если сценарий кончился.
     * Первый в жизни дроп — «мать с окна», всегда и без случайности.
     */
    fun scriptedPrize(historySize: Int): LootReward? = when (historySize) {
        0 -> motherFromWindow()
        1 -> svoContract()
        2 -> miyagiSon()
        3 -> dodepChance()
        else -> null
    }

    /**
     * Сумма весов пула — для нормализации [draw]. [themePoolSize] = 0, когда
     * все темы уже открыты и сегмент темы из пула выпадает.
     */
    fun totalWeight(tomorrow: DailyItem?, themePoolSize: Int = 1): Double =
        IPHONE_WEIGHT +
            weatherPrizeWeight(tomorrow) +
            (if (themePoolSize > 0) THEME_WEIGHT else 0.0) +
            UMBRELLA_WEIGHT + BOOTS_WEIGHT + RAIN_PASS_WEIGHT + GOOD_DAY_WEIGHT +
            HAT_WEIGHT + PUDDLE_WEIGHT + CLOUD_WEIGHT

    /**
     * Крутит награду по [roll] из диапазона 0..100.
     *
     * [historySize] — сколько наград уже выпало: индексы 0..3 отдают сценарные
     * призы, дальше — взвешенный пул. [themeIndex] — выбор темы (репозиторий
     * передаёт случайный индекс пула), поэтому при одном и том же roll
     * результат детерминирован.
     */
    fun draw(
        historySize: Int,
        roll: Double,
        tomorrow: DailyItem?,
        themePool: List<AppThemeMode>,
        themeIndex: Int = 0
    ): LootReward {
        scriptedPrize(historySize)?.let { return it }
        return drawFromPool(roll, tomorrow, themePool, themeIndex)
    }

    /** Пул «пятого и далее» дропа: взвешенный выбор по [roll] из 0..100. */
    fun drawFromPool(
        roll: Double,
        tomorrow: DailyItem?,
        themePool: List<AppThemeMode>,
        themeIndex: Int = 0
    ): LootReward {
        val segments = poolSegments(tomorrow, themePool, themeIndex)
        val total = segments.sumOf { it.first }
        val normalizedRoll = roll.coerceIn(0.0, 100.0 - 1e-9) / 100.0 * total

        var cumulative = 0.0
        for ((weight, make) in segments) {
            cumulative += weight
            if (normalizedRoll < cumulative) return make()
        }
        return segments.last().second()
    }

    private fun poolSegments(
        tomorrow: DailyItem?,
        themePool: List<AppThemeMode>,
        themeIndex: Int
    ): List<Pair<Double, () -> LootReward>> {
        val segments = mutableListOf<Pair<Double, () -> LootReward>>()
        segments += IPHONE_WEIGHT to { iphoneReward() }

        val weatherWeight = weatherPrizeWeight(tomorrow)
        if (tomorrow != null && weatherWeight > 0.0) {
            segments += weatherWeight to { weatherReward(tomorrow) }
        }
        if (themePool.isNotEmpty()) {
            segments += THEME_WEIGHT to {
                val theme = themePool[themeIndex.mod(themePool.size)]
                themeReward(theme)
            }
        }
        segments += UMBRELLA_WEIGHT to { umbrellaReward() }
        segments += BOOTS_WEIGHT to { bootsReward() }
        segments += RAIN_PASS_WEIGHT to { rainPassReward() }
        segments += GOOD_DAY_WEIGHT to { goodDayReward() }
        segments += HAT_WEIGHT to { hatReward() }
        segments += PUDDLE_WEIGHT to { puddleReward() }
        segments += CLOUD_WEIGHT to { cloudReward() }
        return segments
    }

    /** Текст погодного приза: «Поздравляем! Вам выпал дождь на завтра». */
    fun weatherTitle(kind: LootWeatherKind, dayWordRu: String): String =
        "Поздравляем! Вам ${kind.verbRu} ${kind.wordRu} на $dayWordRu"

    /** «от +12° до +18°» — из реального суточного прогноза. */
    fun temperatureRangeRu(minTemp: Int, maxTemp: Int): String =
        "от ${signed(minTemp)}° до ${signed(maxTemp)}°"

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

    // ----------------------------------------------------------------------
    // Сценарий первых дропов
    // ----------------------------------------------------------------------

    private fun motherFromWindow(): LootReward = LootReward(
        id = "mother_from_window",
        title = "мать с окна",
        subtitle = "прямо с окна, аккуратно",
        type = LootRewardType.MOTHER_FROM_WINDOW,
        rarity = LootRarity.LEGENDARY,
        gender = LootGender.FEMININE
    )

    private fun svoContract(): LootReward = LootReward(
        id = "svo_contract",
        title = "бессрочный контракт на сво",
        subtitle = "подписан до конца, обратной стороны нет",
        type = LootRewardType.SVO_CONTRACT,
        rarity = LootRarity.MYTHIC,
        gender = LootGender.MASCULINE
    )

    private fun miyagiSon(): LootReward = LootReward(
        id = "miyagi_son",
        title = "сын мияги из окна",
        subtitle = "смотрит с окна, уважает",
        type = LootRewardType.MIYAGI_SON,
        rarity = LootRarity.EPIC,
        gender = LootGender.MASCULINE
    )

    /**
     * «Шанс на додеп» не только приз, но и +1 попытка: [LootReward.grantsExtraAttempt]
     * даёт UI повод написать «+1 попытка», а репозиторий возвращает попытку в счётчик.
     */
    private fun dodepChance(): LootReward = LootReward(
        id = "dodep_chance",
        title = "шанс на додеп",
        subtitle = "сверх лимита: +1 попытка",
        type = LootRewardType.DODEP_CHANCE,
        rarity = LootRarity.RARE,
        gender = LootGender.MASCULINE,
        grantsExtraAttempt = true
    )

    // ----------------------------------------------------------------------
    // Пул «пятого и далее» дропа: «мои» призы
    // ----------------------------------------------------------------------

    private fun rainPassReward(): LootReward = LootReward(
        id = "rain_pass_lifetime",
        title = "пожизненный абонемент на дождь",
        subtitle = "небо теперь работает по подписке",
        type = LootRewardType.RAIN_PASS,
        rarity = LootRarity.EPIC,
        gender = LootGender.MASCULINE
    )

    private fun goodDayReward(): LootReward = LootReward(
        id = "good_day_cert",
        title = "сертификат на один хороший день",
        subtitle = "обмену не подлежит, но день будет",
        type = LootRewardType.GOOD_DAY_CERT,
        rarity = LootRarity.RARE,
        gender = LootGender.MASCULINE
    )

    private fun hatReward(): LootReward = LootReward(
        id = "cloud_invisibility_hat",
        title = "шапка-невидимка для туч",
        subtitle = "облака перестанут вас видеть",
        type = LootRewardType.CLOUD_INVISIBILITY_HAT,
        rarity = LootRarity.RARE,
        gender = LootGender.FEMININE
    )

    private fun puddleReward(): LootReward = LootReward(
        id = "courtyard_puddle",
        title = "лужа во дворе, бессрочно",
        subtitle = "ваша собственная. с отражением.",
        type = LootRewardType.COURTYARD_PUDDLE,
        rarity = LootRarity.COMMON,
        gender = LootGender.FEMININE
    )

    private fun cloudReward(): LootReward = LootReward(
        id = "personal_cloud",
        title = "персональная туча над головой",
        subtitle = "всегда с вами, в любую погоду",
        type = LootRewardType.PERSONAL_CLOUD,
        rarity = LootRarity.EPIC,
        gender = LootGender.FEMININE
    )

    // ----------------------------------------------------------------------
    // Прежние призы
    // ----------------------------------------------------------------------

    private fun iphoneReward(): LootReward = LootReward(
        id = "iphone_16_pro_max",
        title = "iPhone 16 Pro Max (1 ТБ)",
        subtitle = "НЕВЕРОЯТНО! Вы сорвали куш с шансом 0.001%! Заберите у курьера.",
        type = LootRewardType.IPHONE,
        rarity = LootRarity.MYTHIC,
        gender = LootGender.MASCULINE
    )

    private fun weatherReward(tomorrow: DailyItem): LootReward {
        val kind = LootWeatherKind.fromWeatherCode(tomorrow.weatherCode)
        // Если приз выдан по сегодняшнему дню (прогноза на завтра не было),
        // честно пишем «на сегодня», а не выдаём сегодняшнюю погоду за завтрашнюю.
        val dayWord = if (tomorrow.dayLabel.startsWith("Сегодня", ignoreCase = true)) {
            "сегодня"
        } else {
            "завтра"
        }
        val range = temperatureRangeRu(tomorrow.minTemp, tomorrow.maxTemp)
        val description = WmoHelper.getDescriptionRu(tomorrow.weatherCode)
        return LootReward(
            id = "weather_tomorrow_${tomorrow.weatherCode}_${tomorrow.minTemp}_${tomorrow.maxTemp}",
            title = weatherTitle(kind, dayWord),
            subtitle = "$range. ${kind.adviceRu}",
            type = LootRewardType.WEATHER_TOMORROW,
            rarity = LootRarity.RARE,
            weatherForecastTomorrow =
                "$description, днем ${tomorrow.maxTemp}°, ночью ${tomorrow.minTemp}°",
            weatherCode = tomorrow.weatherCode,
            temperatureRangeRu = range,
            dayLabelRu = dayWord,
            gender = kind.gender
        )
    }

    private fun themeReward(theme: AppThemeMode): LootReward = LootReward(
        id = "theme_${theme.name}",
        title = "Тема: «${theme.titleRu}»",
        subtitle = "Новое эксклюзивное оформление разблокировано во вкладке «Вид»!",
        type = LootRewardType.THEME,
        rarity = LootRarity.LEGENDARY,
        unlockedThemeName = theme.name,
        gender = LootGender.FEMININE
    )

    private fun umbrellaReward(): LootReward = LootReward(
        id = "umbrella_discount",
        title = "Промокод: Зонт со скидкой 50%",
        subtitle = "Используйте промокод WEATHER2025 в магазине зонтиков!",
        type = LootRewardType.UMBRELLA_DISCOUNT,
        rarity = LootRarity.EPIC,
        gender = LootGender.MASCULINE
    )

    /**
     * Название в единственном числе: глагол согласуется с родом приза
     * («выпал резиновый сапог»), а не с множественным «сапоги».
     */
    private fun bootsReward(): LootReward = LootReward(
        id = "rubber_boots",
        title = "Резиновый сапог «Стиль 2025»",
        subtitle = "Непромокаемая обувь высшей категории. Лужи больше не страшны!",
        type = LootRewardType.RUBBER_BOOTS,
        rarity = LootRarity.COMMON,
        gender = LootGender.MASCULINE
    )
}
