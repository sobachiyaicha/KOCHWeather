package com.koch.weather.data

import com.koch.weather.data.provider.MetNoCachePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetNoCachePolicyTest {

    private val policy = MetNoCachePolicy()

    @Test
    fun scandinavia_is30Minutes() {
        assertEquals(30L * 60L * 1000L, policy.minRefreshIntervalMs(59.9386, 30.3141))
        assertEquals(30L * 60L * 1000L, policy.minRefreshIntervalMs(59.9139, 10.7522))
        assertTrue(policy.isScandinavia(59.9386, 30.3141))
    }

    @Test
    fun restOfWorld_is3Hours() {
        assertEquals(3L * 60L * 60L * 1000L, policy.minRefreshIntervalMs(25.2048, 55.2708))
        assertEquals(3L * 60L * 60L * 1000L, policy.minRefreshIntervalMs(55.7558, 37.6173))
        assertEquals(3L * 60L * 60L * 1000L, policy.minRefreshIntervalMs(53.9006, 27.5590))
        assertFalse(policy.isScandinavia(25.2048, 55.2708))
    }

    @Test
    fun cacheDecision_respectsProviderAndAge() {
        val now = 10_000_000L
        assertTrue(
            policy.shouldUseCache("met_no", now - 10 * 60_000L, now, 59.9386, 30.3141)
        )
        assertFalse(
            policy.shouldUseCache("met_no", now - 40 * 60_000L, now, 59.9386, 30.3141)
        )
        assertTrue(
            policy.shouldUseCache("met_no", now - 2 * 60 * 60_000L, now, 25.2048, 55.2708)
        )
        assertFalse(
            policy.shouldUseCache("met_no", now - 4 * 60 * 60_000L, now, 25.2048, 55.2708)
        )
        assertFalse(policy.shouldUseCache("open_meteo", now - 1_000L, now, 59.9, 30.3))
        assertFalse(policy.shouldUseCache(null, now - 1_000L, now, 59.9, 30.3))
        assertFalse(policy.shouldUseCache("met_no", 0L, now, 59.9, 30.3))
    }
}
