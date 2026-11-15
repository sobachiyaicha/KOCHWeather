package com.koch.weather.data

import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.LootReward
import com.koch.weather.data.model.LootRewardType
import com.koch.weather.data.model.LootboxHistoryCodec
import com.koch.weather.data.model.LootboxPrizeFactory
import com.koch.weather.data.model.LootboxState
import com.koch.weather.data.model.ThemeCatalog
import com.koch.weather.data.model.WeatherIconType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Item 14 фазы 5: в лутбоксе выпадают ВИДЫ погоды по реальному прогнозу,
 * история дропов переживает перезапуск, seq растёт на каждый дроп.
 */
class LootboxPrizeFactoryTest {

    private val themes = ThemeCatalog.lootboxRewardPool(ThemeCatalog.DEFAULT_UNLOCKED)

    private fun day(
        code: Int,
        icon: WeatherIconType,
        min: Int = 10,
        max: Int = 16,
        label: String = "Пн"
    ) = DailyItem(
        dayLabel = label,
        weatherCode = code,
        iconType = icon,
        pop = 80,
        minTemp = min,
        maxTemp = max
    )

    private val rainTomorrow = day(61, WeatherIconType.RAIN)
    private val snowTomorrow = day(71, WeatherIconType.SNOW, min = -5, max = -1)
    private val sunnyTomorrow = day(0, WeatherIconType.CLEAR_DAY, min = 18, max = 27)

    /** Roll сразу после iPhone, внутри погодного отрезка. */
    private fun weatherRoll(weight: Double): Double = LootboxPrizeFactory.IPHONE_WEIGHT + weight / 2.0

    @Test
    fun rainForecast_givesTextWithRainTomorrow() {
        val reward = LootboxPrizeFactory.draw(
            roll = weatherRoll(LootboxPrizeFactory.WEATHER_WEIGHT_WET),
            tomorrow = rainTomorrow,
            themePool = themes
        )
        assertEquals(LootRewardType.WEATHER_TOMORROW, reward.type)
        assertTrue(
            "Текст должен содержать «дождь на завтра», получено: ${reward.title}",
            reward.title.contains("дождь на завтра")
        )
        assertEquals(61, reward.weatherCode)
        assertEquals("завтра", reward.dayLabelRu)
        assertEquals("от +10° до +16°", reward.temperatureRangeRu)
        assertTrue(reward.subtitle.contains("зонт"))
    }

    @Test
    fun snowForecast_givesSnowPrize() {
        val reward = LootboxPrizeFactory.draw(
            roll = weatherRoll(LootboxPrizeFactory.WEATHER_WEIGHT_WET),
            tomorrow = snowTomorrow,
            themePool = themes
        )
        assertEquals(LootRewardType.WEATHER_TOMORROW, reward.type)
        assertTrue(reward.title.contains("снег на завтра"))
        assertEquals("от -5° до -1°", reward.temperatureRangeRu)
    }

    @Test
    fun dryForecast_doesNotGiveRain() {
        val reward = LootboxPrizeFactory.draw(
            roll = weatherRoll(LootboxPrizeFactory.WEATHER_WEIGHT_DRY),
            tomorrow = sunnyTomorrow,
            themePool = themes
        )
        assertEquals(LootRewardType.WEATHER_TOMORROW, reward.type)
        assertEquals(0, reward.weatherCode)
        assertFalse(reward.title.contains("дождь"))
        assertFalse(reward.title.contains("снег"))
        assertTrue(reward.title.contains("солнце на завтра"))
        assertTrue(reward.subtitle.contains("очки"))
    }

    @Test
    fun noForecast_neverGivesWeatherPrize() {
        assertEquals(0.0, LootboxPrizeFactory.weatherPrizeWeight(null), 0.0001)

        var roll = 0.0
        while (roll < 100.0) {
            val reward = LootboxPrizeFactory.draw(roll, tomorrow = null, themePool = themes)
            assertNotEquals(
                "Без прогноза погодный приз не выпадает (roll=$roll)",
                LootRewardType.WEATHER_TOMORROW,
                reward.type
            )
            roll += 0.05
        }
    }

