package com.koch.weather.data

import com.koch.weather.data.provider.MetNoForecastParser
import com.koch.weather.data.provider.MetNoParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class MetNoForecastParserTest {

    private val compactJson = """
        {
          "type": "Feature",
          "properties": {
            "meta": { "updated_at": "2026-11-13T22:29:32Z" },
            "timeseries": [
              {
                "time": "2026-11-13T23:00:00Z",
                "data": {
                  "instant": { "details": { "air_temperature": 2.8, "relative_humidity": 82.3, "wind_speed": 5.1, "cloud_area_fraction": 100.0, "air_pressure_at_sea_level": 985.9 } },
                  "next_1_hours": { "summary": { "symbol_code": "cloudy" }, "details": { "precipitation_amount": 0.0 } }
                }
              },
              {
                "time": "2026-11-14T00:00:00Z",
                "data": {
                  "instant": { "details": { "air_temperature": 2.6, "relative_humidity": 83.5, "wind_speed": 5.4 } },
                  "next_1_hours": { "summary": { "symbol_code": "lightrain" }, "details": { "precipitation_amount": 0.2 } }
                }
              },
              {
                "time": "2026-11-14T01:00:00Z",
                "data": {
                  "instant": { "details": { "air_temperature": 2.4, "relative_humidity": 84.7, "wind_speed": 5.5 } },
                  "next_1_hours": { "summary": { "symbol_code": "clearsky_night" }, "details": { "precipitation_amount": 0.0 } }
                }
              },
              {
                "time": "2026-11-14T12:00:00Z",
                "data": {
                  "instant": { "details": { "air_temperature": 12.0, "relative_humidity": 60.0, "wind_speed": 2.0 } },
                  "next_1_hours": { "summary": { "symbol_code": "clearsky_day" }, "details": { "precipitation_amount": 0.0 } }
                }
              },
              {
                "time": "2026-11-15T00:00:00Z",
                "data": {
                  "instant": { "details": { "air_temperature": -3.0, "relative_humidity": 90.0, "wind_speed": 6.0 } },
                  "next_1_hours": { "summary": { "symbol_code": "snow" }, "details": { "precipitation_amount": 0.4 } }
                }
              },
              {
                "time": "2026-11-15T12:00:00Z",
                "data": {
                  "instant": { "details": { "air_temperature": -1.0, "relative_humidity": 88.0, "wind_speed": 5.0 } },
                  "next_1_hours": { "summary": { "symbol_code": "snow" }, "details": { "precipitation_amount": 0.3 } }
                }
              }
            ]
          }
        }
    """.trimIndent()

    private val now = Instant.parse("2026-11-13T23:30:00Z").toEpochMilli()

    @Test
    fun parsesCurrentHourlyAndDailyFromRealCompactShape() {
        val response = MetNoForecastParser.parse(
            body = compactJson,
            nowMillis = now,
            latitude = 59.9386,
            longitude = 30.3141,
            timeZone = TimeZone.getTimeZone("Europe/Moscow")
        )
        assertEquals(59.9386, response.latitude, 0.001)
        assertEquals(30.3141, response.longitude, 0.001)

        val current = response.current
        assertNotNull(current)
        assertEquals("2026-11-14T02:00", current?.time)
        assertEquals(2.8, current?.temperature2m ?: 0.0, 0.001)
        assertEquals(3, current?.weatherCode)
        assertEquals(0, current?.isDay)
        assertEquals(82, current?.relativeHumidity2m)
        assertEquals(5.1, current?.windSpeed10m ?: 0.0, 0.001)
        assertNull(current?.apparentTemperature)

        val hourly = response.hourly
        assertNotNull(hourly)
        assertEquals(4, hourly?.time?.size)
        assertEquals("2026-11-14T02:00", hourly?.time?.first())
        assertEquals(2.8, hourly?.temperature2m?.first() ?: 0.0, 0.001)
        assertEquals(3, hourly?.weatherCode?.first())
        assertNull(hourly?.precipitationProbability)

        val daily = response.daily
        assertNotNull(daily)
        assertEquals(listOf("2026-11-14", "2026-11-15"), daily?.time)
        assertEquals(12.0, daily?.temperature2mMax?.first() ?: 0.0, 0.001)
        assertEquals(2.4, daily?.temperature2mMin?.first() ?: 0.0, 0.001)
        assertEquals(73, daily?.weatherCode?.get(1))
        assertNull(daily?.precipitationProbabilityMax)
    }

    @Test
    fun dubaiCoordinates_areAccepted() {
        val response = MetNoForecastParser.parse(
            body = compactJson,
            nowMillis = now,
            latitude = 25.2048,
            longitude = 55.2708,
            timeZone = TimeZone.getTimeZone("Asia/Dubai")
        )
        assertEquals(25.2048, response.latitude, 0.001)
        assertEquals(55.2708, response.longitude, 0.001)
        assertNotNull(response.current)
        assertNotNull(response.daily)
        assertTrue((response.daily?.time?.size ?: 0) >= 1)
    }

    @Test(expected = MetNoParseException::class)
    fun emptyTimeseries_throws() {
        MetNoForecastParser.parse(
            body = """{"properties":{"timeseries":[]}}""",
            nowMillis = now,
            latitude = 59.9,
            longitude = 30.3
        )
    }

    @Test(expected = MetNoParseException::class)
    fun invalidJson_throws() {
        MetNoForecastParser.parse(
            body = "not json",
            nowMillis = now,
            latitude = 59.9,
            longitude = 30.3
        )
    }

    @Test(expected = MetNoParseException::class)
    fun missingCurrentSymbol_throws() {
        val body = """
            {"properties":{"timeseries":[
              {"time":"2026-11-13T23:00:00Z","data":{"instant":{"details":{"air_temperature":2.0}}}}
            ]}}
        """.trimIndent()
        MetNoForecastParser.parse(body, now, 59.9, 30.3)
    }
}
