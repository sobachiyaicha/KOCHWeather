package com.koch.weather.data.provider

import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
 * Параллельная гонка основных провайдеров (MetNo + OpenMeteo), затем
 * последовательный резерв (7timer). Резерв запускается только если оба
 * основных вернули Failure, поэтому не тормозит нормальный сценарий.
 */
class WeatherProviderChain(
    private val providers: List<WeatherProvider>,
    private val fallbackProviders: List<WeatherProvider> = emptyList()
) {

    /**
     * Общий бюджет на гонку + резерв — 30 секунд (UI показывает прогресс).
     * Если ни один провайдер не ответил, отдаём честный TIMEOUT.
     */
    suspend fun fetch(latitude: Double, longitude: Double): WeatherProviderChainResult =
        withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
            fetchWithFallback(latitude, longitude)
        } ?: timeoutFailure()

    private suspend fun fetchWithFallback(
        latitude: Double,
        longitude: Double
    ): WeatherProviderChainResult {
        // Резерв получает отдельное окно: гонка основных не должна съесть весь бюджет.
        val primary = withTimeoutOrNull(PRIMARY_TIMEOUT_MS) {
            race(latitude, longitude)
        } ?: WeatherProviderChainResult.AllFailed(
            listOf(
                ProviderAttemptFailure(
                    source = WeatherSource.NONE,
                    failure = WeatherFailure(
                        WeatherErrorReason.TIMEOUT,
                        "Основные провайдеры не ответили за ${PRIMARY_TIMEOUT_MS / 1000} секунд."
                    )
                )
            )
        )
        if (primary is WeatherProviderChainResult.Success) return primary

        val failures = (primary as WeatherProviderChainResult.AllFailed)
            .failedAttempts
            .toMutableList()

        for (provider in fallbackProviders) {
            val result = withTimeoutOrNull(FALLBACK_TIMEOUT_MS) {
                try {
                    provider.fetchForecast(latitude, longitude)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ProviderFetchResult.Failure(WeatherErrors.from(e))
                }
            } ?: ProviderFetchResult.Failure(
                WeatherFailure(
                    WeatherErrorReason.TIMEOUT,
                    "Резервный источник ${provider.source.displayNameRu} не ответил за " +
                        "${FALLBACK_TIMEOUT_MS / 1000} секунд."
                )
            )
            when (result) {
                is ProviderFetchResult.Success -> return WeatherProviderChainResult.Success(
                    response = result.response,
                    source = provider.source,
                    fetchedAt = result.fetchedAt,
                    fromProviderCache = result.fromProviderCache,
                    failedAttempts = failures.toList()
                )

                is ProviderFetchResult.Failure -> failures.add(
                    ProviderAttemptFailure(provider.source, result.failure)
                )
            }
        }
        return WeatherProviderChainResult.AllFailed(failures)
    }

    private suspend fun race(
        latitude: Double,
        longitude: Double
    ): WeatherProviderChainResult = coroutineScope {
        if (providers.isEmpty()) {
            return@coroutineScope WeatherProviderChainResult.AllFailed(emptyList())
        }

        val results = Channel<Pair<WeatherSource, ProviderFetchResult>>(providers.size)
        val jobs = providers.map { provider ->
            launch {
                val result = try {
                    provider.fetchForecast(latitude, longitude)
                } catch (e: CancellationException) {
                    return@launch
                } catch (e: Exception) {
                    ProviderFetchResult.Failure(WeatherErrors.from(e))
                }
                // Если гонку уже выиграл другой провайдер, канал закрыт.
                try {
                    results.send(provider.source to result)
                } catch (_: Exception) {
                    // проигравший/отменённый запрос — ожидаемо
                }
            }
        }

        val failedAttempts = mutableListOf<ProviderAttemptFailure>()
        repeat(providers.size) {
            val (source, result) = results.receive()
            when (result) {
                is ProviderFetchResult.Success -> {
                    jobs.forEach { it.cancel() }
                    results.close()
                    return@coroutineScope WeatherProviderChainResult.Success(
                        response = result.response,
                        source = source,
                        fetchedAt = result.fetchedAt,
                        fromProviderCache = result.fromProviderCache,
                        failedAttempts = failedAttempts.toList()
                    )
                }

                is ProviderFetchResult.Failure -> failedAttempts.add(
                    ProviderAttemptFailure(source, result.failure)
                )
            }
        }
        WeatherProviderChainResult.AllFailed(failedAttempts)
    }

    private fun timeoutFailure(): WeatherProviderChainResult =
        WeatherProviderChainResult.AllFailed(
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

    companion object {
        const val PRIMARY_TIMEOUT_MS = 20_000L
        const val FALLBACK_TIMEOUT_MS = 8_000L
        const val TOTAL_TIMEOUT_MS = 30_000L
    }
}
