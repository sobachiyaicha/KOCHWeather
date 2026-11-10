package com.koch.weather.data

import com.koch.weather.data.model.WeatherIconType
import com.koch.weather.data.model.WmoHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WmoHelperTest {

    @Test
    fun testWmoDescriptionsRu() {
        assertEquals("Ясно", WmoHelper.getDescriptionRu(0))
        assertEquals("Преимущественно ясно", WmoHelper.getDescriptionRu(1))
        assertEquals("Переменная облачность", WmoHelper.getDescriptionRu(2))
        assertEquals("Облачно", WmoHelper.getDescriptionRu(3))
        assertEquals("Туман", WmoHelper.getDescriptionRu(45))
        assertEquals("Морось", WmoHelper.getDescriptionRu(51))
        assertEquals("Снегопад", WmoHelper.getDescriptionRu(73))
        assertEquals("Ливневый дождь", WmoHelper.getDescriptionRu(80))
        assertEquals("Гроза", WmoHelper.getDescriptionRu(95))
    }

    @Test
    fun testIconTypes() {
        assertEquals(WeatherIconType.CLEAR_DAY, WmoHelper.getIconType(0, isDay = true))
        assertEquals(WeatherIconType.CLEAR_NIGHT, WmoHelper.getIconType(0, isDay = false))
        assertEquals(WeatherIconType.PARTLY_CLOUDY_DAY, WmoHelper.getIconType(2, isDay = true))
        assertEquals(WeatherIconType.CLOUDY, WmoHelper.getIconType(3, isDay = true))
        assertEquals(WeatherIconType.RAIN, WmoHelper.getIconType(61, isDay = true))
        assertEquals(WeatherIconType.SNOW, WmoHelper.getIconType(71, isDay = true))
        assertEquals(WeatherIconType.THUNDERSTORM, WmoHelper.getIconType(95, isDay = true))
    }

    @Test
    fun testClothingAdvice() {
        val coldAdvice = WmoHelper.getClothingAdvice(-15, 71)
        assertTrue(coldAdvice.contains("мороз"))
        assertTrue(coldAdvice.contains("снег"))

        val rainAdvice = WmoHelper.getClothingAdvice(18, 61)
        assertTrue(rainAdvice.contains("Комфортно"))
        assertTrue(rainAdvice.contains("зонт"))
    }

    @Test
    fun testTrafficAdvice() {
        val clearTraffic = WmoHelper.getTrafficAdvice(0)
        assertEquals(3, clearTraffic.first)
        assertTrue(clearTraffic.second.contains("сухие"))

        val snowTraffic = WmoHelper.getTrafficAdvice(73)
        assertEquals(9, snowTraffic.first)
        assertTrue(snowTraffic.second.contains("Снегопад"))
    }
}
