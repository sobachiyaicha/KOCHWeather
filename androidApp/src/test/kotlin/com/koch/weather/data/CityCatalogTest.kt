package com.koch.weather.data

import com.koch.weather.data.model.CityCatalog
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.DefaultCities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Item 6 фазы 5: добавление города из поиска/списка, дедупликация по имени
 * и координатам, лимит сохранённых городов, флаг гео-города.
 */
class CityCatalogTest {

    private val kazan = CityLocation("Казань", 55.7963, 49.1088, "Россия", "Татарстан")
    private val searchSpb = CityLocation("Санкт-Петербург", 59.9386, 30.3141, "Россия")
    private val spb = DefaultCities.cities[0]
    private val moscow = DefaultCities.cities[1]

    @Test
    fun searchCity_isNeverCurrentLocation() {
        assertFalse(kazan.isCurrentLocation)
        val plan = CityCatalog.planAdd(DefaultCities.cities, kazan)
        assertNotNull(plan)
        assertTrue(plan!!.isNew)
        assertFalse(plan.cities.last().isCurrentLocation)
        assertEquals("Казань", plan.cities.last().name)
        assertEquals(plan.cities.lastIndex, plan.selectedIndex)
    }

    @Test
    fun geoCity_keepsCurrentLocationFlag() {
        assertTrue(spb.isCurrentLocation)
        assertTrue(DefaultCities.isGeoCity(spb))
        assertFalse(DefaultCities.isGeoCity(kazan))
        assertFalse(DefaultCities.isGeoCity(null))
        // Город из поиска с тем же именем остаётся не-гео-городом: у него нет флага.
        val plan = CityCatalog.planAdd(listOf(moscow), searchSpb)
        assertNotNull(plan)
        assertFalse(plan!!.cities.last().isCurrentLocation)
    }

    @Test
    fun sameNameAndCoordinates_isDuplicate() {
        assertTrue(CityCatalog.isSamePlace(spb, searchSpb))
        assertEquals(0, CityCatalog.indexOfPlace(DefaultCities.cities, searchSpb))

        val plan = CityCatalog.planAdd(DefaultCities.cities, searchSpb)
        assertNotNull(plan)
        assertFalse("Повторный выбор не должен создавать дубликат", plan!!.isNew)
        assertEquals(0, plan.selectedIndex)
        assertEquals(DefaultCities.cities.size, plan.cities.size)
    }

    @Test
    fun sameCoordinatesDifferentName_isStillSamePlace() {
        val neighbour = CityLocation("Питер", 59.9390, 30.3150, "Россия")
        assertTrue(CityCatalog.isSamePlace(spb, neighbour))
    }

    @Test
    fun sameNameInDifferentRegion_isDifferentPlace() {
        val rostovOnDon = CityLocation("Ростов", 47.2357, 39.7015, "Россия")
        val rostovGreat = CityLocation("Ростов", 57.1850, 39.4140, "Россия")
        assertFalse(CityCatalog.isSamePlace(rostovOnDon, rostovGreat))
        val plan = CityCatalog.planAdd(listOf(rostovOnDon), rostovGreat)
        assertNotNull(plan)
        assertTrue(plan!!.isNew)
        assertEquals(2, plan.cities.size)
    }

    @Test
    fun dedupe_keepsOrderAndRemovesRepeats() {
        val list = listOf(spb, moscow, searchSpb, kazan, kazan.copy(latitude = 55.7964))
        val deduped = CityCatalog.dedupe(list)
        assertEquals(listOf("Санкт-Петербург", "Москва", "Казань"), deduped.map { it.name })
        assertTrue(deduped.first().isCurrentLocation)
        assertFalse(deduped.last().isCurrentLocation)
    }

    @Test
    fun limit_isRespected() {
        val cities = (0 until CityCatalog.MAX_SAVED_CITIES).map { index ->
            CityLocation("Город $index", 50.0 + index * 0.5, 40.0 + index * 0.5, "Россия")
        }
        assertEquals(CityCatalog.MAX_SAVED_CITIES, CityCatalog.dedupe(cities).size)
        assertNull(CityCatalog.planAdd(cities, kazan))
        // Существующий город выбирается даже на лимите — добавления нет.
        val existing = CityCatalog.planAdd(cities, cities[3])
        assertNotNull(existing)
        assertFalse(existing!!.isNew)
        assertEquals(3, existing.selectedIndex)
    }
}
