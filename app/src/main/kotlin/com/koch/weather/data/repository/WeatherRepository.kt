package com.koch.weather.data.repository

import android.content.Context
import com.koch.weather.data.api.OpenMeteoClient
import com.koch.weather.data.cache.WeatherCache
import com.koch.weather.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class WeatherRepository @JvmOverloads constructor(
    context: Context,
    private val trafficRepository: TrafficRepository = TrafficRepository()
) {
    private val client = OpenMeteoClient()
    private val cache = WeatherCache(context.applicationContext)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // In-memory cache for ultra-fast (0ms) tab and city switching
    private val memoryCache = ConcurrentHashMap<String, WeatherDomainData>()

    val defaultCities = listOf(
        CityLocation("Санкт-Петербург", 59.9386, 30.3141, "Россия"),
        CityLocation("Москва", 55.7558, 37.6173, "Россия"),
        CityLocation("Минск", 53.9006, 27.5590, "Беларусь"),
        CityLocation("Дубай", 25.2048, 55.2708, "ОАЭ")
    )

    init {
        // Pre-fill memory cache with rich baseline data for instant render
        for (city in defaultCities) {
            memoryCache[city.name] = getFallbackData(city.name, isOffline = false)
        }
    }

    fun getCachedWeatherFast(city: CityLocation): WeatherDomainData {
        return memoryCache[city.name] ?: getFallbackData(city.name, isOffline = false)
    }

    suspend fun getInitialCities(): List<CityLocation> = withContext(Dispatchers.IO) {
        val saved = cache.getSavedCities()
        if (!saved.isNullOrEmpty()) {
            // If the saved list was an older default that didn't have Moscow at index 1, adjust it
            if (saved.size >= 2 && saved[0].name == "Санкт-Петербург" && saved[1].name != "Москва") {
                val updated = mutableListOf<CityLocation>()
                updated.add(defaultCities[0]) // Санкт-Петербург
                updated.add(defaultCities[1]) // Москва
                for (c in saved) {
                    if (c.name != "Санкт-Петербург" && c.name != "Москва") {
                        updated.add(c)
                    }
                }
                cache.saveCitiesList(updated)
                updated
            } else {
                saved
            }
        } else {
            cache.saveCitiesList(defaultCities)
            defaultCities
        }
    }

    suspend fun getLastCity(): CityLocation? = withContext(Dispatchers.IO) {
        cache.getLastCity()
    }

    suspend fun saveCities(cities: List<CityLocation>) = withContext(Dispatchers.IO) {
        cache.saveCitiesList(cities)
    }

    suspend fun getWeatherData(city: CityLocation): WeatherDomainData = withContext(Dispatchers.IO) {
        val result = client.getForecast(city.latitude, city.longitude)
        if (result.isSuccess) {
            val dto = result.getOrThrow()
            val domain = mapToDomain(city.name, dto, isOffline = false)
            memoryCache[city.name] = domain
            // Persist to DataStore cache in background
            try {
                cache.saveForecast(city, json.encodeToString(dto))
            } catch (_: Exception) {}
            domain
        } else {
            // Read from DataStore cache for this city
            val cachedJson = cache.getForecastForCity(city.name)
            if (!cachedJson.isNullOrBlank()) {
                try {
                    val dto = json.decodeFromString<WeatherApiResponse>(cachedJson)
                    val domain = mapToDomain(city.name, dto, isOffline = true)
                    memoryCache[city.name] = domain
                    domain
                } catch (_: Exception) {
                    val fallback = getFallbackData(city.name, isOffline = true)
                    memoryCache[city.name] = fallback
                    fallback
                }
            } else {
                val fallback = getFallbackData(city.name, isOffline = true)
                memoryCache[city.name] = fallback
                fallback
            }
        }
    }

    suspend fun searchCities(query: String): List<CityLocation> = withContext(Dispatchers.IO) {
        val res = client.searchCity(query)
        if (res.isSuccess) {
            val list = res.getOrThrow()
            if (list.isNotEmpty()) {
                return@withContext list.map {
                    CityLocation(
                        name = it.name,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        country = it.country ?: it.admin1 ?: "Мир"
                    )
                }
            }
        }
        defaultCities.filter { it.name.contains(query, ignoreCase = true) }
    }

    private fun mapToDomain(cityName: String, dto: WeatherApiResponse, isOffline: Boolean): WeatherDomainData {
        val cur = dto.current ?: CurrentWeatherDto()
        val curTemp = cur.temperature2m.roundToInt()
        val wmoCode = cur.weatherCode
        val isDay = (cur.isDay ?: 1) == 1
        val desc = WmoHelper.getDescriptionRu(wmoCode)
        val iconType = WmoHelper.getIconType(wmoCode, isDay)

        val dailyDto = dto.daily
        val maxTemp = dailyDto?.temperature2mMax?.firstOrNull()?.roundToInt() ?: (curTemp + 2)
        val minTemp = dailyDto?.temperature2mMin?.firstOrNull()?.roundToInt() ?: (curTemp - 6)

        val clothingAdvice = WmoHelper.getClothingAdvice(curTemp, wmoCode)
        val trafficInfo = trafficRepository.getTraffic(city)
        val trafficScore = trafficInfo.score
        val trafficAdvice = trafficInfo.description

        // Hourly mapping
        val hourlyList = mutableListOf<HourlyItem>()
        val hourlyTimes = dto.hourly?.time ?: emptyList()
        val hourlyTemps = dto.hourly?.temperature2m ?: emptyList()
        val hourlyCodes = dto.hourly?.weatherCode ?: emptyList()
        val hourlyPops = dto.hourly?.precipitationProbability ?: emptyList()
        val hourlyWinds = dto.hourly?.windSpeed10m ?: emptyList()

        val curHourPrefix = cur.time.take(13)
        var currentIndex = hourlyTimes.indexOfFirst { it.startsWith(curHourPrefix) }
        if (currentIndex < 0) {
            currentIndex = 0
        }

        hourlyList.add(
            HourlyItem(
                hourLabel = "Сейчас",
                temp = curTemp,
                weatherCode = wmoCode,
                iconType = iconType,
                pop = hourlyPops.getOrNull(currentIndex) ?: 0,
                wind = cur.windSpeed10m ?: 4.0
            )
        )

        val startIdx = currentIndex + 1
        val endIdx = minOf(startIdx + 24, hourlyTimes.size)
        for (i in startIdx until endIdx) {
            val rawTime = hourlyTimes[i]
            val hourStr = rawTime.substringAfter("T").take(2)
            val code = hourlyCodes.getOrNull(i) ?: wmoCode
            val hourInt = hourStr.toIntOrNull() ?: 12
            val isHourDay = hourInt in 6..21
            hourlyList.add(
                HourlyItem(
                    hourLabel = hourStr,
                    temp = hourlyTemps.getOrNull(i)?.roundToInt() ?: curTemp,
                    weatherCode = code,
                    iconType = WmoHelper.getIconType(code, isHourDay),
                    pop = hourlyPops.getOrNull(i) ?: 0,
                    wind = hourlyWinds.getOrNull(i) ?: 3.5
                )
            )
        }

        if (hourlyList.size <= 1) {
            hourlyList.clear()
            hourlyList.addAll(getMockHourly(curTemp, wmoCode, iconType))
        }

        // Daily mapping
        val dailyList = mutableListOf<DailyItem>()
        val dailyTimes = dailyDto?.time ?: emptyList()
        val dailyCodes = dailyDto?.weatherCode ?: emptyList()
        val dailyMaxs = dailyDto?.temperature2mMax ?: emptyList()
        val dailyMins = dailyDto?.temperature2mMin ?: emptyList()
        val dailyPops = dailyDto?.precipitationProbabilityMax ?: emptyList()

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EE", Locale.forLanguageTag("ru"))
        val dayNames = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")

        for (i in dailyTimes.indices) {
            val label = if (i == 0) {
                "Сегодня"
            } else {
                try {
                    val date = inputFormat.parse(dailyTimes[i])
                    if (date != null) {
                        dayFormat.format(date).replaceFirstChar { it.uppercase() }
                    } else {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, i)
                        dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                    }
                } catch (_: Exception) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, i)
                    dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                }
            }

            val code = dailyCodes.getOrNull(i) ?: 2
            dailyList.add(
                DailyItem(
                    dayLabel = label,
                    weatherCode = code,
                    iconType = WmoHelper.getIconType(code, isDay = true),
                    pop = dailyPops.getOrNull(i) ?: 0,
                    minTemp = dailyMins.getOrNull(i)?.roundToInt() ?: (curTemp - 6),
                    maxTemp = dailyMaxs.getOrNull(i)?.roundToInt() ?: (curTemp + 2)
                )
            )
        }

        if (dailyList.isEmpty()) {
            dailyList.addAll(getMockDaily(curTemp))
        }

        return WeatherDomainData(
            cityName = cityName,
            currentTemp = curTemp,
            conditionDescription = desc,
            minTemp = minTemp,
            maxTemp = maxTemp,
            humidity = cur.relativeHumidity2m ?: 65,
            windSpeed = cur.windSpeed10m ?: 4.2,
            weatherCode = wmoCode,
            iconType = iconType,
            clothingAdvice = clothingAdvice,
            trafficScore = trafficScore,
            trafficAdvice = trafficAdvice,
            hourly = hourlyList,
            daily = dailyList,
            isOffline = isOffline
        )
    }

    private fun getFallbackData(cityName: String, isOffline: Boolean): WeatherDomainData {
        val isMinsk = cityName.contains("Минск", ignoreCase = true)
        val isSpb = cityName.contains("Санкт", ignoreCase = true) || cityName.contains("Петербург", ignoreCase = true)
        val isDubai = cityName.contains("Дубай", ignoreCase = true)
        val isMoscow = cityName.contains("Москва", ignoreCase = true)

        val temp = when {
            isMinsk -> 21
            isDubai -> 31
            isSpb -> 5
            isMoscow -> 7
            else -> 15
        }

        val minTemp = when {
            isMinsk -> 13
            isDubai -> 24
            isSpb -> 1
            isMoscow -> 2
            else -> 8
        }

        val maxTemp = when {
            isMinsk -> 22
            isDubai -> 34
            isSpb -> 7
            isMoscow -> 9
            else -> 18
        }

        val wmoCode = when {
            isDubai -> 0
            isMinsk -> 3
            isMoscow -> 2
            else -> 2
        }

        val desc = WmoHelper.getDescriptionRu(wmoCode)
        val iconType = WmoHelper.getIconType(wmoCode, isDay = true)

        return WeatherDomainData(
            cityName = cityName,
            currentTemp = temp,
            conditionDescription = desc,
            minTemp = minTemp,
            maxTemp = maxTemp,
            humidity = if (isDubai) 45 else 72,
            windSpeed = if (isDubai) 3.1 else 4.2,
            weatherCode = wmoCode,
            iconType = iconType,
            clothingAdvice = WmoHelper.getClothingAdvice(temp, wmoCode),
            trafficScore = null,
            trafficAdvice = "Нет данных о пробках",
            hourly = getMockHourly(temp, wmoCode, iconType),
            daily = getMockDaily(temp),
            isOffline = isOffline
        )
    }

    private fun getMockHourly(baseTemp: Int, baseCode: Int, baseIcon: WeatherIconType): List<HourlyItem> {
        val hours = listOf("Сейчас", "15", "16", "17", "18", "19", "20", "21", "22", "23", "00", "01")
        return hours.mapIndexed { index, label ->
            HourlyItem(
                hourLabel = label,
                temp = if (index == 0) baseTemp else baseTemp - (index / 4),
                weatherCode = baseCode,
                iconType = baseIcon,
                pop = if (index % 3 == 0) 10 else 0,
                wind = 3.5
            )
        }
    }

    private fun getMockDaily(baseTemp: Int): List<DailyItem> {
        return listOf(
            DailyItem("Сегодня", 2, WeatherIconType.PARTLY_CLOUDY_DAY, 0, baseTemp - 8, baseTemp + 1),
            DailyItem("Пн", 3, WeatherIconType.CLOUDY, 20, baseTemp - 8, baseTemp + 1),
            DailyItem("Вт", 2, WeatherIconType.PARTLY_CLOUDY_DAY, 0, baseTemp - 8, baseTemp + 1),
            DailyItem("Ср", 3, WeatherIconType.CLOUDY, 10, baseTemp - 8, baseTemp + 1),
            DailyItem("Чт", 2, WeatherIconType.PARTLY_CLOUDY_DAY, 0, baseTemp - 8, baseTemp + 1),
            DailyItem("Пт", 2, WeatherIconType.PARTLY_CLOUDY_DAY, 0, baseTemp - 7, baseTemp + 2),
            DailyItem("Сб", 1, WeatherIconType.CLEAR_DAY, 0, baseTemp - 6, baseTemp + 3),
            DailyItem("Вс", 2, WeatherIconType.PARTLY_CLOUDY_DAY, 5, baseTemp - 7, baseTemp + 2)
        )
    }
}
