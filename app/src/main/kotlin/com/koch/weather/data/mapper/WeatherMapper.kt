package com.koch.weather.data.mapper

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.HourlyItem
import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WmoHelper
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

sealed interface WeatherMapResult {
    data class Success(val data: WeatherDomainData) : WeatherMapResult
    data class Failure(val failure: WeatherFailure) : WeatherMapResult
}

/**
 * Чистый маппер ответа Open-Meteo в доменную модель.
 *
 * Принцип: отсутствующие в ответе данные НЕ заменяются выдуманными. Если нет
 * текущей температуры/кода погоды или суточного прогноза — возвращается
 * Failure, и UI показывает «Нет данных». Мок-почасовок и мок-дней больше нет.
 */
object WeatherMapper {

    fun map(
        city: CityLocation,
        dto: WeatherApiResponse,
        isOffline: Boolean,
        lastUpdated: Long,
        trafficScore: Int?,
        trafficAdvice: String = "Нет данных о пробках",
        dataSourceLabel: String? = null
    ): WeatherMapResult {
        val current = dto.current
            ?: return failure("В ответе сервера нет текущей погоды.")
        val currentTemp = current.temperature2m?.roundToInt()
            ?: return failure("В ответе сервера нет текущей температуры.")
        val weatherCode = current.weatherCode
            ?: return failure("В ответе сервера нет кода погоды.")

        val daily = dto.daily
            ?: return failure("В ответе сервера нет суточного прогноза.")
        val dailyTimes = daily.time
        val dailyMaxs = daily.temperature2mMax
        val dailyMins = daily.temperature2mMin
        if (dailyTimes.isEmpty() || dailyMaxs.isEmpty() || dailyMins.isEmpty()) {
            return failure("В ответе сервера нет суточного прогноза.")
        }
        val todayMax = dailyMaxs.getOrNull(0)?.roundToInt()
            ?: return failure("В ответе сервера нет максимальной температуры.")
        val todayMin = dailyMins.getOrNull(0)?.roundToInt()
            ?: return failure("В ответе сервера нет минимальной температуры.")

        val isDay = (current.isDay ?: 1) == 1
        val description = WmoHelper.getDescriptionRu(weatherCode)
        val currentIcon = WmoHelper.getIconType(weatherCode, isDay)

        val hourlyList = buildHourlyList(dto, currentTemp, weatherCode, currentIcon, current.windSpeed10m)
        val dailyList = buildDailyList(daily, weatherCode)
        if (dailyList.isEmpty()) {
            return failure("В ответе сервера нет ни одного полного дня прогноза.")
        }

        return WeatherMapResult.Success(
            WeatherDomainData(
                cityName = city.name,
                currentTemp = currentTemp,
                conditionDescription = description,
                minTemp = todayMin,
                maxTemp = todayMax,
                humidity = current.relativeHumidity2m ?: 0,
                windSpeed = current.windSpeed10m ?: 0.0,
                weatherCode = weatherCode,
                iconType = currentIcon,
                clothingAdvice = WmoHelper.getClothingAdvice(currentTemp, weatherCode),
                trafficScore = trafficScore,
                trafficAdvice = trafficAdvice,
                hourly = hourlyList,
                daily = dailyList,
                isOffline = isOffline,
                lastUpdated = lastUpdated,
                dataSourceLabel = dataSourceLabel
            )
        )
    }

    private fun buildHourlyList(
        dto: WeatherApiResponse,
        currentTemp: Int,
        currentCode: Int,
        currentIcon: com.koch.weather.data.model.WeatherIconType,
        currentWind: Double?
    ): List<HourlyItem> {
        val hourly = dto.hourly
        val hourlyTimes = hourly?.time.orEmpty()
        val hourlyTemps = hourly?.temperature2m.orEmpty()
        val hourlyCodes = hourly?.weatherCode.orEmpty()
        val hourlyPops = hourly?.precipitationProbability.orEmpty()
        val hourlyWinds = hourly?.windSpeed10m.orEmpty()

        val current = dto.current
        val currentHourPrefix = current?.time.orEmpty().take(13)
        var currentIndex = if (currentHourPrefix.isNotEmpty()) {
            hourlyTimes.indexOfFirst { it.startsWith(currentHourPrefix) }
        } else {
            0
        }
        if (currentIndex < 0) currentIndex = 0

        val list = mutableListOf(
            HourlyItem(
                hourLabel = "Сейчас",
                temp = currentTemp,
                weatherCode = currentCode,
                iconType = currentIcon,
                pop = hourlyPops.getOrNull(currentIndex) ?: 0,
                wind = currentWind ?: 0.0,
                popKnown = hourlyPops.getOrNull(currentIndex) != null
            )
        )

        val startIndex = currentIndex + 1
        val endIndex = minOf(startIndex + 24, hourlyTimes.size)
        for (i in startIndex until endIndex) {
            val hour = hourlyTimes[i].substringAfter("T", "").take(2).toIntOrNull() ?: continue
            // Нет реальной температуры на этот час — не выдумываем её.
            val temp = hourlyTemps.getOrNull(i)?.roundToInt() ?: continue
            val code = hourlyCodes.getOrNull(i) ?: currentCode
            list.add(
                HourlyItem(
                    hourLabel = hourlyTimes[i].substringAfter("T", "").take(2),
                    temp = temp,
                    weatherCode = code,
                    iconType = WmoHelper.getIconType(code, isHourDay(hour)),
                    pop = hourlyPops.getOrNull(i) ?: 0,
                    wind = hourlyWinds.getOrNull(i) ?: 0.0,
                    popKnown = hourlyPops.getOrNull(i) != null
                )
            )
        }
        return list
    }

    private fun buildDailyList(
        daily: com.koch.weather.data.model.DailyWeatherDto,
        currentCode: Int
    ): List<DailyItem> {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EE", Locale.forLanguageTag("ru"))
        val dayNames = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
        val dailyPops = daily.precipitationProbabilityMax.orEmpty()
        val list = mutableListOf<DailyItem>()

        for (i in daily.time.indices) {
            val max = daily.temperature2mMax.getOrNull(i)?.roundToInt() ?: continue
            val min = daily.temperature2mMin.getOrNull(i)?.roundToInt() ?: continue
            val code = daily.weatherCode.getOrNull(i) ?: currentCode
            val label = if (i == 0) {
                "Сегодня"
            } else {
                try {
                    val date = inputFormat.parse(daily.time[i])
                    if (date != null) {
                        dayFormat.format(date).replaceFirstChar { it.uppercase() }
                    } else {
                        fallbackDayLabel(dayNames, i)
                    }
                } catch (_: Exception) {
                    fallbackDayLabel(dayNames, i)
                }
            }
            list.add(
                DailyItem(
                    dayLabel = label,
                    weatherCode = code,
                    iconType = WmoHelper.getIconType(code, isDay = true),
                    pop = dailyPops.getOrNull(i) ?: 0,
                    minTemp = min,
                    maxTemp = max,
                    popKnown = dailyPops.getOrNull(i) != null
                )
            )
        }
        return list
    }

    private fun fallbackDayLabel(dayNames: List<String>, offset: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, offset)
        return dayNames[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
    }

    private fun isHourDay(hour: Int): Boolean = hour in 6..21

    private fun failure(message: String): WeatherMapResult.Failure =
        WeatherMapResult.Failure(WeatherFailure(WeatherErrorReason.INVALID_DATA, message))
}
