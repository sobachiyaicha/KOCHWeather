package com.koch.weather.data

import com.koch.weather.data.provider.NominatimGeocodingProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NominatimGeocodingParserTest {

    private val provider = NominatimGeocodingProvider()

    @Test
    fun parsesNominatimJsonv2Array() {
        val json = """
            [
              {
                "place_id": 168600815,
                "lat": "59.9606739",
                "lon": "30.1586551",
                "name": "Санкт-Петербург",
                "display_name": "Санкт-Петербург, Северо-Западный федеральный округ, Россия",
                "address": {
                  "country": "Россия",
                  "city": "Санкт-Петербург",
                  "state": "Санкт-Петербург"
                }
              },
              {
                "place_id": 168968639,
                "lat": "55.7558",
                "lon": "37.6173",
                "display_name": "Москва, Россия",
                "address": { "country": "Россия", "city": "Москва" }
              }
            ]
        """.trimIndent()

        val cities = provider.parseResponse(json)
        assertEquals(2, cities.size)
        assertEquals("Санкт-Петербург", cities[0].name)
        assertEquals(59.9606739, cities[0].latitude, 0.0001)
        assertEquals("Россия", cities[0].country)
        assertEquals("Санкт-Петербург", cities[0].region)
        assertEquals("Москва", cities[1].name)
        assertNull(cities[1].region)
    }

    @Test
    fun invalidCoordinates_areSkipped() {
        val json = """
            [
              {"lat":"bad","lon":"30.0","name":"Bad"},
              {"lat":"91.0","lon":"30.0","name":"Out of range"},
              {"lat":"55.0","lon":"37.0","name":"Good","address":{"country":"Россия"}}
            ]
        """.trimIndent()
        val cities = provider.parseResponse(json)
        assertEquals(1, cities.size)
        assertEquals("Good", cities[0].name)
    }

    @Test
    fun rateLimit_isAtLeastOneSecond() {
        assertEquals(1_000L, provider.rateLimitDelayMs(now = 0L, lastRequestAt = 0L))
        assertEquals(500L, provider.rateLimitDelayMs(now = 500L, lastRequestAt = 0L))
        assertEquals(0L, provider.rateLimitDelayMs(now = 1_500L, lastRequestAt = 0L))
    }

    @Test
    fun emptyArray_returnsEmptyList() {
        assertTrue(provider.parseResponse("[]").isEmpty())
    }
}
