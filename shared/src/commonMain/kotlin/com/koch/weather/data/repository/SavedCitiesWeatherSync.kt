package com.koch.weather.data.repository

import com.koch.weather.data.model.CityCatalog
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.SavedCityWeather
import com.koch.weather.data.model.WeatherDomainData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Догрузка погоды для экрана «Список» (D4 с устройства).
 *
 * Кэш показывается сразу, а города без данных догружаются в фоне: не более
 * [CONCURRENCY] запросов одновременно и не более [MAX_CITIES] городов.
 * Каждый ответ применяется к одной строке; ошибка/таймаут не выдумывают
 * температуру — строка остаётся честным «Нет данных».
 */
object SavedCitiesWeatherSync {

    /** Больше 12 строк «Список» не показывает — лишние запросы не нужны. */
    const val MAX_CITIES: Int = CityCatalog.MAX_SAVED_CITIES

    /** Три параллельных запроса: сеть и провайдеры не захлёбываются. */
    const val CONCURRENCY: Int = 3

    /** Города без реальных данных — в порядке списка, без дублей, максимум 12. */
    fun pendingCities(saved: List<SavedCityWeather>): List<CityLocation> {
        val pending = mutableListOf<CityLocation>()
        for (row in saved) {
            if (row.hasData) continue
            if (pending.any { CityCatalog.isSamePlace(it, row.city) }) continue
            pending.add(row.city)
            if (pending.size >= MAX_CITIES) break
        }
        return pending
    }

    /**
     * Применяет ответ к одной строке списка. Остальные строки не трогаем.
     * Старый кэш не затирает более свежие данные строки.
     */
    fun applyLoaded(
        saved: List<SavedCityWeather>,
        city: CityLocation,
        weather: WeatherDomainData
    ): List<SavedCityWeather> = saved.map { row ->
        val sameCity = CityCatalog.isSamePlace(row.city, city)
        val incomingIsFresh = row.updatedAt == null || weather.lastUpdated >= row.updatedAt
        if (sameCity && (!row.hasData || incomingIsFresh)) {
            SavedCityWeather.from(row.city, weather)
        } else {
            row
        }
    }

    /**
     * Догружает [cities] с ограничением параллельности. [load] возвращает
     * реальные данные или null; [onLoaded] вызывается только для реальных
     * данных и только пока корутина не отменена.
     */
    suspend fun loadPending(
        cities: List<CityLocation>,
        concurrency: Int = CONCURRENCY,
        load: suspend (CityLocation) -> WeatherDomainData?,
        onLoaded: (CityLocation, WeatherDomainData) -> Unit
    ) = coroutineScope {
        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        cities.take(MAX_CITIES).forEach { city ->
            launch {
                semaphore.withPermit {
                    val weather = try {
                        load(city)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    if (weather != null) {
                        onLoaded(city, weather)
                    }
                }
            }
        }
    }
}
