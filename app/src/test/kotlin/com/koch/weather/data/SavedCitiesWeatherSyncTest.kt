package com.koch.weather.data

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.SavedCityWeather
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherIconType
import com.koch.weather.data.repository.SavedCitiesWeatherSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * D4 с устройства: «Список» сначала показывает кэш, затем честно догружает
 * города без данных — не более 12 городов, 3 параллельно, по одной строке.
 */
class SavedCitiesWeatherSyncTest {

    private fun city(index: Int, name: String = "Город$index") = CityLocation(
        name = name,
        latitude = 10.0 + index,
        longitude = 20.0 + index,
        country = "Россия"
    )

    private fun weather(
        city: CityLocation,
        temp: Int = 5,
        updatedAt: Long = 100L
    ) = WeatherDomainData(
        cityName = city.name,
        currentTemp = temp,
        conditionDescription = "Переменная облачность",
        minTemp = temp - 2,
        maxTemp = temp + 2,
        humidity = 60,
        windSpeed = 3.0,
        weatherCode = 3,
        iconType = WeatherIconType.PARTLY_CLOUDY_DAY,
        clothingAdvice = "Прохладно",
        hourly = emptyList(),
        daily = emptyList(),
        isOffline = false,
        lastUpdated = updatedAt,
        dataSourceLabel = "MET Norway"
    )

    @Test
    fun pendingCities_skipsCachedRowsAndKeepsOrder() {
        val cities = (1..4).map { city(it) }
        val saved = listOf(
            SavedCityWeather.from(cities[0], weather(cities[0])),
            SavedCityWeather.from(cities[1], null),
            SavedCityWeather.from(cities[2], weather(cities[2])),
            SavedCityWeather.from(cities[3], null)
        )

        val pending = SavedCitiesWeatherSync.pendingCities(saved)

        assertEquals(listOf("Город2", "Город4"), pending.map { it.name })
    }

    @Test
    fun pendingCities_capsAtTwelveCities() {
        val saved = (1..20).map { SavedCityWeather.from(city(it), null) }

        val pending = SavedCitiesWeatherSync.pendingCities(saved)

        assertEquals(12, pending.size)
    }

    @Test
    fun pendingCities_deduplicatesSamePlace() {
        val first = city(1, "Москва")
        val duplicate = first.copy(name = "Москва-дубль")
        val saved = listOf(
            SavedCityWeather.from(first, null),
            SavedCityWeather.from(duplicate, null)
        )

        val pending = SavedCitiesWeatherSync.pendingCities(saved)

        assertEquals(1, pending.size)
    }

    @Test
    fun applyLoaded_updatesOnlyMatchingRow() {
        val cities = (1..3).map { city(it) }
        val saved = cities.map { SavedCityWeather.from(it, null) }

        val updated = SavedCitiesWeatherSync.applyLoaded(
            saved = saved,
            city = cities[1],
            weather = weather(cities[1], temp = 9)
        )

        assertNull(updated[0].tempC)
        assertEquals(9, updated[1].tempC)
        assertNull(updated[2].tempC)
    }

    @Test
    fun applyLoaded_keepsFresherRowAndDoesNotOverwriteWithStaleData() {
        val target = city(1, "Москва")
        val fresh = SavedCityWeather.from(target, weather(target, temp = 7, updatedAt = 200L))

        val updated = SavedCitiesWeatherSync.applyLoaded(
            saved = listOf(fresh),
            city = target,
            weather = weather(target, temp = -30, updatedAt = 100L)
        )

        assertEquals(7, updated.single().tempC)
        assertEquals(200L, updated.single().updatedAt)
    }

    @Test
    fun applyLoaded_neverInventsTemperatureWhenCityDidNotLoad() {
        val cities = (1..2).map { city(it) }
        val saved = cities.map { SavedCityWeather.from(it, null) }

        val unchanged = SavedCitiesWeatherSync.applyLoaded(
            saved = saved,
            city = city(99),
            weather = weather(city(99), temp = 20)
        )

        assertTrue(unchanged.all { it.tempC == null })
        assertEquals("Нет данных", unchanged[0].conditionOrNoDataRu)
    }

    @Test
    fun listRefresh_everyRowEndsWithTemperatureOrHonestNoData() = runTest {
        val cities = (1..5).map { city(it) }
        var saved = cities.mapIndexed { index, target ->
            // Первые два города уже есть в кэше, остальные — без данных.
            SavedCityWeather.from(target, if (index < 2) weather(target) else null)
        }
        val pending = SavedCitiesWeatherSync.pendingCities(saved)
        assertEquals(3, pending.size)

        SavedCitiesWeatherSync.loadPending(
            cities = pending,
            load = { target ->
                // Сеть дала данные только для первого из догружаемых.
                if (target.name == "Город3") weather(target) else null
            },
            onLoaded = { target, data ->
                saved = SavedCitiesWeatherSync.applyLoaded(saved, target, data)
            }
        )

        assertEquals(3, saved.count { it.hasData })
        saved.forEach { row ->
            assertTrue(
                "Строка ${row.name} должна быть с температурой или честным «Нет данных»",
                row.hasData || row.conditionOrNoDataRu == "Нет данных"
            )
        }
    }

    @Test
    fun loadPending_runsAtMostThreeAtOnceAndReportsAll() = runTest {
        val cities = (1..9).map { city(it) }
        val active = AtomicInteger(0)
        val maxActive = AtomicInteger(0)
        val loaded = mutableListOf<String>()

        SavedCitiesWeatherSync.loadPending(
            cities = cities,
            load = { target ->
                val now = active.incrementAndGet()
                maxActive.updateAndGet { previous -> maxOf(previous, now) }
                delay(20)
                active.decrementAndGet()
                weather(target)
            },
            onLoaded = { target, _ -> loaded.add(target.name) }
        )

        assertEquals(3, maxActive.get())
        assertEquals(cities.map { it.name }.toSet(), loaded.toSet())
    }

    @Test
    fun loadPending_keepsNoDataWhenLoaderReturnsNull() = runTest {
        val cities = (1..5).map { city(it) }
        val loaded = mutableListOf<String>()

        SavedCitiesWeatherSync.loadPending(
            cities = cities,
            load = { null },
            onLoaded = { target, _ -> loaded.add(target.name) }
        )

        assertTrue(loaded.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadPending_cancellationStopsCallbacks() = runTest {
        val cities = (1..9).map { city(it) }
        var loaded = 0

        val job = launch {
            SavedCitiesWeatherSync.loadPending(
                cities = cities,
                load = {
                    delay(1_000)
                    weather(it)
                },
                onLoaded = { _, _ -> loaded++ }
            )
        }
        advanceTimeBy(10)
        job.cancelAndJoin()

        assertEquals(0, loaded)
    }
}
