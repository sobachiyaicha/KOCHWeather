package com.koch.weather.data.provider

import com.koch.weather.data.model.WeatherSource

/**
 * Политика MET Norway: не чаще 30 минут для Скандинавии и 3 часов для
 * остального мира. Значения в миллисекундах.
 */
class MetNoCachePolicy {

    fun isScandinavia(latitude: Double, longitude: Double): Boolean =
        latitude in 54.0..72.0 && longitude in 4.0..32.0

    fun minRefreshIntervalMs(latitude: Double, longitude: Double): Long =
        if (isScandinavia(latitude, longitude)) 30L * 60L * 1000L else 3L * 60L * 60L * 1000L

    /**
     * Можно ли отдать сохранённый MET Norway-прогноз без сетевого запроса.
     * Старые кэши без sourceId не считаются MET-кэшем.
     */
    fun shouldUseCache(
        sourceId: String?,
        savedAt: Long,
        now: Long,
        latitude: Double,
        longitude: Double
    ): Boolean {
        if (sourceId != WeatherSource.MET_NO.id) return false
        if (savedAt <= 0L || now < savedAt) return false
        return now - savedAt < minRefreshIntervalMs(latitude, longitude)
    }
}
