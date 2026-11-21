package com.koch.weather.data.provider

import com.koch.weather.data.api.formatFixed
import com.koch.weather.data.api.urlEncodeUtf8
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.CitySearchResult
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.platform.nowMillis
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Резервный геокодер OpenStreetMap: Nominatim.
 *
 * Политика OSM: не чаще 1 запроса в секунду, обязательный User-Agent.
 * accept-language=ru даёт русские названия.
 */
class NominatimGeocodingProvider(
    private val client: HttpClient = defaultClient(),
    private val clock: () -> Long = { nowMillis() },
    private val delayFn: suspend (Long) -> Unit = { if (it > 0) delay(it) }
) : GeocodingProvider {

    override val source: GeocodingSource = GeocodingSource.NOMINATIM

    private val mutex = Mutex()
    private var lastRequestAt = 0L

    override suspend fun search(query: String): CitySearchResult = withContext(Dispatchers.Default) {
        val encoded = urlEncodeUtf8(query.trim())
        val url = "https://nominatim.openstreetmap.org/search" +
            "?format=jsonv2&addressdetails=1&q=$encoded&limit=5&accept-language=ru"
        mutex.withLock {
            val now = clock()
            delayFn(rateLimitDelayMs(now, lastRequestAt))
            lastRequestAt = clock()
            try {
                val response = client.get(url) {
                    header("User-Agent", WEATHER_HTTP_USER_AGENT)
                    header("Accept", "application/json")
                }
                if (!response.status.isSuccess()) {
                    return@withLock CitySearchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.HTTP_ERROR,
                            "Nominatim вернул ошибку (HTTP ${response.status.value})."
                        )
                    )
                }
                val body = response.bodyAsText()
                if (body.isEmpty()) {
                    return@withLock CitySearchResult.Failure(
                        WeatherFailure(WeatherErrorReason.PARSE_ERROR, "Nominatim вернул пустой ответ.")
                    )
                }
                val cities = try {
                    parseResponse(body)
                } catch (e: Exception) {
                    return@withLock CitySearchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.PARSE_ERROR,
                            "Nominatim: не удалось разобрать ответ (${e.message})."
                        )
                    )
                }
                CitySearchResult.Success(cities)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val mapped = com.koch.weather.data.mapper.WeatherErrors.from(e)
                CitySearchResult.Failure(
                    WeatherFailure(
                        mapped.reason,
                        "Nominatim: ${mapped.messageRu}"
                    )
                )
            }
        }
    }

    fun parseResponse(body: String): List<CityLocation> {
        val places = json.decodeFromString<List<NominatimPlace>>(body)
        val result = mutableListOf<CityLocation>()
        val seen = mutableSetOf<String>()
        for (place in places) {
            val latitude = place.lat.toDoubleOrNull() ?: continue
            val longitude = place.lon.toDoubleOrNull() ?: continue
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) continue
            val name = place.name?.takeIf { it.isNotBlank() }
                ?: place.displayName?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() }
                ?: continue
            val key = name.lowercase() + "|" + formatFixed(latitude, 4) + "|" + formatFixed(longitude, 4)
            if (!seen.add(key)) continue
            result.add(
                CityLocation(
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    country = place.address?.country?.takeIf { it.isNotBlank() } ?: "Мир",
                    region = place.address?.state?.takeIf { it.isNotBlank() }
                        ?: place.address?.region?.takeIf { it.isNotBlank() }
                )
            )
        }
        return result
    }

    fun rateLimitDelayMs(now: Long, lastRequestAt: Long): Long =
        (MIN_REQUEST_INTERVAL_MS - (now - lastRequestAt)).coerceAtLeast(0L)

    companion object {
        internal const val MIN_REQUEST_INTERVAL_MS = 1_000L

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        // Числа читает WeatherTimeoutConfigTest (конфиг плагина нечитаем публично).
        const val CONNECT_TIMEOUT_MILLIS = 4_000L
        const val READ_TIMEOUT_MILLIS = 6_000L
        const val CALL_TIMEOUT_MILLIS = 8_000L

        fun defaultClient(): HttpClient = providerHttpClient(
            // Резерв должен успеть в общий бюджет поиска ~12 с.
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS,
            readTimeoutMillis = READ_TIMEOUT_MILLIS,
            callTimeoutMillis = CALL_TIMEOUT_MILLIS
        )
    }
}

@Serializable
internal data class NominatimPlace(
    val name: String? = null,
    @kotlinx.serialization.SerialName("display_name") val displayName: String? = null,
    val lat: String = "",
    val lon: String = "",
    val address: NominatimAddress? = null
)

@Serializable
internal data class NominatimAddress(
    val country: String? = null,
    val city: String? = null,
    val state: String? = null,
    val region: String? = null
)
