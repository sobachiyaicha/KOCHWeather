package com.koch.weather.data.model

import androidx.compose.runtime.Immutable
import com.koch.weather.data.deeplink.GeoPoint
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Тариф такси по официальным ценам Яндекс Go (Санкт-Петербург, проверено 14.11.2026,
 * taxi.yandex.ru/spb/tariff):
 *  - Эконом: минимум 149 ₽ за 3 мин и 1 км, далее 12 ₽/км и 12 ₽/мин;
 *  - Комфорт+: минимум 205 ₽, далее 17 ₽/км и 17 ₽/мин;
 *  - Ultima (Business): 349 ₽, далее 30 ₽/км и 25 ₽/мин — ОЦЕНКА по Business,
 *    отдельного публичного тарифа Ultima в СПб на дату проверки не было.
 *
 * Никаких фиксированных «320 ₽ за поездку»: цена считается под конкретный маршрут
 * в [TaxiPricing].
 */
enum class TaxiTariffType(
    val titleRu: String,
    val minFareRu: Int,
    val perKmRu: Int,
    val perMinRu: Int,
    val carModel: String,
    val etaMinutes: Int
) {
    ECONOMY("Эконом", 149, 12, 12, "Hyundai Solaris (Белый)", 3),
    COMFORT_PLUS("Комфорт+", 205, 17, 17, "Geely Monjaro (Серый)", 4),
    ULTIMA("Ultima (Business)", 349, 30, 25, "Mercedes-Benz E-Class (Черный)", 6)
}

/**
 * Чистая математика тарифа: одна формула на репозиторий и на ViewModel.
 *
 * Длительность: `3 + расстояние * 2.2` минуты (3 мин — подача, 2.2 мин/км в городе).
 * Стоимость: `max(minFare, minFare + perKm * км_сверх_1 + perMin * мин_сверх_3) * surge`.
 * Первый километр и первые 3 минуты уже включены в минимальный тариф, поэтому
 * поездка ровно на 1 км стоит ровно минимум (тест `oneKilometre_isMinimumFare`).
 * При отсутствии данных о пробках (trafficScore == null) наценки нет.
 */
object TaxiPricing {

    /** Подача и первые 3 минуты включены в минимальный тариф. */
    const val BOARDING_INCLUDED_MINUTES: Double = 3.0

    /** Первый километр включён в минимальный тариф. */
    const val INCLUDED_KM: Double = 1.0

    /** Минут на километр городского трафика. */
    const val MINUTES_PER_KM: Double = 2.2

    /** Сколько минут займёт поездка (дробное значение до округления). */
    fun durationMin(distanceKm: Double): Double =
        BOARDING_INCLUDED_MINUTES + distanceKm.coerceAtLeast(0.0) * MINUTES_PER_KM

    /** Минуты для подписи «в пути»: ближайшая целая минута, но не меньше одной. */
    fun durationMinRounded(distanceKm: Double): Int =
        durationMin(distanceKm).roundToInt().coerceAtLeast(1)

    /**
     * Наценка за пробки: 1.0 без данных и при свободной дороге,
     * иначе +12 % за каждый балл сверх 3 (9 баллов → 1.72).
     */
    fun surgeMultiplier(trafficScore: Int?): Double {
        val score = trafficScore ?: return 1.0
        return 1.0 + (score.coerceIn(0, 10) - 3).coerceAtLeast(0) * 0.12
    }

    fun calculatePriceRu(
        tariff: TaxiTariffType,
        distanceKm: Double,
        trafficScore: Int?
    ): Int {
        val km = distanceKm.coerceAtLeast(0.0)
        val billableKm = (km - INCLUDED_KM).coerceAtLeast(0.0)
        val billableMin = if (billableKm > 0.0) {
            (durationMin(km) - BOARDING_INCLUDED_MINUTES).coerceAtLeast(0.0)
        } else {
            // Внутри включённого километра работает только минимальный тариф.
            0.0
        }
        val base = tariff.minFareRu.toDouble() +
            tariff.perKmRu * billableKm +
            tariff.perMinRu * billableMin
        val withFloor = base.coerceAtLeast(tariff.minFareRu.toDouble())
        return ceil(withFloor * surgeMultiplier(trafficScore)).toInt()
    }

    fun priceByTariff(
        distanceKm: Double,
        trafficScore: Int?
    ): Map<TaxiTariffType, Int> = TaxiTariffType.values().associateWith { tariff ->
        calculatePriceRu(tariff, distanceKm, trafficScore)
    }

    fun durationMinByTariff(distanceKm: Double): Map<TaxiTariffType, Int> {
        val minutes = durationMinRounded(distanceKm)
        return TaxiTariffType.values().associateWith { minutes }
    }
}

enum class TaxiRideStatus(val titleRu: String) {
    IDLE("Готов к заказу"),
    SEARCHING("Поиск ближайшего автомобиля..."),
    ASSIGNED("Водитель назначен!"),
    ARRIVING("Автомобиль подъезжает к вам"),
    IN_PROGRESS("Поездка в процессе"),
    COMPLETED("Поездка успешно завершена")
}

@Immutable
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
        val bearing = bearingDeg * PI / 180.0
        val latDelta = (distanceKm / 111.32) * cos(bearing)
        val cosLat = cos(city.latitude * PI / 180.0).coerceAtLeast(0.0001)
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

@Immutable
@Serializable
data class TaxiRide(
    val orderId: String,
    val tariff: TaxiTariffType,
    val destination: TaxiDestination,
    val priceRu: Int,
    val driverName: String,
    val carPlate: String,
    val carModel: String,
    val status: TaxiRideStatus,
    val etaMinutes: Int
)

@Immutable
data class TaxiState(
    val isSheetOpen: Boolean = false,
    val selectedTariff: TaxiTariffType = TaxiTariffType.ECONOMY,
    val selectedDestination: TaxiDestination = DefaultTaxiDestinations.items[0],
    val activeRide: TaxiRide? = null,
    val isOrdering: Boolean = false,
    /**
     * Цена и время для выбранного маршрута. Никаких фиксированных цен на карточках:
     * ViewModel пересчитывает при смене точки, тарифа и при обновлении погоды.
     */
    val priceByTariff: Map<TaxiTariffType, Int> = TaxiPricing.priceByTariff(
        DefaultTaxiDestinations.items[0].distanceKm,
        null
    ),
    val durationMinByTariff: Map<TaxiTariffType, Int> = TaxiPricing.durationMinByTariff(
        DefaultTaxiDestinations.items[0].distanceKm
    )
) {
    /** Цена выбранного тарифа для выбранного маршрута; null — расчёт ещё не готов. */
    val selectedPriceRu: Int? get() = priceByTariff[selectedTariff]

    /** Время в пути для выбранного тарифа, мин; null — расчёт ещё не готов. */
    val selectedDurationMin: Int? get() = durationMinByTariff[selectedTariff]
}
