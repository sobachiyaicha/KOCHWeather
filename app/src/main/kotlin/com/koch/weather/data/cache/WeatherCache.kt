package com.koch.weather.data.cache

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.koch.weather.data.model.CityLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "weather_cache")

class WeatherCache(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val KEY_LAST_WEATHER = stringPreferencesKey("last_weather_json")
    private val KEY_LAST_CITY = stringPreferencesKey("last_city_json")
    private val KEY_SAVED_CITIES = stringPreferencesKey("saved_cities_json")
    private val KEY_LAST_UPDATED = longPreferencesKey("last_updated_time")
    private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium_purchased")
    private val KEY_UNLOCKED_THEMES = stringSetPreferencesKey("unlocked_themes_set")

    private fun cityKey(cityName: String) =
        stringPreferencesKey("weather_${cityName.trim().lowercase(Locale.ROOT).replace(" ", "_")}")

    val isPremiumFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_PREMIUM] ?: false
    }

    suspend fun setPremium(purchased: Boolean) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_IS_PREMIUM] = purchased
            }
        } catch (_: Exception) {}
    }

    suspend fun isPremium(): Boolean = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { it[KEY_IS_PREMIUM] ?: false }.first()
        } catch (_: Exception) {
            false
        }
    }

    val unlockedThemesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_UNLOCKED_THEMES] ?: emptySet()
    }

    suspend fun unlockTheme(themeKey: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { prefs ->
                val current = prefs[KEY_UNLOCKED_THEMES] ?: emptySet()
                prefs[KEY_UNLOCKED_THEMES] = current + themeKey
            }
        } catch (_: Exception) {}
    }

    suspend fun getUnlockedThemes(): Set<String> = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { it[KEY_UNLOCKED_THEMES] ?: emptySet() }.first()
        } catch (_: Exception) {
            emptySet()
        }
    }

    suspend fun saveForecast(city: CityLocation, jsonString: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_LAST_WEATHER] = jsonString
                prefs[KEY_LAST_CITY] = json.encodeToString(city)
                prefs[KEY_LAST_UPDATED] = System.currentTimeMillis()
                prefs[cityKey(city.name)] = jsonString
            }
        } catch (_: Exception) {}
    }

    suspend fun getForecastForCity(cityName: String): String? = withContext(Dispatchers.IO) {
        try {
            val key = cityKey(cityName)
            context.dataStore.data.map { prefs -> prefs[key] }.first()
                ?: getLastForecastJson()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLastForecastJson(): String? = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { prefs -> prefs[KEY_LAST_WEATHER] }.first()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLastCity(): CityLocation? = withContext(Dispatchers.IO) {
        try {
            val cityJson = context.dataStore.data.map { prefs -> prefs[KEY_LAST_CITY] }.first() ?: return@withContext null
            json.decodeFromString<CityLocation>(cityJson)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCitiesList(cities: List<CityLocation>) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_SAVED_CITIES] = json.encodeToString(cities)
            }
        } catch (_: Exception) {}
    }

    suspend fun getSavedCities(): List<CityLocation>? = withContext(Dispatchers.IO) {
        try {
            val citiesJson = context.dataStore.data.map { prefs -> prefs[KEY_SAVED_CITIES] }.first() ?: return@withContext null
            json.decodeFromString<List<CityLocation>>(citiesJson)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getLastUpdatedTime(): Long = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { prefs -> prefs[KEY_LAST_UPDATED] ?: 0L }.first()
        } catch (_: Exception) {
            0L
        }
    }
}
