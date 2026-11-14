package com.koch.weather.data

import com.koch.weather.data.mapper.WeatherMapResult
import com.koch.weather.data.mapper.WeatherMapper
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.CurrentUnitsDto
import com.koch.weather.data.model.CurrentWeatherDto
import com.koch.weather.data.model.HourlyUnitsDto
import com.koch.weather.data.model.DailyWeatherDto
import com.koch.weather.data.model.HourlyWeatherDto
import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherErrorReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherMapperTest {

    private val city = CityLocation("Санкт-Петербург", 59.9386, 30.3141, "Россия")

    private fun sampleDto(): WeatherApiResponse = WeatherApiResponse(
        latitude = 59.9386,
        longitude = 30.3141,
        current = CurrentWeatherDto(
            time = "2026-11-14T10:00",
            temperature2m = 5.5,
            relativeHumidity2m = 69,
            apparentTemperature = 2.0,
            isDay = 1,
            precipitation = 0.0,
            weatherCode = 3,
            windSpeed10m = 22.0
        ),
        hourly = HourlyWeatherDto(
            time = listOf("2026-11-14T10:00", "2026-11-14T11:00"),
            temperature2m = listOf(5.5, 6.2),
            precipitationProbability = listOf(10, 15),
            weatherCode = listOf(3, 2),
            windSpeed10m = listOf(22.0, 20.0)
        ),
        daily = DailyWeatherDto(
            time = listOf("2026-11-14", "2026-11-15"),
            weatherCode = listOf(3, 2),
            temperature2mMax = listOf(6.2, 5.8),
            temperature2mMin = listOf(2.4, 1.2),
            precipitationProbabilityMax = listOf(15, 20)
        )
    )

    private fun map(dto: WeatherApiResponse, offline: Boolean = false, at: Long = 1_000L): WeatherDomainData {
        val result = WeatherMapper.map(
            city = city,
            dto = dto,
            isOffline = offline,
            lastUpdated = at,
            trafficScore = null,
            trafficAdvice = "Нет данных о пробках"
        )
        assertTrue("Ожидался Success, получено $result", result is WeatherMapResult.Success)
        return (result as WeatherMapResult.Success).data
    }

    @Test
    fun liveForecast_mapsRealValues_andKeepsTrafficNull() {
        val data = map(sampleDto(), offline = false, at = 123L)

        assertEquals("Санкт-Петербург", data.cityName)
        assertEquals(6, data.currentTemp) // 5.5 -> 6, а не выдуманные 21/5
        assertEquals(2, data.minTemp)
        assertEquals(6, data.maxTemp)
        assertEquals("Облачно", data.conditionDescription)
        assertFalse(data.isOffline)
        assertEquals(123L, data.lastUpdated)
        assertNull(data.trafficScore)
        assertEquals("Нет данных о пробках", data.trafficAdvice)
        assertEquals(2, data.hourly.size) // «Сейчас» + один реальный час, без mock-12
        assertEquals("Сейчас", data.hourly.first().hourLabel)
        assertEquals(2, data.daily.size)
        assertEquals("Сегодня", data.daily.first().dayLabel)
    }

    @Test
    fun openMeteoWindKmh_isConvertedToMetersPerSecond() {
        val dto = sampleDto().copy(
            currentUnits = CurrentUnitsDto(windSpeed10m = "km/h"),
            hourlyUnits = HourlyUnitsDto(windSpeed10m = "km/h")
        )
        val data = map(dto)

        assertEquals(22.0 / 3.6, data.windSpeed ?: 0.0, 0.001)
        assertEquals(22.0 / 3.6, data.hourly.first().wind ?: 0.0, 0.001)
        assertEquals(20.0 / 3.6, data.hourly[1].wind ?: 0.0, 0.001)
    }

    @Test
    fun openMeteoWindMs_staysMetersPerSecond() {
        val dto = sampleDto().copy(
            currentUnits = CurrentUnitsDto(windSpeed10m = "m/s"),
            hourlyUnits = HourlyUnitsDto(windSpeed10m = "m/s")
        )
        val data = map(dto)

        assertEquals(22.0, data.windSpeed ?: 0.0, 0.001)
        assertEquals(22.0, data.hourly.first().wind ?: 0.0, 0.001)
        assertEquals(20.0, data.hourly[1].wind ?: 0.0, 0.001)
    }

    @Test
    fun cachedForecast_isMarkedOffline_andUsesSavedTimestamp() {
        val data = map(sampleDto(), offline = true, at = 777L)
        assertTrue(data.isOffline)
        assertEquals(777L, data.lastUpdated)
    }

    @Test
    fun missingCurrent_isFailure_notFabricatedTemperature() {
        val dto = sampleDto().copy(current = null)
        val result = WeatherMapper.map(city, dto, false, 1L, null)
        assertTrue(result is WeatherMapResult.Failure)
        val failure = (result as WeatherMapResult.Failure).failure
        assertEquals(WeatherErrorReason.INVALID_DATA, failure.reason)
        assertTrue(failure.messageRu.contains("текущей"))
    }

    @Test
    fun missingCurrentTemperature_isFailure_notFallback21() {
        val dto = sampleDto().copy(
            current = CurrentWeatherDto(
                time = "2026-11-14T10:00",
                temperature2m = null,
                weatherCode = 3
            )
        )
        val result = WeatherMapper.map(city, dto, false, 1L, null)
        assertTrue(result is WeatherMapResult.Failure)
    }

    @Test
    fun missingWeatherCode_isFailure_notInventedPartlyCloudy() {
        val dto = sampleDto().copy(
            current = CurrentWeatherDto(
                time = "2026-11-14T10:00",
                temperature2m = 5.0,
                weatherCode = null
            )
        )
        val result = WeatherMapper.map(city, dto, false, 1L, null)
        assertTrue(result is WeatherMapResult.Failure)
    }

    @Test
    fun missingDaily_isFailure_notMockDaily() {
        val dto = sampleDto().copy(daily = null)
        val result = WeatherMapper.map(city, dto, false, 1L, null)
        assertTrue(result is WeatherMapResult.Failure)
    }

    @Test
    fun missingHourly_keepsOnlyRealCurrentPoint_noMockHours() {
        val data = map(sampleDto().copy(hourly = null))
        assertEquals(1, data.hourly.size)
        assertEquals("Сейчас", data.hourly.first().hourLabel)
        assertEquals(data.currentTemp, data.hourly.first().temp)
        assertTrue(data.daily.size == 2) // суточный прогноз реальный
    }

    @Test
    fun realTrafficScore_passesThrough() {
        val result = WeatherMapper.map(
            city = city,
            dto = sampleDto(),
            isOffline = false,
            lastUpdated = 5L,
            trafficScore = 7,
            trafficAdvice = "Серьезные пробки"
        )
        val data = (result as WeatherMapResult.Success).data
        assertEquals(7, data.trafficScore)
        assertEquals("Серьезные пробки", data.trafficAdvice)
    }

    @Test
    fun missingPrecipitationProbability_isMarkedUnknown_notZeroPercent() {
        val dto = sampleDto().copy(
            hourly = sampleDto().hourly?.copy(precipitationProbability = null),
            daily = sampleDto().daily?.copy(precipitationProbabilityMax = null)
        )
        val data = map(dto)
        assertFalse(data.hourly.first().popKnown)
        assertFalse(data.daily.first().popKnown)
    }

    @Test
    fun presentPrecipitationProbability_isMarkedKnown() {
        val data = map(sampleDto())
        assertTrue(data.hourly.first().popKnown)
        assertTrue(data.daily.first().popKnown)
    }

    @Test
    fun missingHumidityAndWind_stayNull_notZero() {
        val dto = sampleDto().copy(
            current = sampleDto().current?.copy(
                relativeHumidity2m = null,
                windSpeed10m = null
            ),
            hourly = sampleDto().hourly?.copy(windSpeed10m = null)
        )
        val data = map(dto)
        assertNull(data.humidity)
        assertNull(data.windSpeed)
        assertNull(data.hourly.first().wind)
    }

    @Test
    fun dataSourceLabel_isPassedForLiveForecast() {
        val result = WeatherMapper.map(
            city = city,
            dto = sampleDto(),
            isOffline = false,
            lastUpdated = 5L,
            trafficScore = null,
            dataSourceLabel = "MET Norway"
        )
        val data = (result as WeatherMapResult.Success).data
        assertEquals("MET Norway", data.dataSourceLabel)
    }

    @Test
    fun cachedForecast_hasNullDataSourceLabel() {
        val data = map(sampleDto(), offline = true)
        assertNull(data.dataSourceLabel)
    }

    @Test
    fun malformedDailyArrays_doNotProduceInventedDays() {
        val dto = sampleDto().copy(
            daily = DailyWeatherDto(
                time = listOf("2026-11-14", "2026-11-15"),
                weatherCode = listOf(3),
                temperature2mMax = listOf(6.2),
                temperature2mMin = listOf(2.4)
            )
        )
        val data = map(dto)
        assertEquals(1, data.daily.size)
        assertNotNull(data.daily.first())
    }
}
