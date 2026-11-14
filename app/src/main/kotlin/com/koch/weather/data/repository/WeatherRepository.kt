package com.koch.weather.data.repository

import android.content.Context
import com.koch.weather.data.api.OpenMeteoClient
import com.koch.weather.data.cache.CachedForecast
import com.koch.weather.data.cache.WeatherCache
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.mapper.WeatherMapResult
import com.koch.weather.data.mapper.WeatherMapper
import com.koch.weather.data.model.CachedWeather
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.CitySearchResult
import com.koch.weather.data.model.SavedCityWeather
import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherLoadResult
import com.koch.weather.data.model.WeatherSource
import com.koch.weather.data.provider.GeocodingChain
import com.koch.weather.data.provider.MetNoProvider
import com.koch.weather.data.provider.NominatimGeocodingProvider
import com.koch.weather.data.provider.OpenMeteoGeocodingProvider
import com.koch.weather.data.provider.OpenMeteoProvider
import com.koch.weather.data.provider.PhotonGeocodingProvider
import com.koch.weather.data.provider.ProviderAttemptFailure
import com.koch.weather.data.provider.WeatherProviderChain
import com.koch.weather.data.provider.WeatherProviderChainResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class WeatherRepository @JvmOverloads constructor(
    context: Context,
    private val trafficRepository: TrafficRepository = TrafficRepository()
) {
    private val openMeteoClient = OpenMeteoClient()
    private val metNoProvider = MetNoProvider()
    private val weatherProviderChain = WeatherProviderChain(
        listOf(
            metNoProvider,
            OpenMeteoProvider(openMeteoClient)
        )
    )
    private val geocodingChain = GeocodingChain(
        listOf(
            PhotonGeocodingProvider(),
            NominatimGeocodingProvider(),
            OpenMeteoGeocodingProvider(openMeteoClient)
        )
    )

    private val cache = WeatherCache(context.applicationContext)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Только реальные прогнозы: сеть (MET Norway/Open-Meteo) или DataStore.
    private val memoryCache = ConcurrentHashMap<String, WeatherDomainData>()
    private val memorySource = ConcurrentHashMap<String, WeatherSource>()

    val defaultCities = listOf(
        CityLocation("Санкт-Петербург", 59.9386, 30.3141, "Россия", "Санкт-Петербург"),
        CityLocation("Москва", 55.7558, 37.6173, "Россия", "Москва"),
        CityLocation("Минск", 53.9006, 27.5590, "Беларусь", "Минск"),
        CityLocation("Дубай", 25.2048, 55.2708, "ОАЭ", "Дубай")
    )

    /** Мгновенный доступ к уже загруженному реальному прогнозу (без сети и DataStore). */
    fun getCachedWeatherFast(city: CityLocation): WeatherDomainData? =
        memoryCache[city.name]?.copy(dataSourceLabel = null)

    /** Реальный прогноз из памяти или DataStore с указанием источника; null — данных нет. */
    suspend fun getCachedWeather(city: CityLocation): CachedWeather? = withContext(Dispatchers.IO) {
        val memory = memoryCache[city.name]
        if (memory != null) {
            return@withContext CachedWeather(
                data = memory.copy(dataSourceLabel = null, isOffline = true),
                source = memorySource[city.name] ?: WeatherSource.CACHE
            )
        }
        readCachedWeather(city)
    }

    /**
     * Список сохранённых городов с реальными температурами из памяти/DataStore.
     * Для города без данных [SavedCityWeather.tempC] == null.
     */
    suspend fun getSavedCitiesWeather(cities: List<CityLocation>): List<SavedCityWeather> =
        withContext(Dispatchers.IO) {
            cities.map { city ->
                val domain = memoryCache[city.name] ?: readCachedWeather(city)?.data
                SavedCityWeather.from(city, domain)
            }
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

    suspend fun getWeatherData(
        city: CityLocation,
        isOnline: Boolean = true
    ): WeatherLoadResult = withContext(Dispatchers.IO) {
        // Без сети не ждём таймаутов: сразу честный кэш/«нет данных».
        if (!isOnline) {
            return@withContext cachedOrUnavailable(
                city = city,
                failure = WeatherErrors.noNetwork(),
                attempts = emptyList()
            )
        }
        val now = System.currentTimeMillis()

        // Политика MET Norway: при наличии свежего кэша не дёргаем сеть чаще
        // разрешённого интервала (30 мин Скандинавия / 3 ч остальной мир).
        val snapshot = cache.getCachedForecast(city.name)
        if (snapshot != null && metNoProvider.shouldUseCachedForecast(
                sourceId = snapshot.sourceId,
                savedAt = snapshot.savedAt,
                now = now,
                latitude = city.latitude,
                longitude = city.longitude
            )
        ) {
            val interval = metNoProvider.minRefreshIntervalMs(city.latitude, city.longitude)
            val domain = decodeCachedDomain(city, snapshot)
            if (domain != null) {
                val freshEnough = domain.copy(
                    isOffline = false,
                    // Это DataStore-кэш, поэтому подпись источника в домене null;
                    // источник для UI идёт отдельно в weatherAttributionRu.
                    dataSourceLabel = null
                )
                memoryCache[city.name] = freshEnough
                memorySource[city.name] = WeatherSource.MET_NO
                return@withContext WeatherLoadResult.Live(
                    data = freshEnough,
                    source = WeatherSource.MET_NO,
                    fallbackNote = "MET Norway: данные из кэша (обновление не чаще " +
                        "${interval / 60_000} мин)."
                )
            }
        }

        when (val chain = weatherProviderChain.fetch(city.latitude, city.longitude)) {
            is WeatherProviderChainResult.Success -> {
                val traffic = trafficRepository.getTraffic(city)
                val mapped = WeatherMapper.map(
                    city = city,
                    dto = chain.response,
                    isOffline = false,
                    lastUpdated = chain.fetchedAt,
                    trafficScore = traffic.score,
                    trafficAdvice = traffic.description,
                    dataSourceLabel = if (chain.fromProviderCache) {
                        null
                    } else {
                        chain.source.displayNameRu
                    }
                )
                when (mapped) {
                    is WeatherMapResult.Success -> {
                        memoryCache[city.name] = mapped.data
                        memorySource[city.name] = chain.source
                        try {
                            // R19: сохраняем полный доменный снимок (не только ответ API),
                            // чтобы при офлайне показать ровно те же цифры и время.
                            cache.saveForecast(
                                city = city,
                                jsonString = json.encodeToString(mapped.data),
                                sourceId = chain.source.id
                            )
                        } catch (_: Exception) {
                            // Показ уже полученного реального прогноза не должен падать из-за кэша.
                        }
                        val failoverNote = buildFailoverNote(chain.failedAttempts)
                        val providerCacheNote = if (chain.fromProviderCache) {
                            "MET Norway: данные из кэша провайдера."
                        } else {
                            null
                        }
                        WeatherLoadResult.Live(
                            data = mapped.data,
                            source = chain.source,
                            fallbackNote = listOfNotNull(failoverNote, providerCacheNote)
                                .joinToString(" ")
                                .ifBlank { null }
                        )
                    }

                    is WeatherMapResult.Failure -> cachedOrUnavailable(
                        city = city,
                        failure = mapped.failure,
                        attempts = chain.failedAttempts
                    )
                }
            }

            is WeatherProviderChainResult.AllFailed -> {
                val failure = chain.failedAttempts.lastOrNull()?.failure
                    ?: WeatherFailure(
                        WeatherErrorReason.UNKNOWN,
                        "Не удалось получить прогноз. Повторите попытку."
                    )
                cachedOrUnavailable(city, failure, chain.failedAttempts)
            }
        }
    }

    suspend fun searchCities(query: String): CitySearchResult = withContext(Dispatchers.IO) {
        // Photon → Nominatim → Open-Meteo geocoding; локальных «поисков» по
        // дефолтным городам при ошибке больше нет.
        geocodingChain.search(query)
    }

    /**
     * При сетевой ошибке отдаём последний РЕАЛЬНЫЙ прогноз для этого же города
     * (память/DataStore) с isOffline=true. Если кэша нет — Unavailable.
     */
    private suspend fun cachedOrUnavailable(
        city: CityLocation,
        failure: WeatherFailure,
        attempts: List<ProviderAttemptFailure>
    ): WeatherLoadResult {
        val reason = combinedFailure(failure, attempts)
        val memory = memoryCache[city.name]
        if (memory != null) {
            val offline = memory.copy(isOffline = true, dataSourceLabel = null)
            memoryCache[city.name] = offline
            return WeatherLoadResult.Cached(
                data = offline,
                source = memorySource[city.name] ?: WeatherSource.CACHE,
                reason = reason
            )
        }
        val cached = readCachedWeather(city)
        if (cached != null) {
            val offline = cached.data.copy(isOffline = true, dataSourceLabel = null)
            memoryCache[city.name] = offline
            memorySource[city.name] = cached.source
            return WeatherLoadResult.Cached(
                data = offline,
                source = cached.source,
                reason = reason
            )
        }
        return WeatherLoadResult.Unavailable(reason, attempts.map { it.failure })
    }

    private suspend fun readCachedWeather(city: CityLocation): CachedWeather? {
        val snapshot = cache.getCachedForecast(city.name) ?: return null
        val domain = decodeCachedDomain(city, snapshot) ?: return null
        val source = WeatherSource.fromId(snapshot.sourceId) ?: WeatherSource.CACHE
        return CachedWeather(
            data = domain.copy(isOffline = true, dataSourceLabel = null),
            source = source
        )
    }

    private fun decodeCachedDomain(
        city: CityLocation,
        snapshot: CachedForecast
    ): WeatherDomainData? {
        // Новый формат R19: полный WeatherDomainData-снимок.
        try {
            val domain = json.decodeFromString<WeatherDomainData>(snapshot.json)
            return domain.copy(
                lastUpdated = if (domain.lastUpdated > 0) domain.lastUpdated else snapshot.savedAt,
                isOffline = true,
                dataSourceLabel = null
            )
        } catch (_: Exception) {
            // Миграция старого кэша (JSON ответа Open-Meteo) — без потери данных.
        }
        return try {
            val dto = json.decodeFromString<WeatherApiResponse>(snapshot.json)
            val mapped = WeatherMapper.map(
                city = city,
                dto = dto,
                isOffline = true,
                lastUpdated = snapshot.savedAt,
                trafficScore = null,
                trafficAdvice = "Нет данных о пробках"
            )
            (mapped as? WeatherMapResult.Success)?.data?.copy(dataSourceLabel = null)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildFailoverNote(attempts: List<ProviderAttemptFailure>): String? =
        if (attempts.isEmpty()) {
            null
        } else {
            attempts.joinToString("; ") {
                "${it.source.displayNameRu} недоступен: ${it.failure.messageRu}"
            }
        }

    private fun combinedFailure(
        failure: WeatherFailure,
        attempts: List<ProviderAttemptFailure>
    ): WeatherFailure =
        if (attempts.isEmpty()) {
            failure
        } else {
            WeatherFailure(
                reason = failure.reason,
                messageRu = buildFailoverNote(attempts) ?: failure.messageRu
            )
        }
}
