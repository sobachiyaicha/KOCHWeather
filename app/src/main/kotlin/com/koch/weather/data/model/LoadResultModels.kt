package com.koch.weather.data.model

/** Состояние погодных данных в UI. */
enum class WeatherDataState {
    /** Первичная загрузка, данных ещё нет. */
    LOADING,

    /** Свежий прогноз из сети. */
    LIVE,

    /** Последний реальный прогноз из DataStore/памяти, помечен офлайновым. */
    CACHED,

    /** Данных нет: ни сети, ни кэша. UI показывает плейсхолдер и «Нет данных». */
    UNAVAILABLE
}

enum class WeatherErrorReason {
    NO_NETWORK,
    TIMEOUT,
    HTTP_ERROR,
    PARSE_ERROR,
    INVALID_DATA,
    CACHE_POLICY,
    UNKNOWN
}

data class WeatherFailure(
    val reason: WeatherErrorReason,
    val messageRu: String
)

/**
 * Откуда пришли погодные данные. UI показывает attribution и причину
 * failover-а (например, MET Norway недоступен → Open-Meteo).
 */
enum class WeatherSource(
    val id: String,
    val displayNameRu: String,
    val attributionRu: String
) {
    MET_NO("met_no", "MET Norway", "Данные: MET Norway (api.met.no)"),
    OPEN_METEO("open_meteo", "Open-Meteo", "Данные: Open-Meteo"),
    CACHE("cache", "Сохранённый прогноз", "Данные: сохранённый прогноз"),
    NONE("none", "Нет данных", "Данные: нет данных");

    companion object {
        fun fromId(id: String?): WeatherSource? =
            values().firstOrNull { it.id == id }
    }
}

data class CachedWeather(
    val data: WeatherDomainData,
    val source: WeatherSource = WeatherSource.CACHE
)

sealed interface WeatherLoadResult {
    data class Live(
        val data: WeatherDomainData,
        val source: WeatherSource,
        val fallbackNote: String? = null
    ) : WeatherLoadResult

    data class Cached(
        val data: WeatherDomainData,
        val source: WeatherSource = WeatherSource.CACHE,
        val reason: WeatherFailure? = null
    ) : WeatherLoadResult

    data class Unavailable(
        val failure: WeatherFailure,
        val attempts: List<WeatherFailure> = emptyList()
    ) : WeatherLoadResult
}

sealed interface CitySearchResult {
    data class Success(val cities: List<CityLocation>) : CitySearchResult
    data class Failure(val failure: WeatherFailure) : CitySearchResult
}
