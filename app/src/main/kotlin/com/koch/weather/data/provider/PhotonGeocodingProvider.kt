package com.koch.weather.data.provider

import com.koch.weather.data.api.executeAsync
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.CitySearchResult
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Photon (photon.komoot.io) — основной геокодер.
 *
 * ВАЖНО: параметр lang=ru Photon возвращает HTTP 400 (поддерживаются не все
 * языки), поэтому запрос идёт без lang: Photon отдаёт локальные имена OSM,
 * для русских городов это русские названия. accept-language=ru обеспечивает
 * резервный Nominatim.
 */
class PhotonGeocodingProvider(
    private val client: OkHttpClient = defaultClient()
) : GeocodingProvider {

    override val source: GeocodingSource = GeocodingSource.PHOTON

    override suspend fun search(query: String): CitySearchResult = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://photon.komoot.io/api/?q=$encoded&limit=5"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", WEATHER_HTTP_USER_AGENT)
            .header("Accept", "application/json")
            .build()
        try {
            client.executeAsync(request).use { response ->
                if (!response.isSuccessful) {
                    return@withContext CitySearchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.HTTP_ERROR,
                            "Photon вернул ошибку (HTTP ${response.code})."
                        )
                    )
                }
                val body = response.body?.string()
                    ?: return@withContext CitySearchResult.Failure(
                        WeatherFailure(WeatherErrorReason.PARSE_ERROR, "Photon вернул пустой ответ.")
                    )
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
            }
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
            val key = String.format(Locale.US, "%s|%.4f|%.4f", name.lowercase(), latitude, longitude)
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

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            // Быстрый каскад: Photon не должен задерживать Nominatim дольше 4 с.
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
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
