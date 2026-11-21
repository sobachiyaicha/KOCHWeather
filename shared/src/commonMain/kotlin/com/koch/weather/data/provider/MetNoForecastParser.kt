package com.koch.weather.data.provider

import com.koch.weather.data.model.CurrentWeatherDto
import com.koch.weather.data.model.DailyWeatherDto
import com.koch.weather.data.model.HourlyWeatherDto
import com.koch.weather.data.model.WeatherApiResponse
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

class MetNoParseException(message: String) : Exception(message)

/**
 * Разбор MET Norway locationforecast/2.0/compact в существующую модель
 * WeatherApiResponse, чтобы дальше работал общий WeatherMapper.
 *
 * Никаких выдуманных значений: точки без температуры и без symbol_code
 * пропускаются; если нет текущей точки или daily — исключение.
 *
 * Замена java.text.SimpleDateFormat: kotlinx-datetime, [TimeZone] —
 * kotlinx.datetime.TimeZone; строки даты/времени те же ("yyyy-MM-dd",
 * "HH", "yyyy-MM-dd'T'HH:00", "yyyy-MM-dd'T'HH:mm") — результат 1:1.
 */
object MetNoForecastParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS

    private data class Point(
        val timeMs: Long,
        val localDate: String,
        val localHour: Int,
        val temperature: Double,
        val humidity: Double?,
        val windSpeed: Double?,
        val symbolCode: String?,
        val precipitation: Double?,
        val precipitation6h: Double?,
        val precipitation12h: Double?
    )

    /** Кусок ряда осадков: значение за [durationHours] часов, начиная с [startMs]. */
    private data class PrecipitationSpan(
        val startMs: Long,
        val durationHours: Int,
        val amount: Double
    )

    fun parse(
        body: String,
        nowMillis: Long,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
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

        val points = series.mapNotNull { item ->
            val timeMs = parseUtcMillis(item.time) ?: return@mapNotNull null
            val details = item.data.instant?.details
            val temperature = details?.airTemperature ?: return@mapNotNull null
            val symbol = item.data.next1Hours?.summary?.symbolCode
                ?: item.data.next6Hours?.summary?.symbolCode
                ?: item.data.next12Hours?.summary?.symbolCode
            val local = Instant.fromEpochMilliseconds(timeMs).toLocalDateTime(timeZone)
            Point(
                timeMs = timeMs,
                localDate = local.date.toString(),
                localHour = local.hour,
                temperature = temperature,
                humidity = details.relativeHumidity,
                windSpeed = details.windSpeed,
                symbolCode = symbol,
                precipitation = item.data.next1Hours?.details?.precipitationAmount,
                precipitation6h = item.data.next6Hours?.details?.precipitationAmount,
                precipitation12h = item.data.next12Hours?.details?.precipitationAmount
            )
        }.sortedBy { it.timeMs }

        if (points.isEmpty()) {
            throw MetNoParseException("MET Norway: в ответе нет точек с температурой")
        }

        val currentPoint = points.lastOrNull { it.timeMs <= nowMillis } ?: points.first()
        val currentWmo = MetNoSymbolMapper.toWmo(currentPoint.symbolCode)
            ?: throw MetNoParseException("MET Norway: не удалось определить текущий symbol_code")

        val current = CurrentWeatherDto(
            time = formatYyyyMmDdTHh00(timeZone, currentPoint.timeMs),
            temperature2m = currentPoint.temperature,
            relativeHumidity2m = currentPoint.humidity?.roundToInt(),
            apparentTemperature = null,
            isDay = if (currentPoint.localHour in 6..21) 1 else 0,
            precipitation = currentPoint.precipitation,
            weatherCode = currentWmo,
            windSpeed10m = currentPoint.windSpeed
        )

        // Почасовой ряд — следующие 24 часа от текущей точки, а не «остаток суток»:
        // вечером старый фильтр по localDate оставлял 1–3 часа и график осадков пустел.
        val horizonEnd = currentPoint.timeMs + DAY_MS
        val hourlyPoints = points
            .filter { it.timeMs >= currentPoint.timeMs && it.timeMs < horizonEnd }
            .take(24)

        val hourly = HourlyWeatherDto(
            time = hourlyPoints.map { formatYyyyMmDdTHhMm(timeZone, it.timeMs) },
            temperature2m = hourlyPoints.map { it.temperature },
            precipitationProbability = null,
            weatherCode = hourlyPoints.map { point ->
                MetNoSymbolMapper.toWmo(point.symbolCode) ?: currentWmo
            },
            windSpeed10m = hourlyPoints.map { it.windSpeed },
            // Реальные мм/ч из next_1_hours; у точки без поля — null, не 0.
            precipitation = hourlyPoints.map { it.precipitation }
        )

        val pointsByDate = points.groupBy { it.localDate }
        val dates = pointsByDate.keys.sorted()
        val dailyTimes = mutableListOf<String>()
        val dailyCodes = mutableListOf<Int>()
        val dailyMaxs = mutableListOf<Double>()
        val dailyMins = mutableListOf<Double>()
        val dailyPrecipitation = mutableListOf<Double?>()

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
            // Сумма за сутки: 1-часовые + 6-часовые + 12-часовые количества без дублей.
            dailyPrecipitation.add(sumDailyPrecipitation(dayPoints))
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
                precipitationProbabilityMax = null,
                precipitationSum = dailyPrecipitation
            )
        )
    }

    /**
     * Суточная сумма осадков, мм.
     *
     * В реальном compact-ответе MET (например `shots/fq3_metno_live.json`) ближние
     * точки содержат одновременно `next_1_hours` и `next_6_hours`, причём 6-часовое
     * значение — скользящее окно от этой же точки. Простое сложение всех найденных
     * полей удвоило бы осадки, поэтому суммируем непересекающиеся интервалы:
     * сначала часовые (по 1 ч), затем 6- и 12-часовые только там, где этих часов
     * ещё нет. Значения за дальние дни (только 6-часовые точки) суммируются целиком.
     *
     * null — если за сутки не было ни одного значения (не подменяем нулём).
     */
    private fun sumDailyPrecipitation(dayPoints: List<Point>): Double? {
        val spans = ArrayList<PrecipitationSpan>()
        for (point in dayPoints) {
            point.precipitation?.let { spans.add(PrecipitationSpan(point.timeMs, 1, it)) }
            point.precipitation6h?.let { spans.add(PrecipitationSpan(point.timeMs, 6, it)) }
            point.precipitation12h?.let { spans.add(PrecipitationSpan(point.timeMs, 12, it)) }
        }
        if (spans.isEmpty()) return null

        // Короткие интервалы важнее: часовые данные точнее 6- и 12-часовых.
        val ordered = spans.sortedWith(compareBy({ it.durationHours }, { it.startMs }))
        val accepted = ArrayList<Pair<Long, Long>>()
        var total = 0.0
        for (span in ordered) {
            val start = span.startMs
            val end = start + span.durationHours * HOUR_MS
            val overlaps = accepted.any { (takenStart, takenEnd) -> start < takenEnd && takenStart < end }
            if (overlaps) continue
            accepted.add(start to end)
            total += span.amount
        }
        return total
    }

    private fun parseUtcMillis(raw: String): Long? {
        val value = raw.trim()
        val withoutZ = value.removeSuffix("Z")
        // Исходник: SimpleDateFormat "yyyy-MM-dd'T'HH:mm:ss" и ".SSS" в UTC,
        // isLenient=false. ISO-парсер kotlinx-datetime принимает обе формы
        // (дробная часть секунд необязательна) и отклоняет остальное так же.
        return try {
            LocalDateTime.parse(withoutZ)
                .toInstant(UtcOffset.ZERO)
                .toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }
    }
}

/** "yyyy-MM-dd'T'HH:00" в заданной таймзоне (минуты исходником отбрасывались). */
internal fun formatYyyyMmDdTHh00(timeZone: TimeZone, epochMs: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone)
    return local.date.toString() + "T" + local.hour.toString().padStart(2, '0') + ":00"
}

/** "yyyy-MM-dd'T'HH:mm" в заданной таймзоне (секунды исходником отбрасывались). */
internal fun formatYyyyMmDdTHhMm(timeZone: TimeZone, epochMs: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone)
    return local.date.toString() + "T" +
        local.hour.toString().padStart(2, '0') + ":" +
        local.minute.toString().padStart(2, '0')
}
