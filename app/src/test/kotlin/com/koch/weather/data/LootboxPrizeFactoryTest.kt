package com.koch.weather.data

import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.LootGender
import com.koch.weather.data.model.LootRarity
import com.koch.weather.data.model.LootReward
import com.koch.weather.data.model.LootRewardType
import com.koch.weather.data.model.LootboxAttemptsPolicy
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
 * Лутбокс: первые четыре дропа идут по сценарию («мать с окна» — всегда первая),
 * дальше — взвешенный пул без кофе; род приза согласует «выпал/выпала/выпало»;
 * попытки (5 бесплатных, +1 за додеп, +1 за рекламу с потолком) считаются здесь
 * же, потому что политика вынесена из репозитория и не требует DataStore.
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

    /** Roll (0..100), который попадает в весовой отрезок на позиции [cumulativeWeight]. */
    private fun rollFor(cumulativeWeight: Double, tomorrow: DailyItem?): Double =
        cumulativeWeight * 100.0 / LootboxPrizeFactory.totalWeight(tomorrow)

    /** Roll сразу после iPhone, внутри погодного отрезка. */
    private fun weatherRoll(weight: Double, tomorrow: DailyItem?): Double =
        rollFor(LootboxPrizeFactory.IPHONE_WEIGHT + weight / 2.0, tomorrow)

    /** Пятый и далее дроп (сценарий из четырёх уже отыгран). */
    private fun poolDraw(roll: Double, tomorrow: DailyItem? = rainTomorrow): LootReward =
        LootboxPrizeFactory.draw(
            historySize = LootboxPrizeFactory.SCRIPTED_COUNT,
            roll = roll,
            tomorrow = tomorrow,
            themePool = themes
        )

    // ------------------------------------------------------------------
    // Сценарий первых дропов
    // ------------------------------------------------------------------

    @Test
    fun firstEverDrop_isAlwaysMotherFromWindow() {
        var roll = 0.0
        while (roll < 100.0) {
            val reward = LootboxPrizeFactory.draw(
                historySize = 0,
                roll = roll,
                tomorrow = rainTomorrow,
                themePool = themes
            )
            assertEquals(LootRewardType.MOTHER_FROM_WINDOW, reward.type)
            assertEquals("мать с окна", reward.title)
            assertEquals("прямо с окна, аккуратно", reward.subtitle)
            assertEquals(LootRarity.LEGENDARY, reward.rarity)
            assertEquals(LootGender.FEMININE, reward.gender)
            assertEquals("выпала", reward.revealVerbRu)
            roll += 12.5
        }
    }

    @Test
    fun firstFourDrops_followScriptedOrder() {
        val expected = listOf(
            LootRewardType.MOTHER_FROM_WINDOW to "мать с окна",
            LootRewardType.SVO_CONTRACT to "бессрочный контракт на сво",
            LootRewardType.MIYAGI_SON to "сын мияги из окна",
            LootRewardType.DODEP_CHANCE to "шанс на додеп"
        )
        expected.forEachIndexed { index, (type, title) ->
            // Даже самый «крайний» roll не должен перебить сценарий.
            listOf(0.0, 50.0, 99.99).forEach { roll ->
                val reward = LootboxPrizeFactory.draw(
                    historySize = index,
                    roll = roll,
                    tomorrow = rainTomorrow,
                    themePool = themes
                )
                assertEquals("дроп #${index + 1} (roll=$roll)", type, reward.type)
                assertEquals(title, reward.title)
            }
        }

        assertEquals(4, LootboxPrizeFactory.SCRIPTED_COUNT)
        assertEquals(LootRewardType.MOTHER_FROM_WINDOW, LootboxPrizeFactory.scriptedPrize(0)?.type)
        assertEquals(LootRewardType.SVO_CONTRACT, LootboxPrizeFactory.scriptedPrize(1)?.type)
        assertEquals(LootRewardType.MIYAGI_SON, LootboxPrizeFactory.scriptedPrize(2)?.type)
        assertEquals(LootRewardType.DODEP_CHANCE, LootboxPrizeFactory.scriptedPrize(3)?.type)
        assertNull(LootboxPrizeFactory.scriptedPrize(LootboxPrizeFactory.SCRIPTED_COUNT))
    }

    @Test
    fun scriptedRaritiesAndGenders_matchSpec() {
        assertEquals(LootRarity.LEGENDARY, LootboxPrizeFactory.scriptedPrize(0)?.rarity)
        assertEquals(LootGender.FEMININE, LootboxPrizeFactory.scriptedPrize(0)?.gender)

        assertEquals(LootRarity.MYTHIC, LootboxPrizeFactory.scriptedPrize(1)?.rarity)
        assertEquals(LootGender.MASCULINE, LootboxPrizeFactory.scriptedPrize(1)?.gender)

        assertEquals(LootRarity.EPIC, LootboxPrizeFactory.scriptedPrize(2)?.rarity)
        assertEquals(LootGender.MASCULINE, LootboxPrizeFactory.scriptedPrize(2)?.gender)

        assertEquals(LootRarity.RARE, LootboxPrizeFactory.scriptedPrize(3)?.rarity)
        assertEquals(LootGender.MASCULINE, LootboxPrizeFactory.scriptedPrize(3)?.gender)
    }

    @Test
    fun fifthDrop_comesFromPoolAndCoversAllPrizes() {
        val scripted = (0 until LootboxPrizeFactory.SCRIPTED_COUNT)
            .mapNotNull { LootboxPrizeFactory.scriptedPrize(it)?.type }
            .toSet()

        val seen = mutableSetOf<LootRewardType>()
        var roll = 0.0
        while (roll <= 100.0) {
            val reward = poolDraw(roll)
            assertFalse("Пятый дроп не может быть сценарным: ${reward.type}", reward.type in scripted)
            seen += reward.type
            roll += 0.05
        }
        assertEquals(
            setOf(
                LootRewardType.IPHONE,
                LootRewardType.WEATHER_TOMORROW,
                LootRewardType.THEME,
                LootRewardType.UMBRELLA_DISCOUNT,
                LootRewardType.RUBBER_BOOTS,
                LootRewardType.RAIN_PASS,
                LootRewardType.GOOD_DAY_CERT,
                LootRewardType.CLOUD_INVISIBILITY_HAT,
                LootRewardType.COURTYARD_PUDDLE,
                LootRewardType.PERSONAL_CLOUD
            ),
            seen
        )
    }

    @Test
    fun inventedPrizes_haveRequiredTextsRarityAndGender() {
        fun byType(type: LootRewardType): LootReward {
            var roll = 0.0
            while (roll <= 100.0) {
                val reward = poolDraw(roll)
                if (reward.type == type) return reward
                roll += 0.01
            }
            throw AssertionError("Приз $type не выпал ни на одном roll")
        }

        val rainPass = byType(LootRewardType.RAIN_PASS)
        assertEquals("пожизненный абонемент на дождь", rainPass.title)
        assertEquals(LootRarity.EPIC, rainPass.rarity)
        assertEquals(LootGender.MASCULINE, rainPass.gender)

        val goodDay = byType(LootRewardType.GOOD_DAY_CERT)
        assertEquals("сертификат на один хороший день", goodDay.title)
        assertEquals(LootRarity.RARE, goodDay.rarity)
        assertEquals(LootGender.MASCULINE, goodDay.gender)

        val hat = byType(LootRewardType.CLOUD_INVISIBILITY_HAT)
        assertEquals("шапка-невидимка для туч", hat.title)
        assertEquals(LootRarity.RARE, hat.rarity)
        assertEquals(LootGender.FEMININE, hat.gender)

        val puddle = byType(LootRewardType.COURTYARD_PUDDLE)
        assertEquals("лужа во дворе, бессрочно", puddle.title)
        assertEquals(LootRarity.COMMON, puddle.rarity)
        assertEquals(LootGender.FEMININE, puddle.gender)

        val cloud = byType(LootRewardType.PERSONAL_CLOUD)
        assertEquals("персональная туча над головой", cloud.title)
        assertEquals(LootRarity.EPIC, cloud.rarity)
        assertEquals(LootGender.FEMININE, cloud.gender)
    }

    // ------------------------------------------------------------------
    // Кофе удалён из лутбокса полностью
    // ------------------------------------------------------------------

    @Test
    fun coffeePrize_isGoneFromTypesTextsAndPool() {
        assertFalse(
            "LootRewardType.COFFEE должен быть удалён",
            LootRewardType.values().any { it.name == "COFFEE" }
        )

        val allRewards = (0 until LootboxPrizeFactory.SCRIPTED_COUNT)
            .mapNotNull { LootboxPrizeFactory.scriptedPrize(it) } +
            run {
                val list = mutableListOf<LootReward>()
                var roll = 0.0
                while (roll <= 100.0) {
                    list += poolDraw(roll)
                    roll += 0.05
                }
                list
            }

        allRewards.forEach { reward ->
            val text = (reward.title + " " + reward.subtitle).lowercase()
            assertFalse("Кофейный приз вернулся: ${reward.title}", text.contains("кофе"))
            assertFalse("Кофейный приз вернулся: ${reward.title}", text.contains("капучино"))
            assertFalse("Промокод на кофе вернулся: ${reward.title}", text.contains("surf"))
        }
    }

    // ------------------------------------------------------------------
    // Род приза и глагол «выпал/выпала/выпало»
    // ------------------------------------------------------------------

    @Test
    fun revealVerbRu_agreesWithGender() {
        assertEquals("выпала", LootboxPrizeFactory.scriptedPrize(0)?.revealVerbRu)
        assertEquals("выпал", LootboxPrizeFactory.scriptedPrize(1)?.revealVerbRu)
        assertEquals("выпал", LootboxPrizeFactory.scriptedPrize(2)?.revealVerbRu)
        assertEquals("выпал", LootboxPrizeFactory.scriptedPrize(3)?.revealVerbRu)

        // Погодный приз берёт род у вида погоды: «выпало солнце», «выпала облачная погода».
        val sun = LootboxPrizeFactory.draw(
            historySize = LootboxPrizeFactory.SCRIPTED_COUNT,
            roll = weatherRoll(LootboxPrizeFactory.WEATHER_WEIGHT_DRY, sunnyTomorrow),
            tomorrow = sunnyTomorrow,
            themePool = themes
        )
        assertEquals(LootRewardType.WEATHER_TOMORROW, sun.type)
        assertEquals("выпало", sun.revealVerbRu)

        val neutral = LootReward(
            id = "neutral",
            title = "нечто",
            subtitle = "проверка",
            type = LootRewardType.RUBBER_BOOTS,
            rarity = LootRarity.COMMON
        )
        assertEquals(LootGender.NEUTER, neutral.gender)
        assertEquals("выпало", neutral.revealVerbRu)
    }

    // ------------------------------------------------------------------
    // Попытки: лимит, додеп, реклама
    // ------------------------------------------------------------------

    @Test
    fun attemptsCountDown_toFiveFreeAttempts() {
        assertEquals(5, LootboxState().freeAttemptsTotal)
        assertEquals(5, LootboxState().attemptsLeft)
        assertEquals(3, LootboxState(attemptsUsed = 2).attemptsLeft)
        assertEquals(0, LootboxState(attemptsUsed = 5).attemptsLeft)
        assertEquals(0, LootboxState(attemptsUsed = 7).attemptsLeft)
        assertEquals(LootboxState.FREE_ATTEMPTS_TOTAL, LootboxAttemptsPolicy.FREE_ATTEMPTS)

        assertTrue(LootboxAttemptsPolicy.canOpen(0))
        assertTrue(LootboxAttemptsPolicy.canOpen(4))
        assertFalse("После 5 попыток приз не выдаётся", LootboxAttemptsPolicy.canOpen(5))
        assertFalse(LootboxAttemptsPolicy.canOpen(6))

        // Премиум-пользователю лимит не писан.
        assertTrue(LootboxAttemptsPolicy.canOpen(99, isPremium = true))
        assertEquals(5, LootboxAttemptsPolicy.left(99, isPremium = true))
    }

    @Test
    fun dropSpendsAttempt_andWithDropKeepsCounter() {
        assertEquals(1, LootboxAttemptsPolicy.afterDrop(0))
        assertEquals(5, LootboxAttemptsPolicy.afterDrop(4))
        // Премиум счётчик не двигает.
        assertEquals(4, LootboxAttemptsPolicy.afterDrop(4, isPremium = true))

        val state = LootboxState(attemptsUsed = 2).withDrop(
            LootboxPrizeFactory.scriptedPrize(0)!!
        )
        assertEquals("withDrop не трогает счётчик попыток", 2, state.attemptsUsed)
        assertEquals(3, state.attemptsLeft)
    }

    @Test
    fun dodep_GrantsOneExtraAttempt() {
        val dodep = LootboxPrizeFactory.scriptedPrize(3)!!
        assertEquals(LootRewardType.DODEP_CHANCE, dodep.type)
        assertTrue("Додеп должен возвращать попытку", dodep.grantsExtraAttempt)
        assertFalse(LootboxPrizeFactory.scriptedPrize(0)!!.grantsExtraAttempt)

        // Потратили 3 из 5, выпал додеп: попытка списалась и сразу вернулась.
        val afterDrop = LootboxAttemptsPolicy.afterDrop(
            attemptsUsed = 3,
            grantsExtraAttempt = dodep.grantsExtraAttempt
        )
        assertEquals(3, afterDrop)
        assertEquals("Додеп даёт +1 попытку", 2, LootboxState(attemptsUsed = afterDrop).attemptsLeft)

        // Без додепа та же попытка просто тратится.
        val withoutDodep = LootboxAttemptsPolicy.afterDrop(attemptsUsed = 3)
        assertEquals(4, withoutDodep)
        assertEquals(1, LootboxState(attemptsUsed = withoutDodep).attemptsLeft)

        // Бонус можно получить и на нуле: остаток снова > 0.
        val afterEmpty = LootboxAttemptsPolicy.afterDrop(attemptsUsed = 5, grantsExtraAttempt = true)
        assertEquals(5, afterEmpty)
        assertEquals(0, LootboxState(attemptsUsed = afterEmpty).attemptsLeft)
    }

    @Test
    fun adAttempt_grantsOneAndIsCapped() {
        assertTrue(LootboxAttemptsPolicy.canGrantAdAttempt(0))
        assertEquals(6, LootboxState(attemptsUsed = LootboxAttemptsPolicy.grantAdAttempt(0)).attemptsLeft)

        // Пять бонусных — потолок, шестую реклама не даёт.
        var used = 0
        repeat(LootboxAttemptsPolicy.AD_ATTEMPTS_MAX) {
            assertTrue(LootboxAttemptsPolicy.canGrantAdAttempt(used))
            used = LootboxAttemptsPolicy.grantAdAttempt(used)
        }
        assertEquals(-LootboxAttemptsPolicy.AD_ATTEMPTS_MAX, used)
        assertFalse("Бонусы сверх потолка не копятся", LootboxAttemptsPolicy.canGrantAdAttempt(used))
        assertEquals(10, LootboxState(attemptsUsed = used).attemptsLeft)

        // Реклама работает и когда бесплатные уже кончились.
        assertTrue(LootboxAttemptsPolicy.canGrantAdAttempt(5))
        assertEquals(1, LootboxState(attemptsUsed = LootboxAttemptsPolicy.grantAdAttempt(5)).attemptsLeft)
    }

    @Test
    fun bonusAttempts_doNotShiftScriptedSequence() {
        // Индекс сценария = history.size, а не счётчик попыток: бонусы его не сдвигают.
        val state = LootboxState(attemptsUsed = -3)
        assertEquals(0, state.history.size)
        assertEquals(
            LootRewardType.MOTHER_FROM_WINDOW,
            LootboxPrizeFactory.draw(0, 42.0, rainTomorrow, themes).type
        )
        assertEquals(
            LootRewardType.SVO_CONTRACT,
            LootboxPrizeFactory.draw(1, 42.0, rainTomorrow, themes).type
        )
    }

    // ------------------------------------------------------------------
    // Погодный приз (item 14 фазы 5) — прежнее поведение
    // ------------------------------------------------------------------

    @Test
    fun rainForecast_givesTextWithRainTomorrow() {
        val reward = poolDraw(weatherRoll(LootboxPrizeFactory.WEATHER_WEIGHT_WET, rainTomorrow))
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
        val reward = poolDraw(
            roll = weatherRoll(LootboxPrizeFactory.WEATHER_WEIGHT_WET, snowTomorrow),
            tomorrow = snowTomorrow
        )
        assertEquals(LootRewardType.WEATHER_TOMORROW, reward.type)
        assertTrue(reward.title.contains("снег на завтра"))
        assertEquals("от -5° до -1°", reward.temperatureRangeRu)
    }

    @Test
    fun dryForecast_doesNotGiveRain() {
        val reward = poolDraw(
            roll = weatherRoll(LootboxPrizeFactory.WEATHER_WEIGHT_DRY, sunnyTomorrow),
            tomorrow = sunnyTomorrow
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
            val reward = poolDraw(roll, tomorrow = null)
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
        val reward = poolDraw(0.0005)
        assertEquals(LootRewardType.IPHONE, reward.type)
        assertTrue(reward.subtitle.contains("0.001"))
        assertEquals(0.001, LootboxPrizeFactory.IPHONE_WEIGHT, 0.0000001)
    }

    // ------------------------------------------------------------------
    // Состояние и история
    // ------------------------------------------------------------------

    @Test
    fun dropSequence_incrementsSeqAndCapsHistory() {
        var state = LootboxState()
        val reward = LootReward(
            id = "r1",
            title = "Тест",
            subtitle = "Проверка",
            type = LootRewardType.RUBBER_BOOTS,
            rarity = LootRarity.COMMON
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
                rarity = LootRarity.RARE,
                weatherCode = 61,
                temperatureRangeRu = "от +10° до +16°",
                dayLabelRu = "завтра",
                timestamp = 1_763_000_000_000L + index,
                gender = LootGender.MASCULINE
            )
        }
        val encoded = LootboxHistoryCodec.encode(history)
        val decoded = LootboxHistoryCodec.decode(encoded)
        assertEquals(history, decoded)
        assertEquals(61, decoded.first().weatherCode)
        assertEquals("выпал", decoded.first().revealVerbRu)
        assertFalse("revealVerbRu вычисляемое — в JSON его быть не должно", encoded.contains("revealVerbRu"))

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
                type = LootRewardType.RUBBER_BOOTS,
                rarity = LootRarity.COMMON
            )
        }
        val decoded = LootboxHistoryCodec.decode(LootboxHistoryCodec.encode(rewards))
        assertEquals(LootboxState.MAX_HISTORY, decoded.size)
        assertEquals("id-1", decoded.first().id)
    }
}
