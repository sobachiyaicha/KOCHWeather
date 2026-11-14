package com.koch.weather.ui.main

import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.WeatherDataState
import com.koch.weather.data.model.WeatherLoadResult
import com.koch.weather.data.model.WeatherSource

/**
 * Чистое применение результата загрузки к UI state. Отдельный объект нужен,
 * чтобы R19-переходы (CACHED → LIVE, offline-баннер) покрывались JVM-тестами.
 */
object WeatherUiReducer {

    fun apply(
        state: MainScreenUiState,
        result: WeatherLoadResult,
        isConnected: Boolean
    ): MainScreenUiState = when (result) {
        is WeatherLoadResult.Live -> state.copy(
            weather = result.data,
            weatherState = WeatherDataState.LIVE,
            isOffline = false,
            isWeatherUnavailable = false,
            weatherErrorMessage = null,
            weatherLastUpdated = result.data.lastUpdated,
            weatherSource = result.source,
            weatherAttributionRu = result.source.attributionRu,
            weatherSourceNote = result.fallbackNote,
            isLoading = false,
            isRefreshing = false
        )

        is WeatherLoadResult.Cached -> state.copy(
            weather = result.data,
            // R19: кэш, даже свежий по политике 30 мин/3 ч, остаётся CACHED.
            weatherState = WeatherDataState.CACHED,
            isOffline = true,
            isWeatherUnavailable = false,
            weatherErrorMessage = result.reason?.messageRu
                ?: "Нет связи. Показан сохранённый прогноз.",
            weatherLastUpdated = result.data.lastUpdated,
            weatherSource = result.source,
            weatherAttributionRu = cachedAttribution(result.source),
            weatherSourceNote = result.reason?.messageRu,
            isLoading = false,
            isRefreshing = false
        )

        is WeatherLoadResult.Unavailable -> {
            val failure = if (!isConnected) WeatherErrors.noNetwork() else result.failure
            // Уже показанный реальный прогноз не удаляем: помечаем офлайновым.
            val keptWeather = state.weather?.copy(
                isOffline = true,
                dataSourceLabel = null
            )
            state.copy(
                weather = keptWeather,
                weatherState = if (keptWeather != null) {
                    WeatherDataState.CACHED
                } else {
                    WeatherDataState.UNAVAILABLE
                },
                isOffline = true,
                isWeatherUnavailable = keptWeather == null,
                weatherErrorMessage = failure.messageRu,
                weatherLastUpdated = keptWeather?.lastUpdated,
                weatherSource = if (keptWeather != null) {
                    state.weatherSource
                } else {
                    WeatherSource.NONE
                },
                weatherAttributionRu = if (keptWeather != null) {
                    cachedAttribution(state.weatherSource)
                } else {
                    WeatherSource.NONE.attributionRu
                },
                weatherSourceNote = failure.messageRu,
                isLoading = false,
                isRefreshing = false
            )
        }
    }

    fun cachedAttribution(source: WeatherSource): String = when (source) {
        WeatherSource.CACHE, WeatherSource.NONE -> "Данные: сохранённый прогноз"
        else -> "Данные: ${source.displayNameRu} (сохранённый прогноз)"
    }
}
