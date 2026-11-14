package com.koch.weather.data

import com.koch.weather.data.provider.GeocodingChain
import com.koch.weather.data.provider.MetNoProvider
import com.koch.weather.data.provider.NominatimGeocodingProvider
import com.koch.weather.data.provider.PhotonGeocodingProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTimeoutConfigTest {

    @Test
    fun metNoRetryClient_hasIncreasedReadTimeout() {
        val client = MetNoProvider.retryClient()
        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(25_000, client.readTimeoutMillis)
        assertEquals(25_000, client.writeTimeoutMillis)
        assertEquals(35_000, client.callTimeoutMillis)
    }

    @Test
    fun geocodingCascade_isFastAndBounded() {
        val photon = PhotonGeocodingProvider.defaultClient()
        assertEquals(4_000, photon.connectTimeoutMillis)
        assertEquals(4_000, photon.readTimeoutMillis)
        assertEquals(6_000, photon.callTimeoutMillis)

        val nominatim = NominatimGeocodingProvider.defaultClient()
        assertEquals(4_000, nominatim.connectTimeoutMillis)
        assertEquals(6_000, nominatim.readTimeoutMillis)
        assertEquals(8_000, nominatim.callTimeoutMillis)

        assertEquals(12_000L, GeocodingChain.SEARCH_TIMEOUT_MS)
    }

    @Test
    fun metNoPrimaryClient_isShorterForRetryWindow() {
        val client = MetNoProvider.defaultClient()
        assertEquals(6_000, client.connectTimeoutMillis)
        assertEquals(10_000, client.readTimeoutMillis)
        assertEquals(12_000, client.callTimeoutMillis)
    }
}
