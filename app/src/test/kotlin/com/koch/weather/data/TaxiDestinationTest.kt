package com.koch.weather.data

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.DefaultTaxiDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TaxiDestinationTest {

    private val spb = CityLocation("Санкт-Петербург", 59.9386, 30.3141, "Россия")
    private val moscow = CityLocation("Москва", 55.7558, 37.6173, "Россия")

    @Test
    fun everyDestinationResolvesAroundSelectedCity() {
        DefaultTaxiDestinations.items.forEach { destination ->
            val point = destination.coordinateNear(spb)
            assertTrue("Широта вне диапазона для ${destination.id}: ${point.latitude}", point.latitude in -90.0..90.0)
            assertTrue("Долгота вне диапазона для ${destination.id}: ${point.longitude}", point.longitude in -180.0..180.0)
            assertNotEquals(spb.latitude, point.latitude)
            assertNotEquals(spb.longitude, point.longitude)
        }
    }

    @Test
    fun airportIsApproximatelyItsDistanceFromCity() {
        val airport = DefaultTaxiDestinations.items.first { it.id == "dest_airport" }
        val point = airport.coordinateNear(spb)
        val actualKm = haversineKm(spb.latitude, spb.longitude, point.latitude, point.longitude)
        assertTrue("Ожидалось ~${airport.distanceKm} км, получено $actualKm", abs(actualKm - airport.distanceKm) < 2.0)
    }

    @Test
    fun coordinatesDependOnCity() {
        val mall = DefaultTaxiDestinations.items.first { it.id == "dest_mall" }
        val spbPoint = mall.coordinateNear(spb)
        val moscowPoint = mall.coordinateNear(moscow)
        assertNotEquals(spbPoint.latitude, moscowPoint.latitude)
        assertNotEquals(spbPoint.longitude, moscowPoint.longitude)
    }

    @Test
    fun explicitCoordinatesOverrideComputedPoint() {
        val fixed = DefaultTaxiDestinations.items.first().copy(lat = 25.2532, lon = 55.3657)
        val point = fixed.coordinateNear(spb)
        assertEquals(25.2532, point.latitude, 0.0001)
        assertEquals(55.3657, point.longitude, 0.0001)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radius * c
    }
}
