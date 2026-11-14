package com.koch.weather.ui.main

import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.WeatherDataState
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherIconType
import com.koch.weather.data.model.WeatherLoadResult
import com.koch.weather.data.model.WeatherSource
import com.koch.weather.data.repository.WeatherLoadAction
import com.koch.weather.data.repository.WeatherLoadPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherLoadPolicyTest {

    private val now = 1_000_000L
    private val cityLat = 59.9386
    private val cityLon = 30.3141

    private fun domain(temp: Int = 5, updatedAt: Long = now - 60_000L) = WeatherDomainData(
        cityName = "Санкт-Петербург",
        currentTemp = temp,
        conditionDescription = "Облачно",
        minTemp = 1,
        maxTemp = 7,
        humidity = null,
        windSpeed = null,
        weatherCode = 3,
        iconType = WeatherIconType.CLOUDY,
        clothingAdvice = "Холодно",
        trafficScore = null,
        trafficAdvice = "Нет данных о пробках",
        hourly = emptyList(),
        daily = emptyList(),
        isOffline = false,
        lastUpdated = updatedAt,
        dataSourceLabel = "MET Norway"
    )

    @Test
    fun cacheWithinPolicy_returnsCachedState_notLive() {
        val action = WeatherLoadPolicy.decide(
            isOnline = true,
            forceRefresh = false,
            sourceId = WeatherSource.MET_NO.id,
            savedAt = now - 5 * 60_000L,
            now = now,
            latitude = cityLat,
            longitude = cityLon
        )
        assertEquals(WeatherLoadAction.SERVE_CACHE, action)

        val cachedDomain = domain()
        val cached = WeatherLoadResult.Cached(
            data = cachedDomain,
            source = WeatherSource.MET_NO,
            reason = WeatherFailure(
                WeatherErrorReason.CACHE_POLICY,
                "MET Norway: данные из кэша"
            )
        )
        val state = WeatherUiReducer.apply(
            state = MainScreenUiState(weather = null, weatherState = WeatherDataState.LOADING),
            result = cached,
            isConnected = true
        )
        assertEquals(WeatherDataState.CACHED, state.weatherState)
        assertNotEquals(WeatherDataState.LIVE, state.weatherState)
        assertTrue(state.isOffline)
        assertFalse(state.isWeatherUnavailable)
        assertNotNull(state.weatherSourceNote)
        assertEquals(cachedDomain.lastUpdated, state.weatherLastUpdated)
        assertTrue(state.weatherSourceNote!!.contains("кэша"))
    }

    @Test
    fun forcedRefresh_skipsCacheAndGoesToNetwork() {
        val action = WeatherLoadPolicy.decide(
            isOnline = true,
            forceRefresh = true,
            sourceId = WeatherSource.MET_NO.id,
            savedAt = now - 60_000L,
            now = now,
            latitude = cityLat,
            longitude = cityLon
        )
        assertEquals(WeatherLoadAction.FETCH_NETWORK, action)
    }

    @Test
    fun offlineWithCache_staysCached() {
        val action = WeatherLoadPolicy.decide(
            isOnline = false,
            forceRefresh = false,
            sourceId = WeatherSource.MET_NO.id,
            savedAt = now - 60_000L,
            now = now,
            latitude = cityLat,
            longitude = cityLon
        )
        assertEquals(WeatherLoadAction.OFFLINE_FALLBACK, action)

        val cached = WeatherLoadResult.Cached(
            data = domain(),
            source = WeatherSource.MET_NO,
            reason = WeatherFailure(WeatherErrorReason.NO_NETWORK, "Нет сети")
        )
        val state = WeatherUiReducer.apply(MainScreenUiState(), cached, isConnected = false)
        assertEquals(WeatherDataState.CACHED, state.weatherState)
        assertTrue(state.isOffline)
        assertNotNull(state.weatherSourceNote)
    }

    @Test
    fun liveAfterSuccess_clearsCachedBanner() {
        val cachedState = MainScreenUiState(
            weather = domain(temp = 5).copy(isOffline = true),
            weatherState = WeatherDataState.CACHED,
            isOffline = true,
            weatherSource = WeatherSource.MET_NO,
            weatherAttributionRu = WeatherUiReducer.cachedAttribution(WeatherSource.MET_NO),
            weatherSourceNote = "Данные из кэша",
            weatherLastUpdated = now - 60_000L
        )
        val liveDomain = domain(temp = 6, updatedAt = now).copy(
            isOffline = false,
            dataSourceLabel = "MET Norway"
        )
        val state = WeatherUiReducer.apply(
            cachedState,
            WeatherLoadResult.Live(liveDomain, WeatherSource.MET_NO),
            isConnected = true
        )

        assertEquals(WeatherDataState.LIVE, state.weatherState)
        assertFalse(state.isOffline)
        assertNull(state.weatherSourceNote)
        assertEquals(now, state.weatherLastUpdated)
        assertEquals("MET Norway", state.weather?.dataSourceLabel)
        assertEquals(6, state.weather?.currentTemp)
    }

    @Test
    fun staleOrForeignCache_goesToNetwork() {
        val stale = WeatherLoadPolicy.decide(
            isOnline = true,
            forceRefresh = false,
            sourceId = WeatherSource.MET_NO.id,
            savedAt = now - 31 * 60_000L,
            now = now,
            latitude = cityLat,
            longitude = cityLon
        )
        assertEquals(WeatherLoadAction.FETCH_NETWORK, stale)

        val foreign = WeatherLoadPolicy.decide(
            isOnline = true,
            forceRefresh = false,
            sourceId = WeatherSource.OPEN_METEO.id,
            savedAt = now - 60_000L,
            now = now,
            latitude = cityLat,
            longitude = cityLon
        )
        assertEquals(WeatherLoadAction.FETCH_NETWORK, foreign)
    }
}
