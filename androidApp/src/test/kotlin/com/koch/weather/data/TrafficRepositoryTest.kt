package com.koch.weather.data

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.repository.TrafficRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Пробки: город вне покрытия Яндекса обязан получать «нет данных» (item 11 фазы 5).
 * Тест закрепляет удаление записей и координатного fallback для города, которого
 * больше нет в маппинге: выдуманный балл 157 не должен вернуться.
 */
class TrafficRepositoryTest {

    private val repository = TrafficRepository(apiKey = "test")

    @Test
    fun knownRussianCity_resolvesItsRegionId() {
        val moscow = CityLocation(name = "Москва", latitude = 55.7558, longitude = 37.6173)
        assertEquals(213, repository.resolveRegionId(moscow))
    }

    @Test
    fun cityOutsideCoverage_hasNoRegionId() {
        // Те же координаты, что давал удалённый fallback: региона быть не должно.
        val outside = CityLocation(
            name = "Город без покрытия",
            latitude = 53.9,
            longitude = 27.5
        )
        assertNull(repository.resolveRegionId(outside))
    }

    @Test
    fun cityOutsideCoverage_getsHonestNoDataTraffic() = runBlocking {
        val outside = CityLocation(
            name = "Город без покрытия",
            latitude = 53.9,
            longitude = 27.5
        )
        val info = repository.getTraffic(outside)
        // Не ходим в сеть и не придумываем балл: честное «нет данных».
        assertNull(info.score)
        assertEquals("Нет данных о пробках", info.description)
        assertEquals("None", info.source)
    }
}
