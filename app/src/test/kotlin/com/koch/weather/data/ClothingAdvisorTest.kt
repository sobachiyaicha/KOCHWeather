package com.koch.weather.data

import com.koch.weather.data.model.ClothingAdvisor
import com.koch.weather.data.model.WmoHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Item 12 фазы 5: совет по одежде — минимум 3 варианта на диапазон температур,
 * модификаторы по ветру/дождю/снегу/солнцу, детерминизм от города и дня года.
 */
class ClothingAdvisorTest {

    private val cities = listOf(
        "Санкт-Петербург",
        "Москва",
        "Сочи",
        "Казань",
        "Дубай",
        "Новосибирск"
    )

    @Test
    fun sameCityAndDay_isDeterministic() {
        val first = ClothingAdvisor.adviceFor(18, 2, 4.0, "Москва", 100)
        val second = ClothingAdvisor.adviceFor(18, 2, 4.0, "Москва", 100)
        assertEquals(first, second)
    }

    @Test
    fun differentDays_changeAdvice() {
        val variants = (1..20).map { day ->
            ClothingAdvisor.adviceFor(18, 2, null, "Москва", day)
        }.toSet()
        assertTrue("Совет должен меняться по дням, уникальных: ${variants.size}", variants.size >= 3)
    }

    @Test
    fun differentCities_canGiveDifferentAdvice() {
        val variants = cities.map { city ->
            ClothingAdvisor.adviceFor(18, 2, null, city, 100)
        }.toSet()
        assertTrue("Разные города могут давать разные советы", variants.size >= 2)
    }

    @Test
    fun everyTemperatureBand_hasAtLeastThreeVariants() {
        val temperatures = listOf(-25, -15, -5, 3, 12, 18, 25, 33)
        temperatures.forEach { temp ->
            assertTrue(
                "Диапазон $temp должен иметь 3+ варианта",
                ClothingAdvisor.variantsFor(temp).size >= 3
            )
        }
    }

    @Test
    fun strongWind_addsWindAdvice_weakWindDoesNot() {
        val windless = ClothingAdvisor.adviceFor(10, 3, 5.0, "Москва", 7)
        assertFalse(windless.lowercase().contains("ветер"))

        val windy = ClothingAdvisor.adviceFor(10, 3, 9.5, "Москва", 7)
        assertTrue("Ветер > 8 м/с должен добавить совет", windy.lowercase().contains("ветер"))
    }

    @Test
    fun rainAndSnow_addUmbrellaOrSnowAdvice() {
        val rain = ClothingAdvisor.adviceFor(12, 61, null, "Сочи", 3)
        assertTrue(rain.lowercase().contains("зонт"))

        val snow = ClothingAdvisor.adviceFor(-4, 73, null, "Новосибирск", 3)
        assertTrue(snow.lowercase().contains("снег"))

        val storm = ClothingAdvisor.adviceFor(15, 95, null, "Сочи", 4)
        assertTrue(storm.lowercase().contains("гроза"))
    }

    @Test
    fun sunnyWarmDay_addsSunglassesAdvice() {
        val sunny = ClothingAdvisor.adviceFor(24, 0, null, "Сочи", 5)
        assertTrue(sunny.lowercase().contains("солнц"))

        // В мороз «солнце» не должно добавлять совет про очки.
        val sunnyFrost = ClothingAdvisor.adviceFor(-10, 0, null, "Сочи", 5)
        assertFalse(sunnyFrost.lowercase().contains("очки"))
    }

    @Test
    fun baseBandKeywords_areStable() {
        assertTrue(ClothingAdvisor.adviceFor(-20, 3).lowercase().contains("мороз"))
        assertTrue(ClothingAdvisor.adviceFor(5, 3).lowercase().contains("холодно"))
        assertTrue(ClothingAdvisor.adviceFor(12, 3).lowercase().contains("прохладно"))
        assertTrue(ClothingAdvisor.adviceFor(18, 3).lowercase().contains("комфортно"))
        assertTrue(ClothingAdvisor.adviceFor(25, 3).lowercase().contains("тепло"))
        assertTrue(ClothingAdvisor.adviceFor(32, 3).lowercase().contains("жарко"))
    }

    @Test
    fun wmoDelegation_keepsLegacyContract() {
        val cold = WmoHelper.getClothingAdvice(-15, 71)
        assertTrue(cold.lowercase().contains("мороз"))
        assertTrue(cold.lowercase().contains("снег"))

        val rain = WmoHelper.getClothingAdvice(18, 61)
        assertTrue(rain.lowercase().contains("комфортно"))
        assertTrue(rain.lowercase().contains("зонт"))
    }

    @Test
    fun seed_isCityAndDayBased() {
        assertEquals(
            ClothingAdvisor.seedFor("Москва", 10),
            ClothingAdvisor.seedFor(" москва ", 10)
        )
        assertEquals(
            ClothingAdvisor.seedFor("Москва", 10) + 1,
            ClothingAdvisor.seedFor("Москва", 11)
        )
    }
}
