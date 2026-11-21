package com.koch.weather.data.mapper

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.ClothingAdvisor
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.HourlyItem
import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WmoHelper
import com.koch.weather.platform.formatDayLabelRu
import com.koch.weather.platform.nowMillis
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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
 *
 * SimpleDateFormat/Calendar заменены на kotlinx-datetime + platform-шов
 * formatDayLabelRu: локальная зона устройства, те же подписи дней
 * («Сегодня» / «Вс», «Пн», …), что и в Android-версии.
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
        val currentWindMps = current.windSpeed10m
            ?.times(windToMetersPerSecond(dto.currentUnits?.windSpeed10m))

        val hourlyList = buildHourlyList(
            dto = dto,
            currentTemp = currentTemp,
            currentCode = weatherCode,
            currentIcon = currentIcon,
            currentWind = currentWindMps
        )
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
                humidity = current.relativeHumidity2m,
                windSpeed = currentWindMps,
                weatherCode = weatherCode,
                iconType = currentIcon,
                clothingAdvice = ClothingAdvisor.adviceFor(
                    tempC = currentTemp,
                    weatherCode = weatherCode,
                    windSpeedMps = currentWindMps,
                    cityName = city.name,
                    dayOfYear = dayOfYear(lastUpdated)
                ),
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
        val hourlyPrecips = hourly?.precipitation.orEmpty()
        val hourlyWindFactor = windToMetersPerSecond(dto.hourlyUnits?.windSpeed10m)

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
                wind = currentWind,
                popKnown = hourlyPops.getOrNull(currentIndex) != null,
                // Осадки «сейчас» — из current.precipitation; нет поля → null.
                precipitationMm = current?.precipitation
                    ?: hourlyPrecips.getOrNull(currentIndex)
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
                    wind = hourlyWinds.getOrNull(i)?.times(hourlyWindFactor),
                    popKnown = hourlyPops.getOrNull(i) != null,
                    precipitationMm = hourlyPrecips.getOrNull(i)
                )
            )
        }
        return list
    }

    private fun buildDailyList(
        daily: com.koch.weather.data.model.DailyWeatherDto,
        currentCode: Int
    ): List<DailyItem> {
        val dayNames = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
        val dailyPops = daily.precipitationProbabilityMax.orEmpty()
        val dailyPrecips = daily.precipitationSum.orEmpty()
        val list = mutableListOf<DailyItem>()

        for (i in daily.time.indices) {
            val max = daily.temperature2mMax.getOrNull(i)?.roundToInt() ?: continue
            val min = daily.temperature2mMin.getOrNull(i)?.roundToInt() ?: continue
            val code = daily.weatherCode.getOrNull(i) ?: currentCode
            val label = if (i == 0) {
                "Сегодня"
            } else {
                try {
                    // Дата Open-Meteo — локальная дата локации; так же, как в Android,
                    // интерпретируем её в локальной зоне устройства (SimpleDateFormat
                    // без явной зоны) и берём короткое имя дня недели с заглавной.
                    val date = LocalDate.parse(daily.time[i])
                    formatDayLabelRu(
                        date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    )
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
                    popKnown = dailyPops.getOrNull(i) != null,
                    precipitationMm = dailyPrecips.getOrNull(i)
                )
            )
        }
        return list
    }

    /** День года из времени снимка — для детерминированного «совета на сегодня». */
    private fun dayOfYear(epochMillis: Long): Int {
        val millis = if (epochMillis > 0) epochMillis else nowMillis()
        return Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .dayOfYear
    }

    /** Аналог Calendar.add(DAY_OF_YEAR, offset): текущая локальная дата + offset дней. */
    private fun fallbackDayLabel(dayNames: List<String>, offset: Int): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val shifted = today.plus(offset, DateTimeUnit.DAY)
        // Calendar.DAY_OF_WEEK: вс=1 … сб=7 → индекс dayNames; у DayOfWeek пн=0 … вс=6.
        return dayNames[(shifted.dayOfWeek.ordinal + 1) % 7]
    }

    private fun isHourDay(hour: Int): Boolean = hour in 6..21

    /**
     * Open-Meteo может вернуть km/h (исторический дефолт), несмотря на запрос
     * `wind_speed_unit=ms`. Домен всегда хранит м/с.
     */
    private fun windToMetersPerSecond(unit: String?): Double {
        val normalized = unit?.trim()?.lowercase() ?: return 1.0
        return when {
            normalized.contains("km/h") || normalized.contains("kmh") -> 1.0 / 3.6
            normalized.contains("mph") -> 0.44704
            normalized.contains("kn") || normalized.contains("knot") -> 0.514444
            else -> 1.0 // m/s или неизвестно — значение уже в м/с
        }
    }

    private fun failure(message: String): WeatherMapResult.Failure =
        WeatherMapResult.Failure(WeatherFailure(WeatherErrorReason.INVALID_DATA, message))
}
