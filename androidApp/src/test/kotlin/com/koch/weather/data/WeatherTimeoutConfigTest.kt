package com.koch.weather.data

import com.koch.weather.data.provider.GeocodingChain
import com.koch.weather.data.provider.MetNoProvider
import com.koch.weather.data.provider.NominatimGeocodingProvider
import com.koch.weather.data.provider.PhotonGeocodingProvider
import com.koch.weather.data.provider.SevenTimerProvider
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Таймауты провайдеров (01-APP-SPEC §7.1). OkHttp-клиенты заменены Ktor;
 * числа проверяются по константам провайдеров — конфигурация установленного
 * HttpTimeout-плагина в ktor 3.6.1 нечитаема публично (ClientPluginInstance.config
 * private), а сами константы идут единственным источником в install(HttpTimeout)
 * через [providerHttpClient]. Соответствие прежним OkHttp-лимитам:
 *  - connectTimeoutMillis = connectTimeout;
 *  - socketTimeoutMillis  = readTimeout;
 *  - requestTimeoutMillis = callTimeout;
 *  - writeTimeout аналога в Ktor нет — у провайдеров только GET без тела
 *    (предохранитель движка: OkHttp write 10 с из httpClientEngine()).
 */
class WeatherTimeoutConfigTest {

    @Test
    fun metNoRetryClient_hasIncreasedReadTimeout() {
        assertEquals(10_000L, MetNoProvider.RETRY_CONNECT_TIMEOUT_MILLIS)
        assertEquals(25_000L, MetNoProvider.RETRY_READ_TIMEOUT_MILLIS)
        assertEquals(35_000L, MetNoProvider.RETRY_CALL_TIMEOUT_MILLIS)
    }

    @Test
    fun sevenTimerFallback_budgetIsEightSeconds() {
        assertEquals(4_000L, SevenTimerProvider.CONNECT_TIMEOUT_MILLIS)
        assertEquals(6_000L, SevenTimerProvider.READ_TIMEOUT_MILLIS)
        assertEquals(8_000L, SevenTimerProvider.CALL_TIMEOUT_MILLIS)
    }

    @Test
    fun geocodingCascade_isFastAndBounded() {
        assertEquals(4_000L, PhotonGeocodingProvider.CONNECT_TIMEOUT_MILLIS)
        assertEquals(4_000L, PhotonGeocodingProvider.READ_TIMEOUT_MILLIS)
        assertEquals(6_000L, PhotonGeocodingProvider.CALL_TIMEOUT_MILLIS)

        assertEquals(4_000L, NominatimGeocodingProvider.CONNECT_TIMEOUT_MILLIS)
        assertEquals(6_000L, NominatimGeocodingProvider.READ_TIMEOUT_MILLIS)
        assertEquals(8_000L, NominatimGeocodingProvider.CALL_TIMEOUT_MILLIS)

        assertEquals(12_000L, GeocodingChain.SEARCH_TIMEOUT_MS)
    }

    @Test
    fun metNoPrimaryClient_isShorterForRetryWindow() {
        assertEquals(6_000L, MetNoProvider.DEFAULT_CONNECT_TIMEOUT_MILLIS)
        assertEquals(10_000L, MetNoProvider.DEFAULT_READ_TIMEOUT_MILLIS)
        assertEquals(12_000L, MetNoProvider.DEFAULT_CALL_TIMEOUT_MILLIS)
    }
}
