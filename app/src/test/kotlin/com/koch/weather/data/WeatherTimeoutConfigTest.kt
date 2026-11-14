package com.koch.weather.data

import com.koch.weather.data.provider.MetNoProvider
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
    fun metNoPrimaryClient_isShorterForRetryWindow() {
        val client = MetNoProvider.defaultClient()
        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(10_000, client.readTimeoutMillis)
        assertEquals(12_000, client.callTimeoutMillis)
    }
}
