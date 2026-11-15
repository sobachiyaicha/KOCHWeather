package com.koch.weather.data

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.DefaultCities
import com.koch.weather.data.model.FactOfDayPolicy
import com.koch.weather.data.repository.AiFactsRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * D5 с устройства: «А знали ли вы…» не должен меняться между перезапусками
 * в один день. Сид факта — постоянный гео-город + день года; восстановленный
 * «последний открытый город» на него не влияет.
 */
class FactOfDayPolicyTest {

    private val repository = AiFactsRepository()

    @Test
    fun twoStartsWithSameCitiesAndDay_giveSameFact() {
        val day = 318
        val firstStart = repository.getFactForDay(
            FactOfDayPolicy.anchorCityName(DefaultCities.cities),
            day
        )
        val secondStart = AiFactsRepository().getFactForDay(
            FactOfDayPolicy.anchorCityName(DefaultCities.cities),
            day
        )

        assertEquals(firstStart, secondStart)
    }

    @Test
    fun restoredLastOpenedCity_doesNotChangeFactOfDay() {
        val day = 318
        val straight = repository.getFactForDay(
            FactOfDayPolicy.anchorCityName(DefaultCities.cities),
            day
        )
        // Второй старт: DataStore восстановил список, последним открывали Казань.
        val restored = listOf(
            DefaultCities.cities[3],
            DefaultCities.cities[2],
            DefaultCities.cities[0],
            DefaultCities.cities[1]
        )
        val afterRestart = repository.getFactForDay(
            FactOfDayPolicy.anchorCityName(restored),
            day
        )

        assertEquals(straight, afterRestart)
    }

    @Test
    fun anchorIgnoresListOrderAndSelectedCity() {
        assertEquals(
            DefaultCities.GEO_CITY_NAME,
            FactOfDayPolicy.anchorCityName(DefaultCities.cities.reversed())
        )
        assertEquals(
            DefaultCities.GEO_CITY_NAME,
            FactOfDayPolicy.anchorCityName(DefaultCities.cities, fallback = "Москва")
        )
    }

    @Test
    fun anchorUsesFirstCityWhenGeoFlagIsMissing() {
        val kazan = CityLocation("Казань", 55.7963, 49.1088, "Россия", "Татарстан")

        assertEquals("Казань", FactOfDayPolicy.anchorCityName(listOf(kazan)))
    }

    @Test
    fun anchorFallsBackToGeoNameForEmptyList() {
        assertEquals(DefaultCities.GEO_CITY_NAME, FactOfDayPolicy.anchorCityName(emptyList()))
        assertEquals("Москва", FactOfDayPolicy.anchorCityName(emptyList(), fallback = "Москва"))
    }
}
