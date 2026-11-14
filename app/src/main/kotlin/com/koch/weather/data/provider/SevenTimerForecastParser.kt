package com.koch.weather.data.provider

import com.koch.weather.data.model.CurrentWeatherDto
import com.koch.weather.data.model.DailyWeatherDto
import com.koch.weather.data.model.HourlyWeatherDto
import com.koch.weather.data.model.WeatherApiResponse
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class SevenTimerParseException(message: String) : Exception(message)

/**
 * 7timer civil: dataseries с шагом 3 часа от init (UTC). Собираем:
 * - current — ближайшая точка;
 * - hourly — 24 часа, значение берём у ближайшей 3-часовой точки;
 * - daily — min/max и доминирующий weather по локальной дате.
 */
object SevenTimerForecastParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private data class Point(
        val timeMs: Long,
        val tempC: Double,
        val wmo: Int,
        val humidity: Int?,
        val windMps: Double?,
        val weatherRaw: String
    )

    fun parse(
        body: String,
        nowMillis: Long,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone = TimeZone.getDefault()
    ): WeatherApiResponse {
        val response = try {
            json.decodeFromString<SevenTimerResponse>(body.removePrefix("\uFEFF").trim())
        } catch (e: Exception) {
            throw SevenTimerParseException("7timer: не удалось разобрать JSON (${e.message})")
        }
        val initMs = parseInit(response.init)
            ?: throw SevenTimerParseException("7timer: некорректный init '${response.init}'")
        if (response.dataseries.isEmpty()) {
            throw SevenTimerParseException("7timer: пустой dataseries")
        }

        val localDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
        val localHour = SimpleDateFormat("HH", Locale.US).apply { this.timeZone = timeZone }
        val localCurrentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).apply {
            this.timeZone = timeZone
        }
        val localHourlyTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
            this.timeZone = timeZone
        }

        val points = response.dataseries.mapNotNull { item ->
            val temp = item.temp2m ?: return@mapNotNull null
            val rawWeather = item.weather ?: return@mapNotNull null
            val wmo = SevenTimerSymbolMapper.toWmo(rawWeather) ?: return@mapNotNull null
            val timeMs = initMs + item.timepoint.coerceAtLeast(0) * 3_600_000L
            Point(
                timeMs = timeMs,
                tempC = temp.toDouble(),
                wmo = wmo,
                humidity = item.rh2m?.removeSuffix("%")?.trim()?.toIntOrNull(),
                windMps = SevenTimerSymbolMapper.beaufortToMetersPerSecond(item.wind10m?.speed),
                weatherRaw = rawWeather
            )
        }.sortedBy { it.timeMs }

        if (points.isEmpty()) {
            throw SevenTimerParseException("7timer: нет точек с температурой/weather")
        }

        val currentPoint = points.minByOrNull { abs(it.timeMs - nowMillis) } ?: points.first()
        val currentLocalHour = localHour.format(Date(currentPoint.timeMs)).toIntOrNull() ?: 12
        val isDay = when {
            currentPoint.weatherRaw.contains("day", ignoreCase = true) -> 1
            currentPoint.weatherRaw.contains("night", ignoreCase = true) -> 0
            else -> if (currentLocalHour in 6..21) 1 else 0
        }

        val current = CurrentWeatherDto(
            time = localCurrentTime.format(Date(currentPoint.timeMs)),
            temperature2m = currentPoint.tempC,
            relativeHumidity2m = currentPoint.humidity,
            apparentTemperature = null,
            isDay = isDay,
            precipitation = null,
            weatherCode = currentPoint.wmo,
            windSpeed10m = currentPoint.windMps
        )

        // 24 часа: повторяем ближайшее 3-часовое значение.
        val currentHourStart = currentPoint.timeMs - (currentPoint.timeMs % 3_600_000L)
        val hourlyTimes = mutableListOf<String>()
        val hourlyTemps = mutableListOf<Double>()
        val hourlyCodes = mutableListOf<Int>()
        val hourlyWinds = mutableListOf<Double?>()
        for (hourOffset in 0 until 24) {
            val target = currentHourStart + hourOffset * 3_600_000L
            val nearest = points.minByOrNull { abs(it.timeMs - target) } ?: continue
            hourlyTimes.add(localHourlyTime.format(Date(target)))
            hourlyTemps.add(nearest.tempC)
            hourlyCodes.add(nearest.wmo)
            hourlyWinds.add(nearest.windMps)
        }

        val byDate = points.groupBy { localDate.format(Date(it.timeMs)) }
        val dailyDates = byDate.keys.sorted()
        val dailyTimes = mutableListOf<String>()
        val dailyCodes = mutableListOf<Int>()
        val dailyMaxs = mutableListOf<Double>()
        val dailyMins = mutableListOf<Double>()
        for (date in dailyDates) {
            val dayPoints = byDate[date].orEmpty()
            if (dayPoints.isEmpty()) continue
            val dominant = dayPoints.groupingBy { it.wmo }.eachCount()
                .maxByOrNull { it.value }?.key ?: continue
            dailyTimes.add(date)
            dailyCodes.add(dominant)
            dailyMaxs.add(dayPoints.maxOf { it.tempC })
            dailyMins.add(dayPoints.minOf { it.tempC })
        }
        if (dailyTimes.isEmpty()) {
            throw SevenTimerParseException("7timer: не удалось собрать daily")
        }

        return WeatherApiResponse(
            latitude = latitude,
            longitude = longitude,
            current = current,
            hourly = HourlyWeatherDto(
                time = hourlyTimes,
                temperature2m = hourlyTemps,
                precipitationProbability = null,
                weatherCode = hourlyCodes,
                windSpeed10m = hourlyWinds
            ),
            daily = DailyWeatherDto(
                time = dailyTimes,
                weatherCode = dailyCodes,
                temperature2mMax = dailyMaxs,
                temperature2mMin = dailyMins,
                precipitationProbabilityMax = null
            )
        )
    }

    private fun parseInit(raw: String): Long? {
        if (raw.length < 10) return null
        val formatter = SimpleDateFormat("yyyyMMddHH", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        return try {
            formatter.parse(raw)?.time
        } catch (_: Exception) {
            null
        }
    }
}
