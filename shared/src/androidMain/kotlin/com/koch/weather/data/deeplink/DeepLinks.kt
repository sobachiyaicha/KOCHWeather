package com.koch.weather.data.deeplink

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.TaxiDestination
import java.math.BigDecimal
import java.net.URLEncoder

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

sealed interface DeepLinkTarget {
    val uri: String

    data class App(
        override val uri: String,
        val packageName: String
    ) : DeepLinkTarget

    data class Web(
        override val uri: String
    ) : DeepLinkTarget
}

/**
 * План перехода: сначала приложение (если установлено), иначе веб-фолбэк.
 * UI должен пройти по [candidates], проверить resolveActivity и запустить Intent.
 */
data class DeepLinkPlan(
    val app: DeepLinkTarget.App?,
    val web: DeepLinkTarget.Web
) {
    val primary: DeepLinkTarget get() = app ?: web

    fun candidates(): List<DeepLinkTarget> = listOfNotNull(app, web)
}

/**
 * Чистые функции deep links без Android-зависимостей — покрываются JVM-тестами.
 *
 * Яндекс Go:
 *  - схема приложения: yandextaxi://route?start-lat=..&start-lon=..&end-lat=..&end-lon=..
 *    (официальный формат из документации Яндекс Go / AppMetrica deep link);
 *  - веб-фолбэк: https://taxi.yandex.ru/route?... (проверен, HTTP 200).
 *
 * Яндекс Карты:
 *  - yandexmaps://build_route_on_map/?lat_from=..&lon_from=..&lat_to=..&lon_to=..
 *  - https://yandex.ru/maps/?rtext=lat,lon~lat,lon&rtt=auto
 *
 * Яндекс Еда:
 *  - схема eda.yandex:// (приложение ru.foodfox.client);
 *  - https://eda.yandex.ru/.
 */
object DeepLinks {

    const val YANDEX_GO_PACKAGE = "ru.yandex.taxi"
    const val YANDEX_MAPS_PACKAGE = "ru.yandex.yandexmaps"
    const val YANDEX_EDA_PACKAGE = "ru.foodfox.client"

    const val REF = "kochweather"

    fun yandexGoRoute(
        from: GeoPoint,
        to: GeoPoint,
        tariffClass: String = "econom"
    ): DeepLinkPlan {
        validate(from)
        validate(to)
        val coordinates = routeCoordinates(from, to)
        val tariff = encode(tariffClass)
        return DeepLinkPlan(
            app = DeepLinkTarget.App(
                uri = "yandextaxi://route?$coordinates&tariffClass=$tariff&ref=$REF",
                packageName = YANDEX_GO_PACKAGE
            ),
            web = DeepLinkTarget.Web(
                uri = "https://taxi.yandex.ru/route?$coordinates&tariffClass=$tariff&ref=$REF"
            )
        )
    }

    /**
     * Полный маршрут такси: точка подачи = центр выбранного города,
     * точка назначения = [TaxiDestination.coordinateNear] вокруг этого города.
     */
    fun yandexGoRouteFor(
        city: CityLocation,
        destination: TaxiDestination,
        tariffClass: String = "econom"
    ): DeepLinkPlan = yandexGoRoute(
        from = GeoPoint(city.latitude, city.longitude),
        to = destination.coordinateNear(city),
        tariffClass = tariffClass
    )

    fun yandexMapsRoute(from: GeoPoint, to: GeoPoint): DeepLinkPlan {
        validate(from)
        validate(to)
        return DeepLinkPlan(
            app = DeepLinkTarget.App(
                uri = "yandexmaps://build_route_on_map/?lat_from=${format(from.latitude)}" +
                    "&lon_from=${format(from.longitude)}" +
                    "&lat_to=${format(to.latitude)}" +
                    "&lon_to=${format(to.longitude)}",
                packageName = YANDEX_MAPS_PACKAGE
            ),
            web = DeepLinkTarget.Web(
                uri = "https://yandex.ru/maps/?rtext=${format(from.latitude)},${format(from.longitude)}" +
                    "~${format(to.latitude)},${format(to.longitude)}&rtt=auto"
            )
        )
    }

    fun yandexEda(): DeepLinkPlan = DeepLinkPlan(
        app = DeepLinkTarget.App(
            // Пустой authority часть Android-парсеров отвергает; host "open"
            // безопасен для схемы eda.yandex и проверяется через resolveActivity.
            uri = "eda.yandex://open",
            packageName = YANDEX_EDA_PACKAGE
        ),
        web = DeepLinkTarget.Web(uri = "https://eda.yandex.ru/")
    )

    private fun routeCoordinates(from: GeoPoint, to: GeoPoint): String =
        "start-lat=${format(from.latitude)}" +
            "&start-lon=${format(from.longitude)}" +
            "&end-lat=${format(to.latitude)}" +
            "&end-lon=${format(to.longitude)}"

    private fun validate(point: GeoPoint) {
        require(point.latitude.isFinite() && point.longitude.isFinite()) {
            "Координаты должны быть конечными числами"
        }
        require(point.latitude in -90.0..90.0) {
            "Широта вне диапазона -90..90: ${point.latitude}"
        }
        require(point.longitude in -180.0..180.0) {
            "Долгота вне диапазона -180..180: ${point.longitude}"
        }
    }

    /** Стабильное представление координат без Locale-запятой и без экспоненты. */
    fun format(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
