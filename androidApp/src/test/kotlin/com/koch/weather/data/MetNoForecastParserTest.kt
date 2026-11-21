package com.koch.weather.data

import com.koch.weather.data.mapper.WeatherMapResult
import com.koch.weather.data.mapper.WeatherMapper
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.provider.MetNoForecastParser
import com.koch.weather.data.provider.MetNoParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlinx.datetime.TimeZone

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

    /**
     * Реальное поведение compact-ответа (сверено с `shots/fq3_metno_live.json`):
     * ближние часы приходят в `next_1_hours`, причём на тех же точках есть
     * скользящее `next_6_hours`; с 3-х суток сетка становится 6-часовой и
     * количество есть только в `next_6_hours`, а `next_12_hours` идёт с пустым
     * `details`. Сумма за 16.11 = 0.4 + 0.0 + 1.5 + 0.6, за 17.11 = 1.0 + 0.5.
     */
    private val mixedHorizonJson = """
        {
          "type": "Feature",
          "properties": {
            "meta": { "updated_at": "2026-11-14T19:12:00Z" },
            "timeseries": [
              {"time":"2026-11-14T20:00:00Z","data":{"instant":{"details":{"air_temperature":3.0,"relative_humidity":85.0,"wind_speed":4.0}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.3}},"next_6_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":1.2}},"next_12_hours":{"summary":{"symbol_code":"cloudy"},"details":{}}}},
              {"time":"2026-11-14T21:00:00Z","data":{"instant":{"details":{"air_temperature":2.8}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}},"next_6_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":1.0}},"next_12_hours":{"summary":{"symbol_code":"cloudy"},"details":{}}}},
              {"time":"2026-11-14T22:00:00Z","data":{"instant":{"details":{"air_temperature":2.6}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}},"next_6_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.8}},"next_12_hours":{"summary":{"symbol_code":"cloudy"},"details":{}}}},
              {"time":"2026-11-14T23:00:00Z","data":{"instant":{"details":{"air_temperature":2.5}},"next_1_hours":{"summary":{"symbol_code":"lightrain"},"details":{"precipitation_amount":0.1}}}},
              {"time":"2026-11-15T00:00:00Z","data":{"instant":{"details":{"air_temperature":2.4}},"next_1_hours":{"summary":{"symbol_code":"lightrain"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T01:00:00Z","data":{"instant":{"details":{"air_temperature":2.2}},"next_1_hours":{"summary":{"symbol_code":"lightrain"},"details":{"precipitation_amount":0.2}},"next_6_hours":{"summary":{"symbol_code":"lightrain"},"details":{"precipitation_amount":0.9}},"next_12_hours":{"summary":{"symbol_code":"lightrain"},"details":{}}}},
              {"time":"2026-11-15T02:00:00Z","data":{"instant":{"details":{"air_temperature":2.1}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T03:00:00Z","data":{"instant":{"details":{"air_temperature":2.0}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T04:00:00Z","data":{"instant":{"details":{"air_temperature":1.9}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T05:00:00Z","data":{"instant":{"details":{"air_temperature":1.8}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T06:00:00Z","data":{"instant":{"details":{"air_temperature":1.7}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T07:00:00Z","data":{"instant":{"details":{"air_temperature":1.6}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T08:00:00Z","data":{"instant":{"details":{"air_temperature":1.5}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T09:00:00Z","data":{"instant":{"details":{"air_temperature":1.4}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-15T10:00:00Z","data":{"instant":{"details":{"air_temperature":1.3}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
              {"time":"2026-11-16T00:00:00Z","data":{"instant":{"details":{"air_temperature":0.5}},"next_6_hours":{"summary":{"symbol_code":"lightsnow"},"details":{"precipitation_amount":0.4}},"next_12_hours":{"summary":{"symbol_code":"lightsnow"},"details":{}}}},
              {"time":"2026-11-16T06:00:00Z","data":{"instant":{"details":{"air_temperature":0.8}},"next_6_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}},"next_12_hours":{"summary":{"symbol_code":"cloudy"},"details":{}}}},
              {"time":"2026-11-16T12:00:00Z","data":{"instant":{"details":{"air_temperature":1.2}},"next_6_hours":{"summary":{"symbol_code":"snow"},"details":{"precipitation_amount":1.5}},"next_12_hours":{"summary":{"symbol_code":"snow"},"details":{}}}},
              {"time":"2026-11-16T18:00:00Z","data":{"instant":{"details":{"air_temperature":0.9}},"next_6_hours":{"summary":{"symbol_code":"lightsnow"},"details":{"precipitation_amount":0.6}},"next_12_hours":{"summary":{"symbol_code":"lightsnow"},"details":{}}}},
              {"time":"2026-11-17T00:00:00Z","data":{"instant":{"details":{"air_temperature":-0.5}},"next_6_hours":{"summary":{"symbol_code":"snow"},"details":{"precipitation_amount":1.0}},"next_12_hours":{"summary":{"symbol_code":"snow"},"details":{}}}},
              {"time":"2026-11-17T06:00:00Z","data":{"instant":{"details":{"air_temperature":-0.2}},"next_6_hours":{"summary":{"symbol_code":"lightsnow"},"details":{"precipitation_amount":0.5}},"next_12_hours":{"summary":{"symbol_code":"lightsnow"},"details":{}}}},
              {"time":"2026-11-17T12:00:00Z","data":{"instant":{"details":{"air_temperature":0.4}},"next_6_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}},"next_12_hours":{"summary":{"symbol_code":"cloudy"},"details":{}}}},
              {"time":"2026-11-17T18:00:00Z","data":{"instant":{"details":{"air_temperature":0.1}},"next_6_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}},"next_12_hours":{"summary":{"symbol_code":"cloudy"},"details":{}}}}
            ]
          }
        }
    """.trimIndent()

    private val mixedNow = Instant.parse("2026-11-14T19:30:00Z").toEpochMilli()

    @Test
    fun parsesCurrentHourlyAndDailyFromRealCompactShape() {
        val response = MetNoForecastParser.parse(
            body = compactJson,
            nowMillis = now,
            latitude = 59.9386,
            longitude = 30.3141,
            timeZone = TimeZone.of("Europe/Moscow")
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
            timeZone = TimeZone.of("Asia/Dubai")
        )
        assertEquals(25.2048, response.latitude, 0.001)
        assertEquals(55.2708, response.longitude, 0.001)
        assertNotNull(response.current)
        assertNotNull(response.daily)
        assertTrue((response.daily?.time?.size ?: 0) >= 1)
    }

    @Test
    fun precipitationAmounts_reachHourlyAndDailyMm() {
        val response = MetNoForecastParser.parse(
            body = compactJson,
            nowMillis = now,
            latitude = 59.9386,
            longitude = 30.3141,
            timeZone = TimeZone.of("Europe/Moscow")
        )
        // next_1_hours.precipitation_amount [0.0, 0.2, 0.0, 0.0] — без выдуманных нулей.
        assertEquals(0.2, response.hourly?.precipitation?.get(1) ?: 0.0, 0.001)

        val mapped = WeatherMapper.map(
            city = CityLocation("Санкт-Петербург", 59.9386, 30.3141),
            dto = response,
            isOffline = false,
            lastUpdated = 1_763_000_000_000L,
            trafficScore = null
        )
        assertTrue(mapped is WeatherMapResult.Success)
        val data = (mapped as WeatherMapResult.Success).data
        assertTrue(
            "В почасовке должен быть час с дождём > 0 мм",
            data.hourly.any { (it.precipitationMm ?: 0.0) > 0.0 }
        )
        // Суммы за сутки из тех же реальных мм: 0.0+0.2 → 0.2, 0.4+0.3 → 0.7.
        assertEquals(0.2, data.daily[0].precipitationMm ?: -1.0, 0.001)
        assertEquals(0.7, data.daily[1].precipitationMm ?: -1.0, 0.001)
    }

    @Test
    fun farDays_dailySumUsesSixHourAmounts_notNull() {
        val response = MetNoForecastParser.parse(
            body = mixedHorizonJson,
            nowMillis = mixedNow,
            latitude = 59.9386,
            longitude = 30.3141,
            timeZone = TimeZone.of("Europe/Moscow")
        )
        val daily = response.daily!!
        assertEquals(
            listOf("2026-11-14", "2026-11-15", "2026-11-16", "2026-11-17"),
            daily.time
        )
        // За 16.11 и 17.11 next_1_hours нет вовсе: сумма собирается из 6-часовых значений.
        assertEquals(2.5, daily.precipitationSum?.get(2) ?: -1.0, 0.001)
        assertEquals(1.5, daily.precipitationSum?.get(3) ?: -1.0, 0.001)

        // Дальний день доходит до домена: в детали дня будет «2.5 мм», а не «Нет данных».
        val mapped = WeatherMapper.map(
            city = CityLocation("Санкт-Петербург", 59.9386, 30.3141),
            dto = response,
            isOffline = false,
            lastUpdated = 1_763_000_000_000L,
            trafficScore = null
        )
        val data = (mapped as WeatherMapResult.Success).data
        assertEquals(4, data.daily.size)
        assertEquals(2.5, data.daily[2].precipitationMm ?: -1.0, 0.001)
        assertEquals(1.5, data.daily[3].precipitationMm ?: -1.0, 0.001)
    }

    @Test
    fun hourlySeries_coversNext24Hours_acrossMidnight() {
        val response = MetNoForecastParser.parse(
            body = mixedHorizonJson,
            nowMillis = mixedNow,
            latitude = 59.9386,
            longitude = 30.3141,
            timeZone = TimeZone.of("Europe/Moscow")
        )
        val hourly = response.hourly!!
        // Текущая точка — 20:00Z (23:00 МСК), впереди 15 часовых точек: ряд не обрывается на полуночи.
        assertEquals(15, hourly.time.size)
        assertTrue("почасовой ряд должен покрывать больше 12 часов", hourly.time.size >= 12)
        assertEquals("2026-11-14T23:00", hourly.time.first())
        assertEquals("2026-11-15T13:00", hourly.time.last())
        assertTrue(hourly.time.any { it.startsWith("2026-11-15") })

        // мм/ч только из next_1_hours: скользящие 6-часовые 1.2/0.9/0.8 не подставлены.
        assertTrue(hourly.precipitation.orEmpty().all { it != null })
        assertEquals(0.3, hourly.precipitation?.first() ?: -1.0, 0.001)
        assertTrue(hourly.precipitation.orEmpty().none { it == 1.2 || it == 0.9 || it == 0.8 })
    }

    @Test
    fun nearDay_withBothOneAndSixHour_doesNotDoubleCount() {
        val response = MetNoForecastParser.parse(
            body = mixedHorizonJson,
            nowMillis = mixedNow,
            latitude = 59.9386,
            longitude = 30.3141,
            timeZone = TimeZone.of("Europe/Moscow")
        )
        val daily = response.daily!!
        // 14.11: 0.3 мм за час и 1.2 мм за 6 часов на той же точке — это скользящее
        // окно, поэтому в сумму идёт только часовое. 15.11: 0.3 против 3.0 при наивном сложении.
        assertEquals(0.3, daily.precipitationSum?.get(0) ?: -1.0, 0.001)
        assertEquals(0.3, daily.precipitationSum?.get(1) ?: -1.0, 0.001)
    }

    @Test
    fun emptyDetailsObject_keepsNull_notZero() {
        val body = """
            {"properties":{"timeseries":[
              {"time":"2026-11-14T20:00:00Z","data":{"instant":{"details":{"air_temperature":2.0}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{}},"next_6_hours":{"summary":{"symbol_code":"cloudy"},"details":{}},"next_12_hours":{"summary":{"symbol_code":"cloudy"},"details":{}}}},
              {"time":"2026-11-14T21:00:00Z","data":{"instant":{"details":{"air_temperature":2.1}},"next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{}},"next_6_hours":{"summary":{"symbol_code":"cloudy"},"details":{}},"next_12_hours":{"summary":{"symbol_code":"cloudy"},"details":{}}}}
            ]}}
        """.trimIndent()
        val response = MetNoForecastParser.parse(
            body = body,
            nowMillis = mixedNow,
            latitude = 59.9386,
            longitude = 30.3141,
            timeZone = TimeZone.of("Europe/Moscow")
        )
        // Пустой details (реальный вид next_12_hours) — «нет данных», а не 0.0 мм.
        assertTrue(response.hourly?.precipitation?.all { it == null } == true)
        assertTrue(response.daily?.precipitationSum?.all { it == null } == true)
    }

    @Test
    fun missingPrecipitation_keepsNull_notZero() {
        val body = """
            {"properties":{"timeseries":[
              {"time":"2026-11-13T23:00:00Z","data":{"instant":{"details":{"air_temperature":2.0}},"next_1_hours":{"summary":{"symbol_code":"cloudy"}}}},
              {"time":"2026-11-14T00:00:00Z","data":{"instant":{"details":{"air_temperature":3.0}},"next_1_hours":{"summary":{"symbol_code":"lightrain"}}}},
              {"time":"2026-11-14T01:00:00Z","data":{"instant":{"details":{"air_temperature":4.0}},"next_1_hours":{"summary":{"symbol_code":"cloudy"}}}}
            ]}}
        """.trimIndent()
        val response = MetNoForecastParser.parse(
            body = body,
            nowMillis = now,
            latitude = 59.9,
            longitude = 30.3,
            timeZone = TimeZone.of("Europe/Moscow")
        )
        assertTrue(response.hourly?.precipitation?.all { it == null } == true)

        val mapped = WeatherMapper.map(
            city = CityLocation("Санкт-Петербург", 59.9, 30.3),
            dto = response,
            isOffline = false,
            lastUpdated = 1L,
            trafficScore = null
        )
        val data = (mapped as WeatherMapResult.Success).data
        assertTrue(data.hourly.all { it.precipitationMm == null })
        assertTrue(data.daily.all { it.precipitationMm == null })
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
