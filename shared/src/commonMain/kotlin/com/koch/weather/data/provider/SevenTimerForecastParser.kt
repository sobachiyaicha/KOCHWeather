package com.koch.weather.data.provider

import com.koch.weather.data.model.CurrentWeatherDto
import com.koch.weather.data.model.DailyWeatherDto
import com.koch.weather.data.model.HourlyWeatherDto
import com.koch.weather.data.model.WeatherApiResponse
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.math.abs

class SevenTimerParseException(message: String) : Exception(message)

/**
 * 7timer civil: dataseries с шагом 3 часа от init (UTC). Собираем:
 * - current — ближайшая точка;
 * - hourly — 24 часа, значение берём у ближайшей 3-часовой точки;
 * - daily — min/max и доминирующий weather по локальной дате.
 *
 * Замена java.text.SimpleDateFormat: kotlinx-datetime; строки те же —
 * "yyyy-MM-dd", "HH", "yyyy-MM-dd'T'HH:00", "yyyy-MM-dd'T'HH:mm" (1:1).
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
        timeZone: TimeZone = TimeZone.currentSystemDefault()
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
        val currentLocal = Instant.fromEpochMilliseconds(currentPoint.timeMs).toLocalDateTime(timeZone)
        val currentLocalHour = currentLocal.hour
        val isDay = when {
            currentPoint.weatherRaw.contains("day", ignoreCase = true) -> 1
            currentPoint.weatherRaw.contains("night", ignoreCase = true) -> 0
            else -> if (currentLocalHour in 6..21) 1 else 0
        }

        val current = CurrentWeatherDto(
            time = formatYyyyMmDdTHh00(timeZone, currentPoint.timeMs),
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
            hourlyTimes.add(formatYyyyMmDdTHhMm(timeZone, target))
            hourlyTemps.add(nearest.tempC)
            hourlyCodes.add(nearest.wmo)
            hourlyWinds.add(nearest.windMps)
        }

        val byDate = points.groupBy {
            Instant.fromEpochMilliseconds(it.timeMs).toLocalDateTime(timeZone).date.toString()
        }
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

    /**
     * Исходник: SimpleDateFormat("yyyyMMddHH", UTC, isLenient=false).parse(raw).
     * Java не требует полного разбора строки — «лишний хвост» игнорируется,
     * поэтому берём первые 10 символов. Нецелые/вне диапазона части → null
     * (lenient=false отклонял month 13 так же).
     */
    private fun parseInit(raw: String): Long? {
        if (raw.length < 10) return null
        val digits = raw.take(10)
        val year = digits.substring(0, 4).toIntOrNull() ?: return null
        val month = digits.substring(4, 6).toIntOrNull() ?: return null
        val day = digits.substring(6, 8).toIntOrNull() ?: return null
        val hour = digits.substring(8, 10).toIntOrNull() ?: return null
        return try {
            LocalDateTime(year, month, day, hour, 0, 0, 0)
                .toInstant(TimeZone.UTC)
                .toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }
    }
}
