package com.koch.weather.data.provider

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.CitySearchResult
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Резервный геокодер OpenStreetMap: Nominatim.
 *
 * Политика OSM: не чаще 1 запроса в секунду, обязательный User-Agent.
 * accept-language=ru даёт русские названия.
 */
class NominatimGeocodingProvider(
    private val client: OkHttpClient = defaultClient(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val delayFn: suspend (Long) -> Unit = { if (it > 0) delay(it) }
) : GeocodingProvider {

    override val source: GeocodingSource = GeocodingSource.NOMINATIM

    private val mutex = Mutex()
    private var lastRequestAt = 0L

    override suspend fun search(query: String): CitySearchResult = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search" +
            "?format=jsonv2&addressdetails=1&q=$encoded&limit=5&accept-language=ru"
        mutex.withLock {
            val now = clock()
            delayFn(rateLimitDelayMs(now, lastRequestAt))
            lastRequestAt = clock()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", WEATHER_HTTP_USER_AGENT)
                .header("Accept", "application/json")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withLock CitySearchResult.Failure(
                            WeatherFailure(
                                WeatherErrorReason.HTTP_ERROR,
                                "Nominatim вернул ошибку (HTTP ${response.code})."
                            )
                        )
                    }
                    val body = response.body?.string()
                        ?: return@withLock CitySearchResult.Failure(
                            WeatherFailure(WeatherErrorReason.PARSE_ERROR, "Nominatim вернул пустой ответ.")
                        )
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
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                CitySearchResult.Failure(
                    WeatherFailure(
                        WeatherErrorReason.NO_NETWORK,
                        "Nominatim недоступен: ${e.message ?: "ошибка сети"}."
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
            val key = String.format(Locale.US, "%s|%.4f|%.4f", name.lowercase(), latitude, longitude)
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

    internal fun rateLimitDelayMs(now: Long, lastRequestAt: Long): Long =
        (MIN_REQUEST_INTERVAL_MS - (now - lastRequestAt)).coerceAtLeast(0L)

    companion object {
        internal const val MIN_REQUEST_INTERVAL_MS = 1_000L

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
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
