package com.koch.weather.data.repository

import com.koch.weather.BuildConfig
import com.koch.weather.data.model.CityLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

data class RealTrafficInfo(
    val score: Int?,
    val description: String,
    val iconColor: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "Yandex Traffic"
)

class TrafficRepository(
    private val apiKey: String = BuildConfig.TRAFFIC_API_KEY
) {

    private data class CacheEntry(
        val info: RealTrafficInfo,
        val savedAt: Long
    )

    private val memoryCache = ConcurrentHashMap<Int, CacheEntry>()
    private val cacheTtlMillis = 10 * 60 * 1000L // 10 minutes cache

    // Map of canonical Russian/CIS cities to Yandex Traffic region IDs
    private val cityToRegionMap = mapOf(
        "санкт-петербург" to 2,
        "санкт петербург" to 2,
        "петербург" to 2,
        "питер" to 2,
        "saint petersburg" to 2,
        "st. petersburg" to 2,
        "spb" to 2,

        "москва" to 213,
        "moscow" to 213,

        "минск" to 157,
        "minsk" to 157,

        "екатеринбург" to 54,
        "новосибирск" to 65,
        "казань" to 43,
        "нижний новгород" to 47,
        "самара" to 51,
        "ростов-на-дону" to 39,
        "ростов на дону" to 39,
        "ростов" to 39,
        "уфа" to 172,
        "красноярск" to 62,
        "воронеж" to 193,
        "пермь" to 50,
        "волгоград" to 38,
        "краснодар" to 35,
        "челябинск" to 56,
        "омск" to 66,
        "тюмень" to 55,
        "сочи" to 239
    )

    private val levelPattern = Pattern.compile("<level>(\\d+)</level>")
    private val hintRuPattern = Pattern.compile("<hint lang=\"ru\">([^<]+)</hint>")
    private val hintGenericPattern = Pattern.compile("<hint>([^<]+)</hint>")
    private val iconPattern = Pattern.compile("<icon>([^<]+)</icon>")

    /**
     * Resolves the Yandex region ID for a given city location.
     * Returns null if the city is not covered by traffic tracking (e.g. Dubai, international cities).
     */
    fun resolveRegionId(city: CityLocation): Int? {
        val normalized = city.name.trim().lowercase()
        cityToRegionMap[normalized]?.let { return it }

        for ((key, regionId) in cityToRegionMap) {
            if (normalized.contains(key) || key.contains(normalized)) {
                return regionId
            }
        }

        // Coordinate fallback for SPb, Moscow, Minsk
        if (city.latitude in 59.6..60.2 && city.longitude in 29.7..30.8) return 2
        if (city.latitude in 55.4..56.1 && city.longitude in 37.1..38.0) return 213
        if (city.latitude in 53.7..54.1 && city.longitude in 27.3..27.8) return 157

        return null
    }

    /**
     * Fetches live traffic data. If no data exists for the city or if network fails,
     * honestly returns score = null and description = "Нет данных о пробках".
     */
    suspend fun getTraffic(city: CityLocation): RealTrafficInfo = withContext(Dispatchers.IO) {
        val regionId = resolveRegionId(city)
            ?: return@withContext RealTrafficInfo(
                score = null,
                description = "Нет данных о пробках",
                iconColor = null,
                source = "None"
            )

        // Check cache first
        val now = System.currentTimeMillis()
        val cached = memoryCache[regionId]
        if (cached != null && (now - cached.savedAt) < cacheTtlMillis) {
            return@withContext cached.info
        }

        try {
            val urlString = "https://export.yandex.ru/bar/reginfo.xml?region=$regionId"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "WeatherApp/1.0 (Android; Clean-Core)")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseXml = connection.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
                val parsed = parseXml(responseXml)
                if (parsed != null) {
                    memoryCache[regionId] = CacheEntry(parsed, now)
                    return@withContext parsed
                }
            }
        } catch (_: Exception) {
            // Network error: fall back to cached data if available, even if expired
            if (cached != null) {
                return@withContext cached.info.copy(source = "Cache (Offline)")
            }
        }

        // Return honest no-data when network fails or XML has no traffic
        RealTrafficInfo(
            score = null,
            description = "Нет данных о пробках",
            iconColor = null,
            source = "None"
        )
    }

    fun parseXml(xml: String): RealTrafficInfo? {
        if (xml.contains("<traffic/>") || !xml.contains("<traffic")) {
            return RealTrafficInfo(
                score = null,
                description = "Нет данных о пробках",
                iconColor = null
            )
        }

        val levelMatcher = levelPattern.matcher(xml)
        val score = if (levelMatcher.find()) {
            levelMatcher.group(1)?.toIntOrNull()
        } else {
            null
        }

        val hintRuMatcher = hintRuPattern.matcher(xml)
        val hint: String? = if (hintRuMatcher.find()) {
            hintRuMatcher.group(1)?.trim()
        } else {
            val hintGenMatcher = hintGenericPattern.matcher(xml)
            if (hintGenMatcher.find()) hintGenMatcher.group(1)?.trim() else null
        }

        val iconMatcher = iconPattern.matcher(xml)
        val iconColor = if (iconMatcher.find()) iconMatcher.group(1)?.trim() else null

        val description = when {
            hint != null && hint.isNotBlank() -> hint
            score != null -> when (score) {
                in 1..2 -> "Дороги свободны"
                in 3..4 -> "Местами затруднения"
                in 5..6 -> "Движение плотное"
                in 7..8 -> "Серьезные пробки"
                in 9..10 -> "Город стоит"
                else -> "Движение в норме"
            }
            else -> "Нет данных о пробках"
        }

        return RealTrafficInfo(
            score = score,
            description = description,
            iconColor = iconColor,
            timestamp = System.currentTimeMillis(),
            source = "Yandex Traffic"
        )
    }
}
