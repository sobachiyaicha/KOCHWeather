package com.koch.weather.data.api

import com.koch.weather.data.model.GeocodingResponse
import com.koch.weather.data.model.GeocodingResult
import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.platform.httpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Клиент Open-Meteo (прогноз + geocoding) на Ktor.
 *
 * Замена OkHttp: таймауты 6/10/12 с (01-APP-SPEC §7.1: connect/read/call) —
 * те же числа, что и в OkHttp-версии; keep-alive даёт сам движок (OkHttp/Darwin).
 * [httpClient] из AppContainer переиспользует общий пул соединений; если не
 * передан — создаётся собственный HttpClient с теми же таймаутами.
 *
 * User-Agent — дословно Android-версия ("KOCHWeather/1.0 (Android)"), на каждый запрос.
 * URL, query-параметры и разбор JSON не менялись.
 */
class OpenMeteoClient(
    connectTimeoutSeconds: Long = 6,
    readTimeoutSeconds: Long = 10,
    callTimeoutSeconds: Long = 12,
    httpClient: HttpClient? = null
) {
    private val client: HttpClient = httpClient ?: HttpClient(httpClientEngine()) {
        install(HttpTimeout) {
            connectTimeoutMillis = connectTimeoutSeconds * 1000
            socketTimeoutMillis = readTimeoutSeconds * 1000
            requestTimeoutMillis = callTimeoutSeconds * 1000
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Явные единицы: UI показывает °C, мм и м/с — не полагаемся на дефолты API. */
    fun buildForecastUrl(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum" +
            "&temperature_unit=celsius&precipitation_unit=mm&wind_speed_unit=ms" +
            "&timezone=auto&forecast_days=10"

    // Dispatchers.IO недоступен в commonMain (internal): корутина уводится на
    // Dispatchers.Default, сам сетевой I/O выполняют потоки движка (OkHttp/Darwin).
    suspend fun getForecast(lat: Double, lon: Double): Result<WeatherApiResponse> = withContext(Dispatchers.Default) {
        try {
            val url = buildForecastUrl(lat, lon)

            println("WeatherApp: Fetching forecast from: " + url)

            val response = client.get(url) {
                header("User-Agent", OPEN_METEO_USER_AGENT)
            }
            println("WeatherApp: OpenMeteo response code: " + response.status.value)
            if (!response.status.isSuccess()) {
                val err = "HTTP " + response.status.value
                println("WeatherApp: OpenMeteo failed with " + err)
                return@withContext Result.failure(OpenMeteoHttpException(response.status.value))
            }
            val body = response.bodyAsText()
            if (body.isEmpty()) {
                return@withContext Result.failure(OpenMeteoEmptyBodyException())
            }
            val parsed = json.decodeFromString<WeatherApiResponse>(body)
            println("WeatherApp: OpenMeteo parsed successfully for lat=" + lat + ", lon=" + lon)
            Result.success(parsed)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("WeatherApp: OpenMeteo exception: " + e.message)
            Result.failure(e)
        }
    }

    suspend fun searchCity(query: String): Result<List<GeocodingResult>> = withContext(Dispatchers.Default) {
        try {
            val encodedQuery = urlEncodeUtf8(query.trim())
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedQuery + "&count=10&language=ru&format=json"

            println("WeatherApp: Searching city: " + url)

            val response = client.get(url) {
                header("User-Agent", OPEN_METEO_USER_AGENT)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(OpenMeteoHttpException(response.status.value))
            }
            val body = response.bodyAsText()
            if (body.isEmpty()) {
                return@withContext Result.failure(OpenMeteoEmptyBodyException())
            }
            val parsed = json.decodeFromString<GeocodingResponse>(body)
            Result.success(parsed.results ?: emptyList())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("WeatherApp: City search exception: " + e.message)
            Result.failure(e)
        }
    }

    companion object {
        /** Дословно Android-версия заголовка Open-Meteo (WEATHER_HTTP_USER_AGENT для MET/OSM). */
        const val OPEN_METEO_USER_AGENT = "KOCHWeather/1.0 (Android)"
    }
}
