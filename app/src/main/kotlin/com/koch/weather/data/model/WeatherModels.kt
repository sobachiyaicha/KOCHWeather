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
    val trafficScore: Int? = null,
    val trafficAdvice: String = "Нет данных о пробках",
    val hourly: List<HourlyItem>,
    val daily: List<DailyItem>,
    val isOffline: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class CityWeatherSummary(
    val city: CityLocation,
    val currentTemp: Int? = null,
    val conditionDescription: String = "",
    val iconType: WeatherIconType = WeatherIconType.PARTLY_CLOUDY_DAY,
    val minTemp: Int? = null,
    val maxTemp: Int? = null,
    val trafficScore: Int? = null,
    val trafficAdvice: String = "",
    val isLoading: Boolean = false
)

enum class AppThemeMode(val titleRu: String) {
    BLUE_SKY("Голубое небо"),
    COPPER_SUNSET("Медный закат"),
    FOREST("Изумрудный лес"),
    MARS("Планета Марс"),
    CYBERPUNK("Неоновый киберпанк"),
    BLOODY_SUNSET("Кровавый закат"),
    AURORA_BOREALIS("Северное сияние")
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
    THEMES,
    LIST,
    SEARCH;

    companion object {
        val CITIES: BottomTab get() = LIST
    }
}

@Serializable
data class UmbrellaItem(
    val id: String,
    val name: String,
    val priceRu: String,
    val priceByn: String,
    val colorHex: Long,
    val isCompact: Boolean = true,
    val description: String = ""
)

object DefaultUmbrellas {
    val items = listOf(
        UmbrellaItem(
            id = "umb_sky_blue",
            name = "Зонт-автомат Sky Blue",
            priceRu = "649 ₽",
            priceByn = "29.99 BYN",
            colorHex = 0xFF2196F3,
            isCompact = true,
            description = "Компактный 3-сложения, ветрозащита 15 м/с"
        ),
        UmbrellaItem(
            id = "umb_classic_black",
            name = "Зонт-трость Classic Black",
            priceRu = "990 ₽",
            priceByn = "44.99 BYN",
            colorHex = 0xFF212121,
            isCompact = false,
            description = "Усиленный каркас из фибергласса, деревянная ручка"
        ),
        UmbrellaItem(
            id = "umb_copper_sunset",
            name = "Зонт Copper Sunset",
            priceRu = "790 ₽",
            priceByn = "36.50 BYN",
            colorHex = 0xFFD84315,
            isCompact = true,
            description = "Тефлоновое нано-покрытие, сохнет за 1 минуту"
        ),
        UmbrellaItem(
            id = "umb_dome_transparent",
            name = "Прозрачный зонт-купол",
            priceRu = "850 ₽",
            priceByn = "39.99 BYN",
            colorHex = 0xFF90CAF9,
            isCompact = false,
            description = "Глубокий купол, защита при сильном ветре"
        )
    )
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
    val availableThemes: Set<AppThemeMode> = setOf(
        AppThemeMode.BLUE_SKY,
        AppThemeMode.COPPER_SUNSET,
        AppThemeMode.FOREST,
        AppThemeMode.MARS
    ),
    val selectedSegment: ConditionSegment = ConditionSegment.WEATHER,
    val selectedTab: BottomTab = BottomTab.HOME,

    // Folding states
    val isHourlyExpanded: Boolean = false,

    // Cloud Lock Screen (15s timer / swipe gesture)
    val isCloudLocked: Boolean = false,
    val isCloudsCleared: Boolean = false,

    // Premium & Ads
    val isPremiumPurchased: Boolean = false,
    val showAds: Boolean = true,

    // AI Facts
    val aiFact: String = "Чтобы точно узнать погоду, нужно посмотреть в окно",

    // Services state (2x2 Grid + Modals)
    val lootboxState: LootboxState = LootboxState(),
    val taxiState: TaxiState = TaxiState(),
    val foodState: FoodState = FoodState(),
    val flightState: FlightState = FlightState(),

    // Rating widget (Phase 3)
    val rating: Float = 3.5f,
    val userRating: Int? = null,
    val showRatingThankYou: Boolean = false,

    // Umbrellas store (Phase 3)
    val isUmbrellaShopOpen: Boolean = false,
    val umbrellas: List<UmbrellaItem> = DefaultUmbrellas.items,

    // Satirical & interactive features
    val isGrassModalOpen: Boolean = false,
    val isGrassUploaded: Boolean = false,
    val isCongratsModalOpen: Boolean = false,

    // Fullscreen Screens: List of Cities & Search
    val citySummaries: List<CityWeatherSummary> = emptyList(),
    val isLoadingCitiesWeather: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<CityLocation> = emptyList(),
    val isSearching: Boolean = false
)
