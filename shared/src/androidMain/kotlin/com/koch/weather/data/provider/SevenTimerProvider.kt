package com.koch.weather.data.provider

import com.koch.weather.data.api.executeAsync
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Резервный источник из РФ без VPN: 7timer civil API.
 * Запускается только после того, как гонка MET/Open-Meteo вернула AllFailed.
 * connect 4s / read 6s / call 8s, собственный User-Agent.
 */
class SevenTimerProvider(
    private val client: OkHttpClient = defaultClient(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val timeZone: () -> TimeZone = { TimeZone.getDefault() }
) : WeatherProvider {

    override val source: WeatherSource = WeatherSource.SEVEN_TIMER

    override suspend fun fetchForecast(
        latitude: Double,
        longitude: Double
    ): ProviderFetchResult = withContext(Dispatchers.IO) {
        val url = "https://www.7timer.info/bin/api.pl" +
            "?lon=${format(longitude)}&lat=${format(latitude)}&product=civil&output=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", WEATHER_HTTP_USER_AGENT)
            .header("Accept", "application/json")
            .build()
        try {
            client.executeAsync(request).use { response ->
                if (!response.isSuccessful) {
                    return@withContext ProviderFetchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.HTTP_ERROR,
                            "7timer вернул ошибку (HTTP ${response.code})."
                        )
                    )
                }
                val body = response.body?.string()
                    ?: return@withContext ProviderFetchResult.Failure(
                        WeatherFailure(WeatherErrorReason.PARSE_ERROR, "7timer вернул пустой ответ.")
                    )
                val now = clock()
                val parsed = try {
                    SevenTimerForecastParser.parse(
                        body = body,
                        nowMillis = now,
                        latitude = latitude,
                        longitude = longitude,
                        timeZone = timeZone()
                    )
                } catch (e: SevenTimerParseException) {
                    return@withContext ProviderFetchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.PARSE_ERROR,
                            e.message ?: "7timer: ошибка разбора."
                        )
                    )
                }
                ProviderFetchResult.Success(response = parsed, fetchedAt = now)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val mapped = WeatherErrors.from(e)
            ProviderFetchResult.Failure(
                WeatherFailure(mapped.reason, "7timer: ${mapped.messageRu}")
            )
        }
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.4f", value)

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
