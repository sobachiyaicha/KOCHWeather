package com.koch.weather.data.model

import kotlinx.serialization.Serializable

enum class TaxiTariffType(
    val titleRu: String,
    val basePriceRu: Int,
    val basePriceByn: Double,
    val carModel: String,
    val etaMinutes: Int,
    val iconEmoji: String
) {
    ECONOMY("Эконом", 320, 10.50, "Hyundai Solaris (Белый)", 3, "🚖"),
    COMFORT_PLUS("Комфорт+", 580, 18.90, "Geely Monjaro (Серый)", 4, "🚘"),
    ULTIMA("Ultima VIP", 1450, 48.00, "Mercedes-Benz E-Class (Черный)", 6, "🏎️")
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
    val distanceKm: Double
)

object DefaultTaxiDestinations {
    val items = listOf(
        TaxiDestination("dest_center", "Центральная площадь", "Главный проспект, пешеходная зона", 3.2),
        TaxiDestination("dest_airport", "Международный Аэропорт", "Терминал 1, вылеты", 18.5),
        TaxiDestination("dest_mall", "ТРЦ «Галерея»", "Шопинг, фудкорт, кинотеатр", 5.7),
        TaxiDestination("dest_home", "Домой под плед", "Спальный район (подальше от дождя)", 7.1)
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
