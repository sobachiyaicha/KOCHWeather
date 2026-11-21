package com.koch.weather.data.provider

import com.koch.weather.data.api.OpenMeteoClient
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherSource
import com.koch.weather.platform.nowMillis

/**
 * Резервный источник для сетей, где Open-Meteo доступен. URL не меняется.
 * При timeout — один повтор с увеличенным read-таймаутом (connect 10s/read 25s).
 */
class OpenMeteoProvider(
    private val client: OpenMeteoClient = OpenMeteoClient(),
    private val retryClient: OpenMeteoClient = OpenMeteoClient(10, 25, 35),
    private val clock: () -> Long = { nowMillis() }
) : WeatherProvider {

    override val source: WeatherSource = WeatherSource.OPEN_METEO

    override suspend fun fetchForecast(
        latitude: Double,
        longitude: Double
    ): ProviderFetchResult {
        val first = client.getForecast(latitude, longitude)
        if (first.isSuccess) {
            return ProviderFetchResult.Success(first.getOrThrow(), clock())
        }
        val failure = WeatherErrors.from(first.exceptionOrNull())
        if (failure.reason == WeatherErrorReason.TIMEOUT) {
            val retry = retryClient.getForecast(latitude, longitude)
            if (retry.isSuccess) {
                return ProviderFetchResult.Success(retry.getOrThrow(), clock())
            }
            return ProviderFetchResult.Failure(WeatherErrors.from(retry.exceptionOrNull()))
        }
        return ProviderFetchResult.Failure(failure)
    }
}
