package com.koch.weather.data.provider

import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.CitySearchResult
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

enum class GeocodingSource(val displayNameRu: String) {
    PHOTON("Photon (OSM)"),
    NOMINATIM("Nominatim (OSM)"),
    OPEN_METEO("Open-Meteo Geocoding")
}

/** Абстракция поиска городов: Photon → Nominatim → Open-Meteo geocoding. */
interface GeocodingProvider {
    val source: GeocodingSource
    suspend fun search(query: String): CitySearchResult
}

/**
 * Цепочка геокодинга. Пустой успешный ответ не считается ошибкой: пробуем
 * следующего провайдера, а если все пустые — возвращаем пустой список.
 */
class GeocodingChain(private val providers: List<GeocodingProvider>) {

    /**
     * Общий бюджет поиска — 12 секунд. Photon имеет собственные 4 с, после
     * его таймаута сразу стартует Nominatim; UI показывает isSearching и может
     * отменить job (OkHttp Call.cancel()).
     */
    suspend fun search(query: String): CitySearchResult =
        withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
            searchInternal(query)
        } ?: CitySearchResult.Failure(
            WeatherFailure(
                WeatherErrorReason.TIMEOUT,
                "Поиск города занял больше ${SEARCH_TIMEOUT_MS / 1000} секунд. " +
                    "Проверьте соединение и повторите."
            )
        )

    private suspend fun searchInternal(query: String): CitySearchResult {
        if (query.isBlank()) return CitySearchResult.Success(emptyList())
        if (providers.isEmpty()) {
            return CitySearchResult.Failure(
                WeatherFailure(WeatherErrorReason.UNKNOWN, "Поиск городов недоступен.")
            )
        }

        var emptyResultSeen = false
        val failures = mutableListOf<String>()
        for (provider in providers) {
            val result = try {
                provider.search(query)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CitySearchResult.Failure(WeatherErrors.from(e))
            }
            when (result) {
                is CitySearchResult.Success -> {
                    if (result.cities.isNotEmpty()) return result
                    emptyResultSeen = true
                }

                is CitySearchResult.Failure -> failures.add(
                    "${provider.source.displayNameRu}: ${result.failure.messageRu}"
                )
            }
        }

        return if (emptyResultSeen) {
            CitySearchResult.Success(emptyList())
        } else {
            CitySearchResult.Failure(
                WeatherFailure(
                    WeatherErrorReason.NO_NETWORK,
                    "Не удалось найти город. " + failures.joinToString("; ")
                )
            )
        }
    }

    companion object {
        const val SEARCH_TIMEOUT_MS = 12_000L
    }
}
