package com.koch.weather.data

import com.koch.weather.data.model.WeatherRetryPolicy
import com.koch.weather.data.model.WeatherRetryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRetryPolicyTest {

    @Test
    fun noCacheBackoff_isFiveTwentyFortySixtyThenCapped() {
        var state = WeatherRetryState()
        val intervals = mutableListOf<Long>()
        var now = 0L
        repeat(5) {
            val next = WeatherRetryPolicy.onFailure(
                state = state,
                now = now,
                hasCache = false,
                foreground = true
            )
            val nextAt = next.nextRetryAt ?: error("nextRetryAt is null")
            intervals += nextAt - now
            now = nextAt
            state = next
        }
        assertEquals(listOf(5_000L, 20_000L, 40_000L, 60_000L, 60_000L), intervals)
        assertEquals(5, state.attempt)
    }

    @Test
    fun liveCacheBackoff_isNeverMoreOftenThanSixtySeconds() {
        var state = WeatherRetryState()
        val intervals = mutableListOf<Long>()
        var now = 0L
        repeat(3) {
            val next = WeatherRetryPolicy.onFailure(
                state = state,
                now = now,
                hasCache = true,
                foreground = true
            )
            val nextAt = next.nextRetryAt ?: error("nextRetryAt is null")
            intervals += nextAt - now
            now = nextAt
            state = next
        }
        assertEquals(listOf(60_000L, 60_000L, 60_000L), intervals)
    }

    @Test
    fun success_resetsBackoff() {
        var state = WeatherRetryState()
        state = WeatherRetryPolicy.onFailure(state, now = 0, hasCache = false, foreground = true)
        state = WeatherRetryPolicy.onFailure(state, now = 5_000, hasCache = false, foreground = true)
        assertEquals(2, state.attempt)

        state = WeatherRetryPolicy.onSuccess()
        assertEquals(0, state.attempt)
        assertFalse(state.isRetrying)
        assertNull(state.nextRetryAt)
    }

    @Test
    fun background_neverSchedulesRetry() {
        val previous = WeatherRetryState(attempt = 2, isRetrying = true, nextRetryAt = 123)
        val state = WeatherRetryPolicy.onFailure(
            state = previous,
            now = 1_000,
            hasCache = false,
            foreground = false
        )
        assertEquals(2, state.attempt)
        assertFalse(state.isRetrying)
        assertNull(state.nextRetryAt)
    }

    @Test
    fun manualRetry_isImmediateAndResetsAttempt() {
        val state = WeatherRetryPolicy.onManualRetry()
        assertEquals(0, state.attempt)
        assertTrue(state.isRetrying)
        assertNull(state.nextRetryAt)
    }

    @Test
    fun backgroundTransition_hidesCountdownWithoutLosingAttempt() {
        val active = WeatherRetryState(attempt = 3, isRetrying = true, nextRetryAt = 500_000)
        val background = WeatherRetryPolicy.onBackground(active)
        assertEquals(3, background.attempt)
        assertFalse(background.isRetrying)
        assertNull(background.nextRetryAt)
    }
}
