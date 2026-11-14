package com.koch.weather.data.model

import com.koch.weather.data.deeplink.GeoPoint
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin

enum class TaxiTariffType(
    val titleRu: String,
    val basePriceRu: Int,
    val basePriceByn: Double,
    val carModel: String,
    val etaMinutes: Int
) {
    ECONOMY("Эконом", 320, 10.50, "Hyundai Solaris (Белый)", 3),
    COMFORT_PLUS("Комфорт+", 580, 18.90, "Geely Monjaro (Серый)", 4),
    ULTIMA("Ultima VIP", 1450, 48.00, "Mercedes-Benz E-Class (Черный)", 6)
}

enum class TaxiRideStatus(val titleRu: String) {
    IDLE("Готов к заказу"),
    SEARCHING("Поиск ближайшего автомобиля..."),
    ASSIGNED("Водитель назначен!"),
    ARRIVING("Автомобиль подъезжает к вам"),
    IN_PROGRESS("Поездка в процессе"),
    COMPLETED("Поездка успешно завершена")
}

@Serializable
data class TaxiDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val distanceKm: Double,
    /**
     * Абсолютные координаты, если точка известна точно. null означает
     * «вычислить вокруг выбранного города» по [bearingDeg] и [distanceKm].
     */
    val lat: Double? = null,
    val lon: Double? = null,
    val bearingDeg: Double = 0.0
) {
    /**
     * Точка назначения для конкретного города. Для общих демо-точек
     * смещаемся от центра города на distanceKm в направлении bearingDeg.
     */
    fun coordinateNear(city: CityLocation): GeoPoint {
        val fixedLat = lat
        val fixedLon = lon
        if (fixedLat != null && fixedLon != null &&
            fixedLat in -90.0..90.0 && fixedLon in -180.0..180.0
        ) {
            return GeoPoint(fixedLat, fixedLon)
        }
        val bearing = Math.toRadians(bearingDeg)
        val latDelta = (distanceKm / 111.32) * cos(bearing)
        val cosLat = cos(Math.toRadians(city.latitude)).coerceAtLeast(0.0001)
        val lonDelta = (distanceKm / (111.32 * cosLat)) * sin(bearing)
        return GeoPoint(
            latitude = (city.latitude + latDelta).coerceIn(-90.0, 90.0),
            longitude = normalizeLongitude(city.longitude + lonDelta)
        )
    }

    private fun normalizeLongitude(value: Double): Double =
        ((value + 540.0) % 360.0) - 180.0
}

object DefaultTaxiDestinations {
    val items = listOf(
        TaxiDestination(
            id = "dest_center",
            title = "Центральная площадь",
            subtitle = "Главный проспект, пешеходная зона",
            distanceKm = 3.2,
            bearingDeg = 15.0
        ),
        TaxiDestination(
            id = "dest_airport",
            title = "Международный Аэропорт",
            subtitle = "Терминал 1, вылеты",
            distanceKm = 18.5,
            bearingDeg = 160.0
        ),
        TaxiDestination(
            id = "dest_mall",
            title = "ТРЦ «Галерея»",
            subtitle = "Шопинг, фудкорт, кинотеатр",
            distanceKm = 5.7,
            bearingDeg = 95.0
        ),
        TaxiDestination(
            id = "dest_home",
            title = "Домой под плед",
            subtitle = "Спальный район (подальше от дождя)",
            distanceKm = 7.1,
            bearingDeg = 250.0
        )
    )
}

@Serializable
data class TaxiRide(
    val orderId: String,
    val tariff: TaxiTariffType,
    val destination: TaxiDestination,
    val priceRu: Int,
    val priceByn: Double,
    val driverName: String,
    val carPlate: String,
    val carModel: String,
    val status: TaxiRideStatus,
    val etaMinutes: Int
)

data class TaxiState(
    val isSheetOpen: Boolean = false,
    val selectedTariff: TaxiTariffType = TaxiTariffType.ECONOMY,
    val selectedDestination: TaxiDestination = DefaultTaxiDestinations.items[0],
    val activeRide: TaxiRide? = null,
    val isOrdering: Boolean = false
)
