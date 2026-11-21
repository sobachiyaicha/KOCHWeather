package com.koch.weather

/**
 * Точка входа для iOS-оболочки.
 *
 * Swift видит это как `AppInfo.shared`.
 */
object AppInfo {
    val name: String = "КОЧWeather"

    fun greeting(): String = "KOCHWeather · iOS"

    fun buildInfo(): String = "Kotlin ${KotlinVersion.CURRENT} · iOS 16.3"
}
