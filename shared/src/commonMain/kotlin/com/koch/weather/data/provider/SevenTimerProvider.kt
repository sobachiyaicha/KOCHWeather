package com.koch.weather.data.provider

import com.koch.weather.data.api.formatFixed
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherSource
import com.koch.weather.platform.nowMillis
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

/**
 * Резервный источник из РФ без VPN: 7timer civil API.
 * Запускается только после того, как гонка MET/Open-Meteo вернула AllFailed.
 * connect 4s / read 6s / call 8s, собственный User-Agent.
 */
class SevenTimerProvider(
    private val client: HttpClient = defaultClient(),
    private val clock: () -> Long = { nowMillis() },
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : WeatherProvider {

    override val source: WeatherSource = WeatherSource.SEVEN_TIMER

    override suspend fun fetchForecast(
        latitude: Double,
        longitude: Double
    ): ProviderFetchResult = withContext(Dispatchers.Default) {
        val url = "https://www.7timer.info/bin/api.pl" +
            "?lon=${format(longitude)}&lat=${format(latitude)}&product=civil&output=json"
        try {
            val response = client.get(url) {
                header("User-Agent", WEATHER_HTTP_USER_AGENT)
                header("Accept", "application/json")
            }
            if (!response.status.isSuccess()) {
                return@withContext ProviderFetchResult.Failure(
                    WeatherFailure(
                        WeatherErrorReason.HTTP_ERROR,
                        "7timer вернул ошибку (HTTP ${response.status.value})."
                    )
                )
            }
            val body = response.bodyAsText()
            if (body.isEmpty()) {
                return@withContext ProviderFetchResult.Failure(
                    WeatherFailure(WeatherErrorReason.PARSE_ERROR, "7timer вернул пустой ответ.")
                )
            }
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val mapped = WeatherErrors.from(e)
            ProviderFetchResult.Failure(
                WeatherFailure(mapped.reason, "7timer: ${mapped.messageRu}")
            )
        }
    }

    private fun format(value: Double): String = formatFixed(value, 4)

    companion object {
        // Числа читает WeatherTimeoutConfigTest (конфиг плагина нечитаем публично).
        const val CONNECT_TIMEOUT_MILLIS = 4_000L
        const val READ_TIMEOUT_MILLIS = 6_000L
        const val CALL_TIMEOUT_MILLIS = 8_000L

        fun defaultClient(): HttpClient = providerHttpClient(
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS,
            readTimeoutMillis = READ_TIMEOUT_MILLIS,
            callTimeoutMillis = CALL_TIMEOUT_MILLIS
        )
    }
}
