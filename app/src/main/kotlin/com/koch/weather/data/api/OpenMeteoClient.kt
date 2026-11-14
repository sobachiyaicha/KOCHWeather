package com.koch.weather.data.api

import android.util.Log
import com.koch.weather.data.model.GeocodingResponse
import com.koch.weather.data.model.GeocodingResult
import com.koch.weather.data.model.WeatherApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OpenMeteoClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun getForecast(lat: Double, lon: Double): Result<WeatherApiResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
                    "&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&timezone=auto&forecast_days=10"

            Log.d("WeatherApp", "Fetching forecast from: " + url)

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "KOCHWeather/1.0 (Android)")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d("WeatherApp", "OpenMeteo response code: " + response.code)
                if (!response.isSuccessful) {
                    val err = "HTTP " + response.code
                    Log.e("WeatherApp", "OpenMeteo failed with " + err)
                    return@withContext Result.failure(OpenMeteoHttpException(response.code))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(OpenMeteoEmptyBodyException())
                val parsed = json.decodeFromString<WeatherApiResponse>(body)
                Log.d("WeatherApp", "OpenMeteo parsed successfully for lat=" + lat + ", lon=" + lon)
                Result.success(parsed)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("WeatherApp", "OpenMeteo exception: " + e.message, e)
            Result.failure(e)
        }
    }

    suspend fun searchCity(query: String): Result<List<GeocodingResult>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedQuery + "&count=10&language=ru&format=json"

            Log.d("WeatherApp", "Searching city: " + url)

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "KOCHWeather/1.0 (Android)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(OpenMeteoHttpException(response.code))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(OpenMeteoEmptyBodyException())
                val parsed = json.decodeFromString<GeocodingResponse>(body)
                Result.success(parsed.results ?: emptyList())
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("WeatherApp", "City search exception: " + e.message, e)
            Result.failure(e)
        }
    }
}
