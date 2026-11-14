package com.koch.weather.data

import com.koch.weather.data.model.DefaultTaxiDestinations
import com.koch.weather.data.model.TaxiTariffType
import com.koch.weather.data.repository.TaxiRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxiRepositoryTest {

    private val repository = TaxiRepository()

    @Test
    fun nullTraffic_doesNotFabricateSurge() {
        val base = repository.calculatePriceRu(TaxiTariffType.ECONOMY, 3.0, null)
        assertEquals(TaxiTariffType.ECONOMY.basePriceRu, base)
        assertTrue(repository.calculatePriceByn(TaxiTariffType.ECONOMY, 3.0, null) > 0.0)
    }

    @Test
    fun realTraffic_increasesPrice() {
        val base = repository.calculatePriceRu(TaxiTariffType.ECONOMY, 3.0, null)
        val jam = repository.calculatePriceRu(TaxiTariffType.ECONOMY, 3.0, 9)
        assertTrue(jam > base)
    }

    @Test
    fun rideSimulation_withNullTraffic_usesBasePrice() = runTest {
        val destination = DefaultTaxiDestinations.items[0]
        val ride = repository.createRideSimulation(TaxiTariffType.ECONOMY, destination, null).first()
        assertEquals(
            repository.calculatePriceRu(TaxiTariffType.ECONOMY, destination.distanceKm, null),
            ride.priceRu
        )
    }
}
