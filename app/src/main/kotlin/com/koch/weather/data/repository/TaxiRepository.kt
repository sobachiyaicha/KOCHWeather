package com.koch.weather.data.repository

import com.koch.weather.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

class TaxiRepository {

    fun getDestinations(): List<TaxiDestination> = DefaultTaxiDestinations.items

    /**
     * trafficScore == null означает «нет данных о пробках»: надбавку не
     * придумываем, считаем по базовому тарифу и расстоянию.
     */
    fun calculatePriceRu(tariff: TaxiTariffType, distanceKm: Double, trafficScore: Int?): Int {
        val surgeMultiplier = trafficScore?.let {
            1.0 + (it.coerceIn(0, 10) - 3).coerceAtLeast(0) * 0.12
        } ?: 1.0
        val distanceMultiplier = 1.0 + (distanceKm - 3.0).coerceAtLeast(0.0) * 0.08
        return (tariff.basePriceRu * surgeMultiplier * distanceMultiplier).roundToInt()
    }

    fun calculatePriceByn(tariff: TaxiTariffType, distanceKm: Double, trafficScore: Int?): Double {
        val priceRu = calculatePriceRu(tariff, distanceKm, trafficScore)
        return kotlin.math.round((priceRu / 31.0) * 100.0) / 100.0
    }

    suspend fun createRideSimulation(
        tariff: TaxiTariffType,
        destination: TaxiDestination,
        trafficScore: Int?
    ): Flow<TaxiRide> = flow {
        val orderId = "TAX-" + UUID.randomUUID().toString().take(6).uppercase()
        val priceRu = calculatePriceRu(tariff, destination.distanceKm, trafficScore)
        val priceByn = calculatePriceByn(tariff, destination.distanceKm, trafficScore)

        val (driverName, carPlate, carModel) = when (tariff) {
            TaxiTariffType.ECONOMY -> Triple("Алексей В.", "о142вх 178", "Hyundai Solaris (Белый)")
            TaxiTariffType.COMFORT_PLUS -> Triple("Михаил К.", "м789ер 777", "Geely Monjaro (Серый металлик)")
            TaxiTariffType.ULTIMA -> Triple("Владимир С.", "а001аа 199", "Mercedes-Benz E-Class (Черный)")
        }

        // 1. Searching driver
        var currentRide = TaxiRide(
            orderId = orderId,
            tariff = tariff,
            destination = destination,
            priceRu = priceRu,
            priceByn = priceByn,
            driverName = "Подбор...",
            carPlate = "...",
            carModel = tariff.carModel,
            status = TaxiRideStatus.SEARCHING,
            etaMinutes = tariff.etaMinutes
        )
        emit(currentRide)
        delay(1500)

        // 2. Driver assigned
        currentRide = currentRide.copy(
            driverName = driverName,
            carPlate = carPlate,
            carModel = carModel,
            status = TaxiRideStatus.ASSIGNED,
            etaMinutes = tariff.etaMinutes
        )
        emit(currentRide)
        delay(2000)

        // 3. Arriving
        currentRide = currentRide.copy(
            status = TaxiRideStatus.ARRIVING,
            etaMinutes = 1
        )
        emit(currentRide)
        delay(2500)

        // 4. In progress
        currentRide = currentRide.copy(
            status = TaxiRideStatus.IN_PROGRESS,
            etaMinutes = (destination.distanceKm * 2.2).roundToInt()
        )
        emit(currentRide)
        delay(3500)

        // 5. Completed
        currentRide = currentRide.copy(
            status = TaxiRideStatus.COMPLETED,
            etaMinutes = 0
        )
        emit(currentRide)
    }
}
