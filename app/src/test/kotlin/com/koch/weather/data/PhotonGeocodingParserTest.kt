package com.koch.weather.data

import com.koch.weather.data.provider.PhotonGeocodingProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotonGeocodingParserTest {

    private val provider = PhotonGeocodingProvider()

    @Test
    fun parsesPhotonFeatureCollection() {
        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "osm_type": "R",
                    "osm_id": 337422,
                    "osm_key": "place",
                    "osm_value": "state",
                    "type": "state",
                    "name": "Saint Petersburg",
                    "country": "Russia",
                    "countrycode": "RU"
                  },
                  "geometry": { "type": "Point", "coordinates": [30.1586551, 59.9606739] }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "name": "Санкт-Петербург",
                    "city": "Санкт-Петербург",
                    "country": "Россия",
                    "state": "Санкт-Петербург"
                  },
                  "geometry": { "type": "Point", "coordinates": [30.3141, 59.9386] }
                }
              ]
            }
        """.trimIndent()

        val cities = provider.parseResponse(json)
        assertEquals(2, cities.size)
        assertEquals("Saint Petersburg", cities[0].name)
        assertEquals(59.9606739, cities[0].latitude, 0.0001)
        assertEquals(30.1586551, cities[0].longitude, 0.0001)
        assertEquals("Russia", cities[0].country)
        assertNull(cities[0].region)
        assertEquals("Санкт-Петербург", cities[1].name)
        assertEquals("Россия", cities[1].country)
        assertEquals("Санкт-Петербург", cities[1].region)
    }

    @Test
    fun skipsFeaturesWithoutCoordinatesOrName() {
        val json = """
            {"features":[
              {"properties":{"name":"No coords"},"geometry":{"coordinates":[]}},
              {"properties":{"country":"Россия"},"geometry":{"coordinates":[30.0,59.0]}},
              {"properties":{"name":"Bad lat"},"geometry":{"coordinates":[30.0,200.0]}},
              {"properties":{"name":"Good"},"geometry":{"coordinates":[30.0,59.0]}}
            ]}
        """.trimIndent()
        val cities = provider.parseResponse(json)
        assertEquals(1, cities.size)
        assertEquals("Good", cities[0].name)
    }

    @Test
    fun emptyFeatures_returnsEmptyList() {
        assertTrue(provider.parseResponse("""{"features":[]}""").isEmpty())
    }
}
