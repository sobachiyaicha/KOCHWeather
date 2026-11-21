package com.koch.weather.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.koch.weather.data.model.CityLocation
import com.koch.weather.platform.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path

/**
 * KMP-версия прежнего androidMain WeatherCache (эталон: git 2e1e3b2,
 * `shared/src/androidMain/kotlin/com/koch/weather/data/cache/WeatherCache.kt`).
 *
 * Отличия от эталона — только механизм получения DataStore, ключи и JSON 1:1:
 * - прежний делегат `preferencesDataStore(name = "weather_cache")` заменён на
 *   `PreferenceDataStoreFactory.createWithPath(scope) { file }`, путь приходит
 *   из `AppContainer.dataStoreFile` (`platform.dataStoreFile()`):
 *   Android — `filesDir/datastore/weather_cache.preferences_pb` (тот же файл,
 *   что создавал делегат: данные переживают обновление без миграции),
 *   iOS — `NSApplicationSupportDirectory/koch.preferences_pb`;
 * - `System.currentTimeMillis()` → `platform.nowMillis()`;
 * - `lowercase(Locale.ROOT)` → `lowercase()` без локали (Kotlin stdlib даёт
 *   locale-invariant маппинг — тот же результат, что Locale.ROOT);
 * - `Context`-параметр конструктора заменён на `(scope, file)` —
 *   см. отчёт cache-porter: список call-site'ов.
 *
 * Гарантия «один DataStore на файл на процесс»: прежний делегат был синглтоном
 * на процесс, а WeatherCache в Android-эталоне создавался в двух местах
 * (MainScreenViewModel и WeatherRepository). Чтобы два экземпляра WeatherCache
 * не породили два активных DataStore на один файл (IllegalStateException
 * "There are multiple DataStores active for the same file"), экземпляры
 * кэшируются в companion по пути файла — семантика делегата сохранена.
 */
