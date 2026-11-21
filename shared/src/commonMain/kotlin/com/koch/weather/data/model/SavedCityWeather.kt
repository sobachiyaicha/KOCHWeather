package com.koch.weather.data.model

import androidx.compose.runtime.Immutable

/**
 * Сохранённый город для экрана «Список» (R13).
 *
 * Если реального прогноза нет (ни сети, ни кэша), [tempC] и [conditionRu]
 * остаются null, а UI обязан показать «Нет данных». Никаких синтетических
 * температур здесь не появляется.
 */
@Immutable
data class SavedCityWeather(
    val city: CityLocation,
    val tempC: Int? = null,
    val conditionRu: String? = null,
    val minTempC: Int? = null,
    val maxTempC: Int? = null,
    val isOffline: Boolean = true,
    val updatedAt: Long? = null
) {
    val name: String get() = city.name
    val latitude: Double get() = city.latitude
    val longitude: Double get() = city.longitude
    val country: String get() = city.country
    val region: String? get() = city.region
    val hasData: Boolean get() = tempC != null

    /** Готовая строка для UI: либо реальное состояние, либо честное «Нет данных». */
    val conditionOrNoDataRu: String get() = conditionRu ?: "Нет данных"

    companion object {
        fun from(city: CityLocation, weather: WeatherDomainData?): SavedCityWeather {
            if (weather == null) {
                return SavedCityWeather(city = city, isOffline = true)
            }
            return SavedCityWeather(
                city = city,
                tempC = weather.currentTemp,
                conditionRu = weather.conditionDescription.takeIf { it.isNotBlank() },
                minTempC = weather.minTemp,
                maxTempC = weather.maxTemp,
                isOffline = weather.isOffline,
                updatedAt = weather.lastUpdated
            )
        }
    }
}
