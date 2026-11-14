package com.koch.weather.data.provider

import com.koch.weather.data.api.OpenMeteoClient
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.CitySearchResult

/** Последний резерв геокодинга — Open-Meteo geocoding (для сетей, где он доступен). */
class OpenMeteoGeocodingProvider(
    private val client: OpenMeteoClient = OpenMeteoClient()
) : GeocodingProvider {

    override val source: GeocodingSource = GeocodingSource.OPEN_METEO

    override suspend fun search(query: String): CitySearchResult {
        val result = client.searchCity(query)
        return if (result.isSuccess) {
            CitySearchResult.Success(
                result.getOrThrow().map {
                    CityLocation(
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        country = it.country ?: "Мир",
                        region = it.admin1?.takeIf { region -> region.isNotBlank() }
                    )
                }
            )
        } else {
            CitySearchResult.Failure(WeatherErrors.from(result.exceptionOrNull()))
        }
    }
}
