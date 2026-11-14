package com.koch.weather.data.provider

import com.koch.weather.data.api.OpenMeteoClient
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.WeatherSource

/**
 * Резервный источник для сетей, где Open-Meteo доступен. URL не меняется,
 * используется тот же клиент, что и раньше.
 */
class OpenMeteoProvider(
    private val client: OpenMeteoClient = OpenMeteoClient(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) : WeatherProvider {

    override val source: WeatherSource = WeatherSource.OPEN_METEO

    override suspend fun fetchForecast(
        latitude: Double,
        longitude: Double
    ): ProviderFetchResult {
        val result = client.getForecast(latitude, longitude)
        return if (result.isSuccess) {
            ProviderFetchResult.Success(
                response = result.getOrThrow(),
                fetchedAt = clock()
            )
        } else {
            ProviderFetchResult.Failure(WeatherErrors.from(result.exceptionOrNull()))
        }
    }
}
