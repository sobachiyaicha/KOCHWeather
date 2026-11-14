package com.koch.weather.data

import com.koch.weather.data.deeplink.DeepLinkTarget
import com.koch.weather.data.deeplink.DeepLinks
import com.koch.weather.data.deeplink.GeoPoint
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.DefaultTaxiDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class DeepLinksTest {

    private val spb = GeoPoint(59.9386, 30.3141)
    private val moscow = GeoPoint(55.7558, 37.6173)

    @Test
    fun yandexGoRoute_hasAppAndWebUris() {
        val plan = DeepLinks.yandexGoRoute(spb, moscow)
        val app = plan.app
        assertNotNull(app)
        assertEquals(
            "yandextaxi://route?start-lat=59.9386&start-lon=30.3141" +
                "&end-lat=55.7558&end-lon=37.6173&tariffClass=econom&ref=kochweather",
            app?.uri
        )
        assertEquals("ru.yandex.taxi", app?.packageName)
        assertEquals(
            "https://taxi.yandex.ru/route?start-lat=59.9386&start-lon=30.3141" +
                "&end-lat=55.7558&end-lon=37.6173&tariffClass=econom&ref=kochweather",
            plan.web.uri
        )

        val appUri = URI(app?.uri)
        assertEquals("yandextaxi", appUri.scheme)
        val webUri = URI(plan.web.uri)
        assertEquals("https", webUri.scheme)
        assertEquals("taxi.yandex.ru", webUri.host)
        assertTrue(webUri.rawQuery.contains("start-lat=59.9386"))
        assertTrue(webUri.rawQuery.contains("end-lon=37.6173"))
    }

    @Test
    fun taxiRouteForDestination_containsBothStartAndEndCoordinates() {
        val city = CityLocation("Санкт-Петербург", 59.9386, 30.3141, "Россия")
        val destination = DefaultTaxiDestinations.items.first { it.id == "dest_airport" }
        val plan = DeepLinks.yandexGoRouteFor(city, destination)
        val point = destination.coordinateNear(city)

        val appUri = URI(plan.app?.uri)
        val query = appUri.rawQuery
        assertTrue(query.contains("start-lat=59.9386"))
        assertTrue(query.contains("start-lon=30.3141"))
        assertTrue(query.contains("end-lat=${DeepLinks.format(point.latitude)}"))
        assertTrue(query.contains("end-lon=${DeepLinks.format(point.longitude)}"))
        assertTrue(plan.web.uri.contains("end-lat=${DeepLinks.format(point.latitude)}"))
        assertTrue(plan.web.uri.contains("end-lon=${DeepLinks.format(point.longitude)}"))
    }

    @Test
    fun yandexMapsRoute_hasVerifiedDeepLinkFormat() {
        val plan = DeepLinks.yandexMapsRoute(spb, moscow)
        assertEquals(
            "yandexmaps://build_route_on_map/?lat_from=59.9386&lon_from=30.3141" +
                "&lat_to=55.7558&lon_to=37.6173",
            plan.app?.uri
        )
        assertEquals("ru.yandex.yandexmaps", plan.app?.packageName)
        assertEquals(
            "https://yandex.ru/maps/?rtext=59.9386,30.3141~55.7558,37.6173&rtt=auto",
            plan.web.uri
        )
    }

    @Test
    fun yandexEda_hasSchemeAndWebFallback() {
        val plan = DeepLinks.yandexEda()
        assertEquals("eda.yandex://open", plan.app?.uri)
        assertEquals("ru.foodfox.client", plan.app?.packageName)
        assertEquals("eda.yandex", URI(plan.app?.uri).scheme)
        assertEquals("open", URI(plan.app?.uri).host)
        assertEquals("https://eda.yandex.ru/", plan.web.uri)
    }

    @Test
    fun plan_candidatesPreferAppThenWeb() {
        val plan = DeepLinks.yandexGoRoute(spb, moscow)
        assertEquals(2, plan.candidates().size)
        assertTrue(plan.candidates()[0] is DeepLinkTarget.App)
        assertEquals(plan.app, plan.primary)
    }

    @Test
    fun coordinatesAreFormattedWithoutLocaleCommaOrExponent() {
        val plan = DeepLinks.yandexGoRoute(
            from = GeoPoint(55.0, 37.0),
            to = GeoPoint(56.0, 38.0)
        )
        assertTrue(plan.app?.uri?.contains("start-lat=55&start-lon=37") == true)
        assertTrue(plan.app?.uri?.contains("end-lat=56&end-lon=38") == true)
        assertTrue(plan.app?.uri?.contains(",") == false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidLatitude_isRejected() {
        DeepLinks.yandexGoRoute(GeoPoint(91.0, 30.0), moscow)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidLongitude_isRejected() {
        DeepLinks.yandexMapsRoute(spb, GeoPoint(55.0, 181.0))
    }

    @Test
    fun tariffClassIsEncoded() {
        val plan = DeepLinks.yandexGoRoute(spb, moscow, tariffClass = "comfort+")
        assertTrue(plan.app?.uri?.contains("tariffClass=comfort%2B") == true)
    }
}
