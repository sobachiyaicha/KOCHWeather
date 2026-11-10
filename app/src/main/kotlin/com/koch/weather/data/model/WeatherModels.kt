package com.koch.weather.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherApiResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val current: CurrentWeatherDto? = null,
    val hourly: HourlyWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

@Serializable
data class CurrentWeatherDto(
    val time: String = "",
    @SerialName("temperature_2m") val temperature2m: Double = 21.0,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = 65,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = 20.0,
    @SerialName("is_day") val isDay: Int? = 1,
    val precipitation: Double? = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 2,
    @SerialName("wind_speed_10m") val windSpeed10m: Double? = 4.2
)

@Serializable
data class HourlyWeatherDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int>? = null,
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double>? = null
)

@Serializable
data class DailyWeatherDto(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null
)

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val id: Long = 0,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String? = null,
    val admin1: String? = null
)

@Serializable
data class CityLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "Россия"
)

enum class WeatherIconType {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY_DAY,
    PARTLY_CLOUDY_NIGHT,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    SNOW,
    THUNDERSTORM
}

data class HourlyItem(
    val hourLabel: String,
    val temp: Int,
    val weatherCode: Int,
    val iconType: WeatherIconType = WeatherIconType.PARTLY_CLOUDY_DAY,
    val pop: Int = 0,
    val wind: Double = 3.0
)

data class DailyItem(
    val dayLabel: String,
    val weatherCode: Int,
    val iconType: WeatherIconType = WeatherIconType.PARTLY_CLOUDY_DAY,
    val pop: Int = 0,
    val minTemp: Int,
    val maxTemp: Int
)

data class WeatherDomainData(
    val cityName: String,
    val currentTemp: Int,
    val conditionDescription: String,
    val minTemp: Int,
    val maxTemp: Int,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: Int,
    val iconType: WeatherIconType,
    val clothingAdvice: String,
    val trafficAdvice: Pair<Int, String>,
    val hourly: List<HourlyItem>,
    val daily: List<DailyItem>,
    val isOffline: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class AppThemeMode {
    BLUE_SKY,
    COPPER_SUNSET,
    FOREST,
    MARS
}

enum class ConditionSegment {
    WEATHER,
    RAIN,
    WIND,
    CLOTHES,
    TRAFFIC
}

enum class BottomTab {
    HOME,
    MAP,
    LIST,
    SEARCH
}

data class MainScreenUiState(
    val isLoading: Boolean = false,
    val weather: WeatherDomainData? = null,
    val cities: List<CityLocation> = emptyList(),
    val selectedCity: CityLocation? = null,
    val selectedCityIndex: Int = 0,
    val currentCityIndex: Int = 0,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,

    // UI state
    val currentTheme: AppThemeMode = AppThemeMode.BLUE_SKY,
    val selectedSegment: ConditionSegment = ConditionSegment.WEATHER,
    val selectedTab: BottomTab = BottomTab.HOME,

    // Satirical & interactive features
    val rating: Float = 3.5f,
    val userRating: Float = 0f,
    val isCloudsCleared: Boolean = false,
    val isGrassModalOpen: Boolean = false,
    val isGrassUploaded: Boolean = false,
    val isCongratsModalOpen: Boolean = false,

    // Search
    val searchQuery: String = "",
    val searchResults: List<CityLocation> = emptyList(),
    val isSearching: Boolean = false
)
