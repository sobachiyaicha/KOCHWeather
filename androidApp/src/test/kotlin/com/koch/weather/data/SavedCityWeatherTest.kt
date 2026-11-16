package com.koch.weather.data

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.SavedCityWeather
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherIconType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedCityWeatherTest {

    private val city = CityLocation("Москва", 55.7558, 37.6173, "Россия")

    private fun domain(offline: Boolean = true) = WeatherDomainData(
        cityName = city.name,
        currentTemp = 7,
        conditionDescription = "Переменная облачность",
        minTemp = 2,
        maxTemp = 9,
        humidity = 70,
        windSpeed = 4.0,
        weatherCode = 2,
        iconType = WeatherIconType.PARTLY_CLOUDY_DAY,
        clothingAdvice = "Прохладно",
        trafficScore = null,
        trafficAdvice = "Нет данных о пробках",
        hourly = emptyList(),
        daily = emptyList(),
        isOffline = offline,
        lastUpdated = 123L
    )

    @Test
    fun fromDomain_exposesNameCoordinatesAndWeather() {
        val saved = SavedCityWeather.from(city, domain(offline = true))
        assertEquals("Москва", saved.name)
        assertEquals(55.7558, saved.latitude, 0.0001)
        assertEquals(37.6173, saved.longitude, 0.0001)
        assertEquals(7, saved.tempC)
        assertEquals("Переменная облачность", saved.conditionRu)
        assertTrue(saved.isOffline)
        assertEquals(123L, saved.updatedAt)
        assertTrue(saved.hasData)
        assertEquals("Переменная облачность", saved.conditionOrNoDataRu)
    }

    @Test
    fun withoutForecast_isHonestNoData() {
        val saved = SavedCityWeather.from(city, null)
        assertNull(saved.tempC)
        assertNull(saved.conditionRu)
        assertNull(saved.minTempC)
        assertNull(saved.maxTempC)
        assertTrue(saved.isOffline)
        assertFalse(saved.hasData)
        assertEquals("Нет данных", saved.conditionOrNoDataRu)
    }

    @Test
    fun cityAlias_keepsCoordinates() {
        val saved = SavedCityWeather(city = city, tempC = 7)
        assertEquals(city, saved.city)
        assertEquals("Россия", saved.country)
    }
}
