package com.koch.weather

import android.app.Application
import com.koch.weather.platform.AndroidPlatform

/**
 * Application-класс (фаза 2, минимальный wiring по запросу platform-engineer,
 * см. `_history/reports/phase-2/platform-engineer.md` «Что нужно от других ролей»):
 * в onCreate устанавливается контекст для платформенных actual
 * (`AndroidPlatform.appContext` — декод картинок, openRawResource фонов, mediaUrl,
 * trafficApiKey). Раньше класс отсутствовал: приложение жило на
 * reflection-фолбэке ActivityThread.currentApplication().
 */
class KochApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatform.install(this)
    }
}
