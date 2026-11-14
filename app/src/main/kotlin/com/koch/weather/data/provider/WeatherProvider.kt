package com.koch.weather.data.provider

import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Единый User-Agent для OSM/MET Norway: без него MET отвечает 403. */
const val WEATHER_HTTP_USER_AGENT =
    "KOCHWeather/1.0 (android; +https://github.com/koch/weather)"

sealed interface ProviderFetchResult {
    data class Success(
        val response: WeatherApiResponse,
        val fetchedAt: Long,
        val fromProviderCache: Boolean = false
    ) : ProviderFetchResult

    data class Failure(val failure: WeatherFailure) : ProviderFetchResult
}

/**
 * Абстракция источника прогноза. Основной — MET Norway, резерв — Open-Meteo.
 */
interface WeatherProvider {
    val source: WeatherSource

    suspend fun fetchForecast(latitude: Double, longitude: Double): ProviderFetchResult

    /** Минимальный интервал между сетевыми запросами к провайдеру (политика MET Norway). */
    fun minRefreshIntervalMs(latitude: Double, longitude: Double): Long = 0L
}

data class ProviderAttemptFailure(
    val source: WeatherSource,
    val failure: WeatherFailure
)

sealed interface WeatherProviderChainResult {
    data class Success(
        val response: WeatherApiResponse,
        val source: WeatherSource,
        val fetchedAt: Long,
        val fromProviderCache: Boolean,
        val failedAttempts: List<ProviderAttemptFailure>
    ) : WeatherProviderChainResult

    data class AllFailed(
        val failedAttempts: List<ProviderAttemptFailure>
    ) : WeatherProviderChainResult
}

/**
 * Последовательно пробует провайдеров: MetNo → OpenMeteo. Ошибки первого
 * источника сохраняются, чтобы UI показал причину failover-а.
 */
class WeatherProviderChain(private val providers: List<WeatherProvider>) {

    /**
     * Общий предохранитель: даже если MET и Open-Meteo по очереди висят на
     * таймаутах, UI получает «нет данных» не позже чем через 20 секунд.
     */
    suspend fun fetch(latitude: Double, longitude: Double): WeatherProviderChainResult =
        withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
            fetchInternal(latitude, longitude)
        } ?: WeatherProviderChainResult.AllFailed(
            listOf(
                ProviderAttemptFailure(
                    source = WeatherSource.NONE,
                    failure = WeatherFailure(
                        WeatherErrorReason.TIMEOUT,
                        "Провайдеры погоды не ответили за ${TOTAL_TIMEOUT_MS / 1000} секунд."
                    )
                )
            )
        )

    private suspend fun fetchInternal(
        latitude: Double,
        longitude: Double
    ): WeatherProviderChainResult {
        val failedAttempts = mutableListOf<ProviderAttemptFailure>()
        for (provider in providers) {
            val result = try {
                provider.fetchForecast(latitude, longitude)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ProviderFetchResult.Failure(WeatherErrors.from(e))
            }
            when (result) {
                is ProviderFetchResult.Success -> return WeatherProviderChainResult.Success(
                    response = result.response,
                    source = provider.source,
                    fetchedAt = result.fetchedAt,
                    fromProviderCache = result.fromProviderCache,
                    failedAttempts = failedAttempts.toList()
                )

                is ProviderFetchResult.Failure -> failedAttempts.add(
                    ProviderAttemptFailure(provider.source, result.failure)
                )
            }
        }
        return WeatherProviderChainResult.AllFailed(failedAttempts)
    }

    companion object {
        const val TOTAL_TIMEOUT_MS = 20_000L
    }
}
