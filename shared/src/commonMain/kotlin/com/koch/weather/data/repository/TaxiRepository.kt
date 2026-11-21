package com.koch.weather.data.repository

import com.koch.weather.data.model.DefaultTaxiDestinations
import com.koch.weather.data.model.TaxiDestination
import com.koch.weather.data.model.TaxiPricing
import com.koch.weather.data.model.TaxiRide
import com.koch.weather.data.model.TaxiRideStatus
import com.koch.weather.data.model.TaxiTariffType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TaxiRepository {

    fun getDestinations(): List<TaxiDestination> = DefaultTaxiDestinations.items

    /**
     * Цена поездки для конкретного маршрута (тариф + расстояние + реальные пробки).
     * Формула живёт в [TaxiPricing], чтобы ViewModel и карточки тарифов считали
     * ровно то же число. trafficScore == null означает «нет данных о пробках»:
     * надбавку не придумываем.
     */
    fun calculatePriceRu(tariff: TaxiTariffType, distanceKm: Double, trafficScore: Int?): Int =
        TaxiPricing.calculatePriceRu(tariff, distanceKm, trafficScore)

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createRideSimulation(
        tariff: TaxiTariffType,
        destination: TaxiDestination,
        trafficScore: Int?
    ): Flow<TaxiRide> = flow {
        val orderId = "TAX-" + Uuid.random().toString().take(6).uppercase()
        val priceRu = calculatePriceRu(tariff, destination.distanceKm, trafficScore)
        val tripMinutes = TaxiPricing.durationMinRounded(destination.distanceKm)

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

        // 4. In progress: реальное расчётное время маршрута, а не фиктивное
        currentRide = currentRide.copy(
            status = TaxiRideStatus.IN_PROGRESS,
            etaMinutes = tripMinutes
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
