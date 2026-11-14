package com.koch.weather.data.provider

import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.CitySearchResult
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import kotlinx.coroutines.CancellationException

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

    suspend fun search(query: String): CitySearchResult {
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
}
