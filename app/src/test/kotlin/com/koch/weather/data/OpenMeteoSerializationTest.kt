package com.koch.weather.data

import com.koch.weather.data.model.GeocodingResponse
import com.koch.weather.data.model.WeatherApiResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenMeteoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testParseForecastJson() {
        val jsonPayload = """
            {
              "latitude": 59.9386,
              "longitude": 30.3141,
              "current": {
                "time": "2026-11-10T14:00",
                "temperature_2m": 5.5,
                "relative_humidity_2m": 69,
                "apparent_temperature": 0.2,
                "is_day": 1,
                "precipitation": 0.0,
                "weather_code": 3,
                "wind_speed_10m": 22.0
              },
              "hourly": {
                "time": ["2026-11-10T00:00", "2026-11-10T01:00"],
                "temperature_2m": [3.6, 3.4],
                "precipitation_probability": [0, 5],
                "weather_code": [3, 2],
                "wind_speed_10m": [15.1, 14.8]
              },
              "daily": {
                "time": ["2026-11-10", "2026-11-11"],
                "weather_code": [3, 2],
                "temperature_2m_max": [6.2, 5.8],
                "temperature_2m_min": [2.4, 1.2],
                "precipitation_probability_max": [15, 20]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<WeatherApiResponse>(jsonPayload)
        assertEquals(59.9386, response.latitude, 0.001)
        val current = response.current
        assertNotNull(current)
        assertEquals(5.5, current?.temperature2m ?: 0.0, 0.001)
        assertEquals(3, current?.weatherCode)
        assertEquals(1, current?.isDay)
        assertEquals(2, response.hourly?.time?.size)
        assertEquals(2, response.daily?.time?.size)
    }

    @Test
    fun testParseGeocodingJson() {
        val jsonPayload = """
            {
              "results": [
                {
                  "id": 498817,
                  "name": "Санкт-Петербург",
                  "latitude": 59.93863,
                  "longitude": 30.31413,
                  "country": "Россия",
                  "admin1": "Санкт-Петербург"
                },
                {
                  "id": 524901,
                  "name": "Москва",
                  "latitude": 55.75222,
                  "longitude": 37.61556,
                  "country": "Россия"
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<GeocodingResponse>(jsonPayload)
        val results = response.results
        assertNotNull(results)
        assertEquals(2, results?.size)
        assertEquals("Санкт-Петербург", results?.get(0)?.name)
        assertEquals("Москва", results?.get(1)?.name)
    }
}
