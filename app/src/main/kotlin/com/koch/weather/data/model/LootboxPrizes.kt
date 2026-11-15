package com.koch.weather.data.model

import java.util.UUID

/**
 * Вид погоды, который выпадает в лутбоксе (item 14 фазы 5). Классификация
 * опирается на уже существующий [WmoHelper] — отдельная таблица WMO-кодов
 * здесь не заводится, чтобы не было двух источников правды.
 */
enum class LootWeatherKind(
    val wordRu: String,
    val verbRu: String,
    val adviceRu: String
) {
    RAIN("дождь", "выпал", "Возьмите зонт: без него прогулка станет короче."),
    SNOW("снег", "выпал", "Наденьте тёплую куртку и обувь с нескользящей подошвой."),
    THUNDERSTORM("гроза", "выпала", "Грозу лучше переждать в помещении."),
    FOG("туман", "выпал", "Включите фары и держите дистанцию."),
    SUN("солнце", "выпало", "Возьмите солнцезащитные очки."),
    CLOUDS("облачная погода", "выпала", "Зонт не помешает на всякий случай.");

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
 * веса и тексты проверялись JVM-тестами без DataStore.
 *
 * Веса: iPhone 0.001 %, погодный приз 35 % при осадках в прогнозе на завтра
 * и 20 % при сухой погоде; остальное — темы/зонт/сапоги/кофе, кофе забирает
 * остаток до 100 %. Если прогноза нет (`tomorrow == null`), погодный приз
 * не выпадает вовсе — остальные веса пересчитываются сами собой.
 */
object LootboxPrizeFactory {

    const val IPHONE_WEIGHT: Double = 0.001
    const val WEATHER_WEIGHT_WET: Double = 35.0
    const val WEATHER_WEIGHT_DRY: Double = 20.0
    const val THEME_WEIGHT: Double = 15.0
    const val UMBRELLA_WEIGHT: Double = 20.0
    const val BOOTS_WEIGHT: Double = 18.0

    fun weatherPrizeWeight(tomorrow: DailyItem?): Double = when {
        tomorrow == null -> 0.0
        LootWeatherKind.hasPrecipitation(tomorrow.weatherCode) -> WEATHER_WEIGHT_WET
        else -> WEATHER_WEIGHT_DRY
    }

    /**
     * Крутит награду по [roll] из диапазона 0..100.
     * [themeIndex] — выбор темы (репозиторий передаёт случайный индекс пула),
     * поэтому при одном и том же roll результат детерминирован.
     */
    fun draw(
        roll: Double,
        tomorrow: DailyItem?,
        themePool: List<AppThemeMode>,
        themeIndex: Int = 0
    ): LootReward {
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

        val knownWeight = segments.sumOf { it.first }
        val coffeeWeight = (100.0 - knownWeight).coerceAtLeast(0.0)
        if (coffeeWeight > 0.0) {
            segments += coffeeWeight to { coffeeReward() }
        }

        val normalizedRoll = roll.coerceIn(0.0, 100.0 - 1e-9)
        var cumulative = 0.0
        for ((weight, make) in segments) {
            cumulative += weight
            if (normalizedRoll < cumulative) return make()
        }
        return coffeeReward()
    }

    /** Текст погодного приза: «Поздравляем! Вам выпал дождь на завтра». */
    fun weatherTitle(kind: LootWeatherKind, dayWordRu: String): String =
        "Поздравляем! Вам ${kind.verbRu} ${kind.wordRu} на $dayWordRu"

    /** «от +12° до +18°» — из реального суточного прогноза. */
    fun temperatureRangeRu(minTemp: Int, maxTemp: Int): String =
        "от ${signed(minTemp)}° до ${signed(maxTemp)}°"

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

    private fun iphoneReward(): LootReward = LootReward(
        id = UUID.randomUUID().toString(),
        title = "iPhone 16 Pro Max (1 ТБ)",
        subtitle = "НЕВЕРОЯТНО! Вы сорвали куш с шансом 0.001%! Заберите у курьера.",
        type = LootRewardType.IPHONE,
        rarity = LootRarity.MYTHIC
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
            id = UUID.randomUUID().toString(),
            title = weatherTitle(kind, dayWord),
            subtitle = "$range. ${kind.adviceRu}",
            type = LootRewardType.WEATHER_TOMORROW,
            rarity = LootRarity.RARE,
            weatherForecastTomorrow =
                "$description, днем ${tomorrow.maxTemp}°, ночью ${tomorrow.minTemp}°",
            weatherCode = tomorrow.weatherCode,
            temperatureRangeRu = range,
            dayLabelRu = dayWord
        )
    }

    private fun themeReward(theme: AppThemeMode): LootReward = LootReward(
        id = UUID.randomUUID().toString(),
        title = "Тема: «${theme.titleRu}»",
        subtitle = "Новое эксклюзивное оформление разблокировано во вкладке «Вид»!",
        type = LootRewardType.THEME,
        rarity = LootRarity.LEGENDARY,
        unlockedThemeName = theme.name
    )

    private fun umbrellaReward(): LootReward = LootReward(
        id = UUID.randomUUID().toString(),
        title = "Промокод: Зонт со скидкой 50%",
        subtitle = "Используйте промокод WEATHER2025 в магазине зонтиков!",
        type = LootRewardType.UMBRELLA_DISCOUNT,
        rarity = LootRarity.EPIC
    )

    private fun bootsReward(): LootReward = LootReward(
        id = UUID.randomUUID().toString(),
        title = "Резиновые сапоги «Стиль 2025»",
        subtitle = "Непромокаемая обувь высшей категории. Лужи больше не страшны!",
        type = LootRewardType.RUBBER_BOOTS,
        rarity = LootRarity.COMMON
    )

    private fun coffeeReward(): LootReward = LootReward(
        id = UUID.randomUUID().toString(),
        title = "Горячий капучино в Surf Coffee",
        subtitle = "Согревающий напиток для идеальной дождливой погоды.",
        type = LootRewardType.COFFEE,
        rarity = LootRarity.COMMON
    )
}
