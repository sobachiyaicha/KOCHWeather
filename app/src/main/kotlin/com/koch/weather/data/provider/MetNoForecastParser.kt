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
import kotlin.math.roundToInt

class MetNoParseException(message: String) : Exception(message)

/**
 * Разбор MET Norway locationforecast/2.0/compact в существующую модель
 * WeatherApiResponse, чтобы дальше работал общий WeatherMapper.
 *
 * Никаких выдуманных значений: точки без температуры и без symbol_code
 * пропускаются; если нет текущей точки или daily — исключение.
 */
object MetNoForecastParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private data class Point(
        val timeMs: Long,
        val localDate: String,
        val localHour: Int,
        val temperature: Double,
        val humidity: Double?,
        val windSpeed: Double?,
        val symbolCode: String?,
        val precipitation: Double?
    )

    fun parse(
        body: String,
        nowMillis: Long,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone = TimeZone.getDefault()
    ): WeatherApiResponse {
        val response = try {
            json.decodeFromString<MetNoResponse>(body.removePrefix("\uFEFF").trim())
        } catch (e: Exception) {
            throw MetNoParseException("MET Norway: не удалось разобрать JSON (${e.message})")
        }
        val series = response.properties?.timeseries.orEmpty()
        if (series.isEmpty()) {
            throw MetNoParseException("MET Norway: пустой timeseries")
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
        val hourFormat = SimpleDateFormat("HH", Locale.US).apply { this.timeZone = timeZone }
        val currentTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).apply {
            this.timeZone = timeZone
        }
        val hourlyTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
            this.timeZone = timeZone
        }

        val points = series.mapNotNull { item ->
            val timeMs = parseUtcMillis(item.time) ?: return@mapNotNull null
            val details = item.data.instant?.details
            val temperature = details?.airTemperature ?: return@mapNotNull null
            val symbol = item.data.next1Hours?.summary?.symbolCode
                ?: item.data.next6Hours?.summary?.symbolCode
                ?: item.data.next12Hours?.summary?.symbolCode
            Point(
                timeMs = timeMs,
                localDate = dateFormat.format(Date(timeMs)),
                localHour = hourFormat.format(Date(timeMs)).toIntOrNull() ?: 0,
                temperature = temperature,
                humidity = details.relativeHumidity,
                windSpeed = details.windSpeed,
                symbolCode = symbol,
                precipitation = item.data.next1Hours?.details?.precipitationAmount
            )
        }.sortedBy { it.timeMs }

        if (points.isEmpty()) {
            throw MetNoParseException("MET Norway: в ответе нет точек с температурой")
        }

        val currentPoint = points.lastOrNull { it.timeMs <= nowMillis } ?: points.first()
        val currentWmo = MetNoSymbolMapper.toWmo(currentPoint.symbolCode)
            ?: throw MetNoParseException("MET Norway: не удалось определить текущий symbol_code")

        val current = CurrentWeatherDto(
            time = currentTimeFormat.format(Date(currentPoint.timeMs)),
            temperature2m = currentPoint.temperature,
            relativeHumidity2m = currentPoint.humidity?.roundToInt(),
            apparentTemperature = null,
            isDay = if (currentPoint.localHour in 6..21) 1 else 0,
            precipitation = currentPoint.precipitation,
            weatherCode = currentWmo,
            windSpeed10m = currentPoint.windSpeed
        )

        val hourlyPoints = points
            .filter { it.localDate == currentPoint.localDate && it.timeMs >= currentPoint.timeMs }
            .ifEmpty { listOf(currentPoint) }

        val hourly = HourlyWeatherDto(
            time = hourlyPoints.map { hourlyTimeFormat.format(Date(it.timeMs)) },
            temperature2m = hourlyPoints.map { it.temperature },
            precipitationProbability = null,
            weatherCode = hourlyPoints.map { point ->
                MetNoSymbolMapper.toWmo(point.symbolCode) ?: currentWmo
            },
            windSpeed10m = hourlyPoints.map { it.windSpeed }
        )

        val pointsByDate = points.groupBy { it.localDate }
        val dates = pointsByDate.keys.sorted()
        val dailyTimes = mutableListOf<String>()
        val dailyCodes = mutableListOf<Int>()
        val dailyMaxs = mutableListOf<Double>()
        val dailyMins = mutableListOf<Double>()

        for (date in dates) {
            val dayPoints = pointsByDate[date].orEmpty()
            if (dayPoints.isEmpty()) continue
            val dominantSymbol = dayPoints
                .mapNotNull { it.symbolCode }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
            val wmo = MetNoSymbolMapper.toWmo(dominantSymbol) ?: continue
            dailyTimes.add(date)
            dailyCodes.add(wmo)
            dailyMaxs.add(dayPoints.maxOf { it.temperature })
            dailyMins.add(dayPoints.minOf { it.temperature })
        }

        if (dailyTimes.isEmpty()) {
            throw MetNoParseException("MET Norway: не удалось собрать суточный прогноз")
        }

        return WeatherApiResponse(
            latitude = latitude,
            longitude = longitude,
            current = current,
            hourly = hourly,
            daily = DailyWeatherDto(
                time = dailyTimes,
                weatherCode = dailyCodes,
                temperature2mMax = dailyMaxs,
                temperature2mMin = dailyMins,
                precipitationProbabilityMax = null
            )
        )
    }

    private fun parseUtcMillis(raw: String): Long? {
        val value = raw.trim()
        val withoutZ = value.removeSuffix("Z")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS"
        )
        for (pattern in patterns) {
            val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            try {
                formatter.parse(withoutZ)?.let { return it.time }
            } catch (_: Exception) {
                // пробуем следующий формат
            }
        }
        return null
    }
}