    @Test
    fun weatherPrizeWeights_dependOnForecastPrecipitation() {
        assertEquals(
            LootboxPrizeFactory.WEATHER_WEIGHT_WET,
            LootboxPrizeFactory.weatherPrizeWeight(rainTomorrow),
            0.0001
        )
        assertEquals(
            LootboxPrizeFactory.WEATHER_WEIGHT_WET,
            LootboxPrizeFactory.weatherPrizeWeight(snowTomorrow),
            0.0001
        )
        assertEquals(
            LootboxPrizeFactory.WEATHER_WEIGHT_DRY,
            LootboxPrizeFactory.weatherPrizeWeight(sunnyTomorrow),
            0.0001
        )
        assertEquals(0.0, LootboxPrizeFactory.weatherPrizeWeight(null), 0.0001)
        assertTrue(LootboxPrizeFactory.WEATHER_WEIGHT_DRY < LootboxPrizeFactory.WEATHER_WEIGHT_WET)
    }

    @Test
    fun iphoneChance_isStillTiny() {
        val reward = LootboxPrizeFactory.draw(
            roll = 0.0005,
            tomorrow = rainTomorrow,
            themePool = themes
        )
        assertEquals(LootRewardType.IPHONE, reward.type)
        assertTrue(reward.subtitle.contains("0.001"))
        assertEquals(0.001, LootboxPrizeFactory.IPHONE_WEIGHT, 0.0000001)
    }

    @Test
    fun dropSequence_incrementsSeqAndCapsHistory() {
        var state = LootboxState()
        val reward = LootReward(
            id = "r1",
            title = "Тест",
            subtitle = "Проверка",
            type = LootRewardType.COFFEE,
            rarity = com.koch.weather.data.model.LootRarity.COMMON
        )
        state = state.withDrop(reward)
        assertEquals(1, state.lastRewardSeq)
        assertEquals(1, state.totalOpened)
        assertEquals(1, state.history.size)
        assertEquals(reward, state.lastReward)

        repeat(LootboxState.MAX_HISTORY + 5) { index ->
            state = state.withDrop(reward.copy(id = "r-$index"))
        }
        assertEquals(LootboxState.MAX_HISTORY, state.history.size)
        assertEquals(LootboxState.MAX_HISTORY + 6, state.lastRewardSeq)
        assertEquals("r-24", state.history.first().id)
    }

    @Test
    fun history_roundTripsThroughJsonAndSurvivesGarbage() {
        val history = (1..3).map { index ->
            LootReward(
                id = "id-$index",
                title = "Поздравляем! Вам выпал дождь на завтра",
                subtitle = "от +10° до +16°. Возьмите зонт.",
                type = LootRewardType.WEATHER_TOMORROW,
                rarity = com.koch.weather.data.model.LootRarity.RARE,
                weatherCode = 61,
                temperatureRangeRu = "от +10° до +16°",
                dayLabelRu = "завтра",
                timestamp = 1_763_000_000_000L + index
            )
        }
        val encoded = LootboxHistoryCodec.encode(history)
        val decoded = LootboxHistoryCodec.decode(encoded)
        assertEquals(history, decoded)
        assertEquals(61, decoded.first().weatherCode)

        assertTrue(LootboxHistoryCodec.decode(null).isEmpty())
        assertTrue(LootboxHistoryCodec.decode("").isEmpty())
        assertTrue(LootboxHistoryCodec.decode("not json").isEmpty())
    }

    @Test
    fun historyCodec_capsAtTwentyEntries() {
        val rewards = (1..25).map { index ->
            LootReward(
                id = "id-$index",
                title = "Приз $index",
                subtitle = "Подзаголовок",
                type = LootRewardType.COFFEE,
                rarity = com.koch.weather.data.model.LootRarity.COMMON
            )
        }
        val decoded = LootboxHistoryCodec.decode(LootboxHistoryCodec.encode(rewards))
        assertEquals(LootboxState.MAX_HISTORY, decoded.size)
        assertEquals("id-1", decoded.first().id)
    }
}
