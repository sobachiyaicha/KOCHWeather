package com.koch.weather.data.model

/** Состояние авто-повтора погоды; nextRetryAt — epoch millis. */
@androidx.compose.runtime.Immutable
data class WeatherRetryState(
    val attempt: Int = 0,
    val isRetrying: Boolean = false,
    val nextRetryAt: Long? = null
)

/**
 * Экспоненциальный backoff для сбоев провайдеров:
 * без кэша 5 → 20 → 40 → 60 с (дальше 60), с кэшем — не чаще 60 с.
 * В фоне повторы не планируются; успех и ручная кнопка сбрасывают серию.
 */
object WeatherRetryPolicy {

    val NO_CACHE_DELAYS_MS = longArrayOf(5_000L, 20_000L, 40_000L, 60_000L)
    const val CACHE_DELAY_MS = 60_000L

    fun delayFor(attempt: Int, hasCache: Boolean): Long {
        if (hasCache) return CACHE_DELAY_MS
        val index = attempt.coerceIn(0, NO_CACHE_DELAYS_MS.lastIndex)
        return NO_CACHE_DELAYS_MS[index]
    }

    fun onFailure(
        state: WeatherRetryState,
        now: Long,
        hasCache: Boolean,
        foreground: Boolean
    ): WeatherRetryState {
        if (!foreground) {
            return state.copy(isRetrying = false, nextRetryAt = null)
        }
        val delay = delayFor(state.attempt, hasCache)
        return WeatherRetryState(
            attempt = state.attempt + 1,
            isRetrying = true,
            nextRetryAt = now + delay
        )
    }

    fun onSuccess(): WeatherRetryState = WeatherRetryState()

    fun onBackground(state: WeatherRetryState): WeatherRetryState =
        state.copy(isRetrying = false, nextRetryAt = null)

    fun onManualRetry(): WeatherRetryState = WeatherRetryState(
        attempt = 0,
        isRetrying = true,
        nextRetryAt = null
    )
}
