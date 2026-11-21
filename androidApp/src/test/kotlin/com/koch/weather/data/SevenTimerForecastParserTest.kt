package com.koch.weather.data

import com.koch.weather.data.provider.SevenTimerForecastParser
import com.koch.weather.data.provider.SevenTimerParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlinx.datetime.TimeZone

class SevenTimerForecastParserTest {

    private val realFragment = """
        {
          "product": "civil",
          "init": "2026111318",
          "dataseries": [
            {
              "timepoint": 3,
              "cloudcover": 9,
              "prec_type": "none",
              "prec_amount": 0,
              "temp2m": 1,
              "rh2m": "85%",
              "wind10m": { "direction": "S", "speed": 3 },
              "weather": "cloudynight"
            },
            {
              "timepoint": 6,
              "cloudcover": 9,
              "prec_type": "snow",
              "prec_amount": 1,
              "temp2m": 0,
              "rh2m": "80%",
              "wind10m": { "direction": "SW", "speed": 4 },
              "weather": "lightsnowday"
            },
            {
              "timepoint": 12,
              "cloudcover": 9,
              "prec_type": "rain",
              "prec_amount": 0,
              "temp2m": 4,
              "rh2m": "90%",
              "wind10m": { "direction": "W", "speed": 5 },
              "weather": "lightrainday"
            },
            {
              "timepoint": 24,
              "cloudcover": 2,
              "prec_type": "none",
              "prec_amount": 0,
              "temp2m": 7,
              "rh2m": "70%",
              "wind10m": { "direction": "NW", "speed": 2 },
              "weather": "clearday"
            },
            {
              "timepoint": 48,
              "cloudcover": 4,
              "prec_type": "none",
              "prec_amount": 0,
              "temp2m": 10,
              "rh2m": "60%",
              "wind10m": { "direction": "N", "speed": 1 },
              "weather": "pcloudyday"
            }
          ]
        }
    """.trimIndent()

    private val now = Instant.parse("2026-11-13T22:00:00Z").toEpochMilli()

    @Test
    fun parsesRealSevenTimerFragment() {
        val response = SevenTimerForecastParser.parse(
            body = realFragment,
            nowMillis = now,
            latitude = 59.9343,
            longitude = 30.3351,
            timeZone = TimeZone.of("UTC")
        )
        assertEquals(59.9343, response.latitude, 0.001)
        assertEquals(30.3351, response.longitude, 0.001)

        val current = response.current
        assertNotNull(current)
        assertEquals("2026-11-13T21:00", current?.time)
        assertEquals(1.0, current?.temperature2m ?: 0.0, 0.001)
        assertEquals(3, current?.weatherCode)
        assertEquals(0, current?.isDay)
        assertEquals(85, current?.relativeHumidity2m)
        assertEquals(3.6, current?.windSpeed10m ?: 0.0, 0.001)
        assertNull(current?.precipitation)
    }

    @Test
    fun buildsHourlyAndDailyAggregation() {
        val response = SevenTimerForecastParser.parse(
            body = realFragment,
            nowMillis = now,
            latitude = 59.9343,
            longitude = 30.3351,
            timeZone = TimeZone.of("UTC")
        )
        val hourly = response.hourly
        assertNotNull(hourly)
        assertEquals(24, hourly?.time?.size)
        assertEquals("2026-11-13T21:00", hourly?.time?.first())
        assertEquals(1.0, hourly?.temperature2m?.first() ?: 0.0, 0.001)
        assertEquals(3.6, hourly?.windSpeed10m?.first() ?: 0.0, 0.001)
        assertNull(hourly?.precipitationProbability)

        val daily = response.daily
        assertNotNull(daily)
        assertTrue((daily?.time?.size ?: 0) >= 3)
        assertEquals(listOf("2026-11-13", "2026-11-14", "2026-11-15"), daily?.time)
        assertEquals(1.0, daily?.temperature2mMax?.get(0) ?: 0.0, 0.001)
        assertEquals(1.0, daily?.temperature2mMin?.get(0) ?: 0.0, 0.001)
        assertEquals(7.0, daily?.temperature2mMax?.get(1) ?: 0.0, 0.001)
        assertEquals(0.0, daily?.temperature2mMin?.get(1) ?: 0.0, 0.001)
    }

    @Test(expected = SevenTimerParseException::class)
    fun emptyDataseries_throws() {
        SevenTimerForecastParser.parse(
            body = """{"product":"civil","init":"2026111318","dataseries":[]}""",
            nowMillis = now,
            latitude = 59.9,
            longitude = 30.3
        )
    }

    @Test(expected = SevenTimerParseException::class)
    fun invalidJson_throws() {
        SevenTimerForecastParser.parse(
            body = "not json",
            nowMillis = now,
            latitude = 59.9,
            longitude = 30.3
        )
    }
}
