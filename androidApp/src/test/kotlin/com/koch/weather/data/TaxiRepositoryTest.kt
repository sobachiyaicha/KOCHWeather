package com.koch.weather.data

import com.koch.weather.data.model.TaxiPricing
import com.koch.weather.data.model.TaxiTariffType
import com.koch.weather.data.repository.TaxiRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Реалистичные цены такси (item 2 фазы 5) по официальным тарифам Яндекс Go СПб:
 * минимум 149/205/349 ₽, далее 12/17/30 ₽ за км и 12/17/25 ₽ за минуту.
 */
class TaxiRepositoryTest {

    private val repository = TaxiRepository()

    @Test
    fun oneKilometre_isMinimumFareForEveryTariff() {
        TaxiTariffType.values().forEach { tariff ->
            assertEquals(
                "1 км должен стоить ровно минимум для ${tariff.titleRu}",
                tariff.minFareRu,
                repository.calculatePriceRu(tariff, 1.0, null)
            )
        }
        // Пробки увеличивают итог (surge применяется к сумме), но без данных
        // о пробках цена — ровно минимум.
    }

    @Test
    fun tenKilometres_withoutTraffic_isComputedByFormula() {
        // duration = 3 + 10 * 2.2 = 25 мин; сверх включённых 3 мин — 22 мин,
        // сверх включённого 1 км — 9 км.
        // Эконом: 149 + 12*9 + 12*22 = 521
        assertEquals(521, repository.calculatePriceRu(TaxiTariffType.ECONOMY, 10.0, null))
        // Комфорт+: 205 + 17*9 + 17*22 = 732
        assertEquals(732, repository.calculatePriceRu(TaxiTariffType.COMFORT_PLUS, 10.0, null))
        // Ultima: 349 + 30*9 + 25*22 = 1169
        assertEquals(1169, repository.calculatePriceRu(TaxiTariffType.ULTIMA, 10.0, null))
    }

    @Test
    fun nullTraffic_doesNotFabricateSurge() {
        assertEquals(
            repository.calculatePriceRu(TaxiTariffType.ECONOMY, 7.0, null),
            repository.calculatePriceRu(TaxiTariffType.ECONOMY, 7.0, 3)
        )
        assertEquals(
            repository.calculatePriceRu(TaxiTariffType.ECONOMY, 7.0, null),
            repository.calculatePriceRu(TaxiTariffType.ECONOMY, 7.0, 0)
        )
        assertEquals(1.0, TaxiPricing.surgeMultiplier(null), 0.0001)
        assertEquals(1.0, TaxiPricing.surgeMultiplier(3), 0.0001)
    }

    @Test
    fun trafficNine_appliesSurge() {
        val base = repository.calculatePriceRu(TaxiTariffType.ECONOMY, 10.0, null)
        val jam = repository.calculatePriceRu(TaxiTariffType.ECONOMY, 10.0, 9)
        // surge = 1 + (9 - 3) * 0.12 = 1.72; 521 * 1.72 = 896.12 -> вверх 897
        assertEquals(897, jam)
        assertEquals(1.72, TaxiPricing.surgeMultiplier(9), 0.0001)
        assertTrue(jam > base)
    }

    @Test
    fun durationGrowsWithDistanceAndIsTariffIndependent() {
        assertEquals(25, TaxiPricing.durationMinRounded(10.0))
        assertEquals(3, TaxiPricing.durationMinRounded(0.0))
        val durations = TaxiPricing.durationMinByTariff(3.2)
        assertEquals(10, durations[TaxiTariffType.ECONOMY])
        assertEquals(durations[TaxiTariffType.ECONOMY], durations[TaxiTariffType.ULTIMA])
    }

    @Test
    fun priceByTariff_containsAllTariffs() {
        val prices = TaxiPricing.priceByTariff(3.2, null)
        assertEquals(TaxiTariffType.values().size, prices.size)
        assertNotNull(prices[TaxiTariffType.ECONOMY])
        assertTrue(prices.getValue(TaxiTariffType.ECONOMY) >= TaxiTariffType.ECONOMY.minFareRu)
        assertTrue(
            prices.getValue(TaxiTariffType.ULTIMA) >
                prices.getValue(TaxiTariffType.ECONOMY)
        )
    }

    @Test
    fun rideSimulation_withNullTraffic_usesRoutePrice() = runTest {
        val destination = repository.getDestinations()[0]
        val ride = repository.createRideSimulation(TaxiTariffType.ECONOMY, destination, null).first()
        assertEquals(
            repository.calculatePriceRu(TaxiTariffType.ECONOMY, destination.distanceKm, null),
            ride.priceRu
        )
        assertTrue(ride.priceRu >= TaxiTariffType.ECONOMY.minFareRu)
    }
}
