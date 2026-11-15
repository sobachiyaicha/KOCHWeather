package com.koch.weather.data.repository

import com.koch.weather.data.provider.MetNoCachePolicy

enum class WeatherLoadAction {
    /** Свежий кэш MET Norway в рамках политики: отдаём CACHED, сеть не трогаем. */
    SERVE_CACHE,

    /** Нужна реальная сетевая попытка (гонка провайдеров). */
    FETCH_NETWORK,

    /** Сети нет: отдаём кэш/«нет данных» без запроса. */
    OFFLINE_FALLBACK
}

/**
 * Решение о загрузке до обращения к DataStore/сети. Вынесено отдельно,
 * чтобы R19-правило «Live только после реального ответа» было тестируемым.
 */
object WeatherLoadPolicy {

    fun decide(
        isOnline: Boolean,
        forceRefresh: Boolean,
        sourceId: String?,
        savedAt: Long,
        now: Long,
        latitude: Double,
        longitude: Double,
        cachePolicy: MetNoCachePolicy = MetNoCachePolicy()
    ): WeatherLoadAction {
        if (!isOnline) return WeatherLoadAction.OFFLINE_FALLBACK
        if (forceRefresh) return WeatherLoadAction.FETCH_NETWORK
        return if (cachePolicy.shouldUseCache(sourceId, savedAt, now, latitude, longitude)) {
            WeatherLoadAction.SERVE_CACHE
        } else {
            WeatherLoadAction.FETCH_NETWORK
        }
    }
}
