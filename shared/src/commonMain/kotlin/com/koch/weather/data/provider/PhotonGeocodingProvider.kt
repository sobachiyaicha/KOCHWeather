package com.koch.weather.data.provider

import com.koch.weather.data.api.formatFixed
import com.koch.weather.data.api.urlEncodeUtf8
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.CitySearchResult
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Photon (photon.komoot.io) — основной геокодер.
 *
 * ВАЖНО: параметр lang=ru Photon возвращает HTTP 400 (поддерживаются не все
 * языки), поэтому запрос идёт без lang: Photon отдаёт локальные имена OSM,
 * для русских городов это русские названия. accept-language=ru обеспечивает
 * резервный Nominatim.
 */
class PhotonGeocodingProvider(
    private val client: io.ktor.client.HttpClient = defaultClient()
) : GeocodingProvider {

    override val source: GeocodingSource = GeocodingSource.PHOTON

    override suspend fun search(query: String): CitySearchResult = withContext(Dispatchers.Default) {
        val encoded = urlEncodeUtf8(query.trim())
        val url = "https://photon.komoot.io/api/?q=$encoded&limit=5"
        try {
            val response = client.get(url) {
                header("User-Agent", WEATHER_HTTP_USER_AGENT)
                header("Accept", "application/json")
            }
            if (!response.status.isSuccess()) {
                return@withContext CitySearchResult.Failure(
                    WeatherFailure(
                        WeatherErrorReason.HTTP_ERROR,
                        "Photon вернул ошибку (HTTP ${response.status.value})."
                    )
                )
            }
            val body = response.bodyAsText()
            if (body.isEmpty()) {
                return@withContext CitySearchResult.Failure(
                    WeatherFailure(WeatherErrorReason.PARSE_ERROR, "Photon вернул пустой ответ.")
                )
            }
            val cities = try {
                parseResponse(body)
            } catch (e: Exception) {
                return@withContext CitySearchResult.Failure(
                    WeatherFailure(
                        WeatherErrorReason.PARSE_ERROR,
                        "Photon: не удалось разобрать ответ (${e.message})."
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
                    "Photon: ${mapped.messageRu}"
                )
            )
        }
    }

    fun parseResponse(body: String): List<CityLocation> {
        val response = json.decodeFromString<PhotonResponse>(body)
        val result = mutableListOf<CityLocation>()
        val seen = mutableSetOf<String>()
        for (feature in response.features) {
            val coordinates = feature.geometry?.coordinates.orEmpty()
            if (coordinates.size < 2) continue
            val longitude = coordinates[0]
            val latitude = coordinates[1]
            if (!latitude.isFinite() || !longitude.isFinite()) continue
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) continue
            val name = feature.properties.name?.takeIf { it.isNotBlank() }
                ?: feature.properties.city?.takeIf { it.isNotBlank() }
                ?: continue
            val key = name.lowercase() + "|" + formatFixed(latitude, 4) + "|" + formatFixed(longitude, 4)
            if (!seen.add(key)) continue
            result.add(
                CityLocation(
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    country = feature.properties.country?.takeIf { it.isNotBlank() } ?: "Мир",
                    region = feature.properties.state?.takeIf { it.isNotBlank() }
                )
            )
        }
        return result
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        // Числа читает WeatherTimeoutConfigTest (конфиг плагина нечитаем публично).
        const val CONNECT_TIMEOUT_MILLIS = 4_000L
        const val READ_TIMEOUT_MILLIS = 4_000L
        const val CALL_TIMEOUT_MILLIS = 6_000L

        fun defaultClient(): io.ktor.client.HttpClient = providerHttpClient(
            // Быстрый каскад: Photon не должен задерживать Nominatim дольше 4 с.
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS,
            readTimeoutMillis = READ_TIMEOUT_MILLIS,
            callTimeoutMillis = CALL_TIMEOUT_MILLIS
        )
    }
}

@Serializable
internal data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList()
)

@Serializable
internal data class PhotonFeature(
    val properties: PhotonProperties = PhotonProperties(),
    val geometry: PhotonGeometry? = null
)

@Serializable
internal data class PhotonProperties(
    val name: String? = null,
    val city: String? = null,
    val country: String? = null,
    val state: String? = null,
    @SerialName("osm_value") val osmValue: String? = null
)

@Serializable
internal data class PhotonGeometry(
    val coordinates: List<Double> = emptyList()
)