class WeatherCache(
    /** Скоуп приложения (AppContainer.applicationScope) для DataStore-актора. */
    private val scope: CoroutineScope,
    /** Полный путь к файлу Preferences DataStore (AppContainer.dataStoreFile). */
    private val file: Path,
    /** Диспетчер кэш-вызовов (iOS: KochMainQueueDispatcher; тесты: Unconfined —
     *  в kexe без runloop главная очередь не качается, см. CacheIoDispatcher.ios.kt). */
    private val ioDispatcher: CoroutineDispatcher = cacheIoDispatcher()
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val KEY_LAST_WEATHER = stringPreferencesKey("last_weather_json")
    private val KEY_LAST_CITY = stringPreferencesKey("last_city_json")
    /** Именно выбранный пользователем город: отдельно от «последнего прогноза». */
    private val KEY_SELECTED_CITY = stringPreferencesKey("selected_city_json")
    private val KEY_SAVED_CITIES = stringPreferencesKey("saved_cities_json")
    private val KEY_LAST_UPDATED = longPreferencesKey("last_updated_time")
    private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium_purchased")
    private val KEY_UNLOCKED_THEMES = stringSetPreferencesKey("unlocked_themes_set")
    private val KEY_CURRENT_THEME = stringPreferencesKey("current_theme")
    private val KEY_CLOUDS_CLEARED = booleanPreferencesKey("clouds_cleared")
    private val KEY_LOOTBOX_HISTORY = stringPreferencesKey("lootbox_history_json")

    /** Сколько бесплатных попыток лутбокса потрачено (лимит — 5 на пользователя). */
    private val KEY_LOOTBOX_ATTEMPTS_USED = intPreferencesKey("lootbox_attempts_used")

    /** Первый в жизни пользователя рекламный поп-ап уже показан (ТЗ: обязательно «лев»). */
    private val KEY_FIRST_AD_SHOWN = booleanPreferencesKey("first_ad_shown")

    private fun cityKey(cityName: String) =
        stringPreferencesKey("weather_${cityName.trim().lowercase().replace(" ", "_")}")

    private fun cityUpdatedKey(cityName: String) =
        longPreferencesKey("weather_updated_${cityName.trim().lowercase().replace(" ", "_")}")

    private fun citySourceKey(cityName: String) =
        stringPreferencesKey("weather_source_${cityName.trim().lowercase().replace(" ", "_")}")

    /**
     * Ленивое создание DataStore с синглтон-гарантией по файлу (см. KDoc класса).
     * Все вызовы `store()` suspend — Mutex согласован со suspend-методами класса.
     */
    private suspend fun store(): DataStore<Preferences> =
        SINGLETON_MUTEX.withLock {
            SINGLETON_INSTANCES.getOrPut(file.toString()) {
                PreferenceDataStoreFactory.createWithPath(
                    scope = scope,
                    produceFile = { file }
                )
            }
        }

    val isPremiumFlow: Flow<Boolean> = flow {
        emitAll(store().data.map { prefs -> prefs[KEY_IS_PREMIUM] ?: false })
    }

    suspend fun setPremium(purchased: Boolean) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                prefs[KEY_IS_PREMIUM] = purchased
            }
        } catch (_: Exception) {}
    }

    suspend fun isPremium(): Boolean = withContext(ioDispatcher) {
        try {
            store().data.map { it[KEY_IS_PREMIUM] ?: false }.first()
        } catch (_: Exception) {
            false
        }
    }

    val unlockedThemesFlow: Flow<Set<String>> = flow {
        emitAll(store().data.map { prefs -> prefs[KEY_UNLOCKED_THEMES] ?: emptySet() })
    }

    suspend fun unlockTheme(themeKey: String) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                val current = prefs[KEY_UNLOCKED_THEMES] ?: emptySet()
                prefs[KEY_UNLOCKED_THEMES] = current + themeKey
            }
        } catch (_: Exception) {}
    }

    suspend fun getUnlockedThemes(): Set<String> = withContext(ioDispatcher) {
        try {
            store().data.map { it[KEY_UNLOCKED_THEMES] ?: emptySet() }.first()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /** Тема и флаг облаков читаются одним снапшотом DataStore. */
    suspend fun getUiPreferences(): UiPreferences = withContext(ioDispatcher) {
        try {
            val prefs = store().data.first()
            UiPreferences(
                themeId = prefs[KEY_CURRENT_THEME],
                cloudsCleared = prefs[KEY_CLOUDS_CLEARED] ?: false
            )
        } catch (_: Exception) {
            UiPreferences()
        }
    }

    suspend fun setCurrentTheme(themeId: String) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                prefs[KEY_CURRENT_THEME] = themeId
            }
        } catch (_: Exception) {}
    }

    suspend fun setCloudsCleared(cleared: Boolean) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                prefs[KEY_CLOUDS_CLEARED] = cleared
            }
        } catch (_: Exception) {}
    }

    /**
     * Флаг «первый рекламный поп-ап в жизни пользователя показан».
     * Пока false — поп-ап обязан открыть `ad_lion` (ролик «лев в очках»).
     * Ошибка DataStore трактуется как «ещё не показывали»: лучше показать лева
     * повторно, чем сломать требование первого показа.
     */
    suspend fun isFirstAdShown(): Boolean = withContext(ioDispatcher) {
        try {
            store().data.map { prefs -> prefs[KEY_FIRST_AD_SHOWN] ?: false }.first()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun setFirstAdShown(shown: Boolean = true) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                prefs[KEY_FIRST_AD_SHOWN] = shown
            }
        } catch (_: Exception) {}
    }

    suspend fun saveForecast(
        city: CityLocation,
        jsonString: String,
        sourceId: String
    ) = withContext(ioDispatcher) {
        try {
            val now = nowMillis()
            store().edit { prefs ->
                prefs[KEY_LAST_WEATHER] = jsonString
                prefs[KEY_LAST_CITY] = json.encodeToString(city)
                prefs[KEY_LAST_UPDATED] = now
                prefs[cityKey(city.name)] = jsonString
                prefs[cityUpdatedKey(city.name)] = now
                prefs[citySourceKey(city.name)] = sourceId
            }
        } catch (_: Exception) {}
    }

    /**
     * Кэш только для запрошенного города. Никакого fallback на прогноз другого
     * города: иначе Москва показывала бы погоду Санкт-Петербурга как свою.
     */
    suspend fun getCachedForecast(cityName: String): CachedForecast? = withContext(ioDispatcher) {
        try {
            val prefs = store().data.first()
            val json = prefs[cityKey(cityName)] ?: return@withContext null
            val savedAt = prefs[cityUpdatedKey(cityName)] ?: prefs[KEY_LAST_UPDATED] ?: 0L
            CachedForecast(
                json = json,
                savedAt = savedAt,
                sourceId = prefs[citySourceKey(cityName)]
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLastCity(): CityLocation? = withContext(ioDispatcher) {
        try {
            val cityJson = store().data.map { prefs -> prefs[KEY_LAST_CITY] }.first() ?: return@withContext null
            json.decodeFromString<CityLocation>(cityJson)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Выбранный город (D-2 feature-qa): отдельный ключ от [KEY_LAST_CITY],
     * который пишется при сохранении прогноза. Хранится целиком, по координатам:
     * при восстановлении город ищется в актуальном списке через
     * `CityCatalog.isSamePlace`.
     */
    suspend fun saveSelectedCity(city: CityLocation) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                prefs[KEY_SELECTED_CITY] = json.encodeToString(city)
            }
        } catch (_: Exception) {}
    }

    /** null = пользователь ещё ничего не выбирал (или DataStore недоступен). */
    suspend fun getSelectedCity(): CityLocation? = withContext(ioDispatcher) {
        try {
            val cityJson = store().data
                .map { prefs -> prefs[KEY_SELECTED_CITY] }
                .first() ?: return@withContext null
            json.decodeFromString<CityLocation>(cityJson)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCitiesList(cities: List<CityLocation>) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                prefs[KEY_SAVED_CITIES] = json.encodeToString(cities)
            }
        } catch (_: Exception) {}
    }

    suspend fun getSavedCities(): List<CityLocation>? = withContext(ioDispatcher) {
        try {
            val citiesJson = store().data.map { prefs -> prefs[KEY_SAVED_CITIES] }.first() ?: return@withContext null
            json.decodeFromString<List<CityLocation>>(citiesJson)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLastUpdatedTime(): Long = withContext(ioDispatcher) {
        try {
            store().data.map { prefs -> prefs[KEY_LAST_UPDATED] ?: 0L }.first()
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * История дропов лутбокса (item 14): последние 20 наград в JSON.
     * Кодировку/декодировку держит LootboxHistoryCodec, кэш хранит строку —
     * так DataStore не зависит от модели награды.
     */
    suspend fun saveLootboxHistory(json: String) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                prefs[KEY_LOOTBOX_HISTORY] = json
            }
        } catch (_: Exception) {}
    }

    /** null = истории ещё нет (первый запуск) или DataStore недоступен. */
    suspend fun getLootboxHistory(): String? = withContext(ioDispatcher) {
        try {
            store().data.map { prefs -> prefs[KEY_LOOTBOX_HISTORY] }.first()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Счётчик потраченных бесплатных попыток лутбокса. Отдельный ключ, чтобы
     * лимит переживал перезапуск и не зависел от длины истории (она обрезается).
     * Отрицательное значение — бонусные попытки (додеп/реклама): контракт
     * `attemptsLeft = freeAttemptsTotal - attemptsUsed` от этого только растёт,
     * поэтому нижнюю границу здесь не отрезаем, только верхнюю по типу Int.
     */
    suspend fun saveLootboxAttemptsUsed(count: Int) = withContext(ioDispatcher) {
        try {
            store().edit { prefs ->
                prefs[KEY_LOOTBOX_ATTEMPTS_USED] = count
            }
        } catch (_: Exception) {}
    }

    /** 0 = попыток ещё не тратили или DataStore недоступен (безопасный дефолт). */
    suspend fun getLootboxAttemptsUsed(): Int = withContext(ioDispatcher) {
        try {
            store().data
                .map { prefs -> prefs[KEY_LOOTBOX_ATTEMPTS_USED] ?: 0 }
                .first()
        } catch (_: Exception) {
            0
        }
    }

    private companion object {
        /**
         * Реестр DataStore «один на файл на процесс» — замена семантики
         * прежнего делегата `preferencesDataStore`. Доступ только под мьютексом.
         */
        private val SINGLETON_MUTEX = Mutex()
        private val SINGLETON_INSTANCES = mutableMapOf<String, DataStore<Preferences>>()
    }
}

/** Последний реальный прогноз для конкретного города + время и источник. */
data class CachedForecast(
    val json: String,
    val savedAt: Long,
    val sourceId: String? = null
)

/** UI-настройки, переживающие force-stop. */
data class UiPreferences(
    val themeId: String? = null,
    val cloudsCleared: Boolean = false
)
