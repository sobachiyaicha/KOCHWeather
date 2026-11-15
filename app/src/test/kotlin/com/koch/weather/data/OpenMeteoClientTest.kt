package com.koch.weather.data

import com.koch.weather.data.api.OpenMeteoClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoClientTest {

    @Test
    fun forecastUrl_requestsExplicitMetricUnits() {
        val url = OpenMeteoClient().buildForecastUrl(59.9343, 30.3351)
        assertTrue(url.contains("wind_speed_unit=ms"))
        assertTrue(url.contains("temperature_unit=celsius"))
        assertTrue(url.contains("precipitation_unit=mm"))
        assertFalse(url.contains("wind_speed_unit=kmh"))
        assertFalse(url.contains("wind_speed_unit=km/h"))
    }
}
