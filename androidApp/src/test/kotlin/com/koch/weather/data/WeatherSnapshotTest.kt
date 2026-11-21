package com.koch.weather.data

import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.HourlyItem
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherIconType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * R19: в DataStore сохраняется полный доменный снимок, а не только ответ API.
 * Снимок должен переживать сериализацию вместе с временем и подписью источника.
 */
class WeatherSnapshotTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun fullDomainSnapshot_roundTripsWithTimestampAndSourceLabel() {
        val snapshot = WeatherDomainData(
            cityName = "Санкт-Петербург",
            currentTemp = 3,
            conditionDescription = "Облачно",
            minTemp = 1,
            maxTemp = 6,
            humidity = 82,
            windSpeed = 5.1,
            weatherCode = 3,
            iconType = WeatherIconType.CLOUDY,
            clothingAdvice = "Холодно. Демисезонное пальто или теплая куртка, шарф.",
            trafficScore = null,
            trafficAdvice = "Нет данных о пробках",
            hourly = listOf(
                HourlyItem(
                    hourLabel = "Сейчас",
                    temp = 3,
                    weatherCode = 3,
                    iconType = WeatherIconType.CLOUDY,
                    pop = 0,
                    wind = 5.1,
                    popKnown = false
                )
            ),
            daily = listOf(
                DailyItem(
                    dayLabel = "Сегодня",
                    weatherCode = 3,
                    iconType = WeatherIconType.CLOUDY,
                    pop = 0,
                    minTemp = 1,
                    maxTemp = 6,
                    popKnown = false
                )
            ),
            isOffline = false,
            lastUpdated = 1_763_000_000_000L,
            dataSourceLabel = "MET Norway"
        )

        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString<WeatherDomainData>(encoded)

        assertEquals(snapshot, decoded)
        assertEquals(1_763_000_000_000L, decoded.lastUpdated)
        assertEquals("MET Norway", decoded.dataSourceLabel)
        assertEquals(3, decoded.currentTemp)
        assertEquals("Нет данных о пробках", decoded.trafficAdvice)
    }
}
