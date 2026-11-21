package com.koch.weather.data.repository

import com.koch.weather.data.model.CityLocation
import com.koch.weather.platform.nowMillis
import com.koch.weather.platform.trafficApiKey
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RealTrafficInfo(
    val score: Int?,
    val description: String,
    val iconColor: String? = null,
    val timestamp: Long = nowMillis(),
    val source: String = "Yandex Traffic"
)

/**
 * Пробки Яндекса (export.yandex.ru/bar/reginfo.xml). Замена HttpURLConnection:
 * Ktor GET с теми же таймаутами 4 с/4 с и тем же User-Agent; XML-парсинг
 * регулярками сохранён 1:1.
 *
 * Ключ [trafficApiKey] — TRAFFIC_API_KEY (iOS: пустая строка — port plan §4.4);
 * сам ключ в URL не подставлялся и в Android-версии, поведение без него
 * «Нет данных о пробках» сохранено.
 */
class TrafficRepository(
    private val apiKey: String = trafficApiKey()
) {

    private data class CacheEntry(
        val info: RealTrafficInfo,
        val savedAt: Long
    )

    private val cacheMutex = Mutex()
    private val memoryCache = HashMap<Int, CacheEntry>()
    private val cacheTtlMillis = 10 * 60 * 1000L // 10 minutes cache

    private val client: HttpClient by lazy {
        HttpClient(com.koch.weather.platform.httpClientEngine()) {
            install(HttpTimeout) {
                connectTimeoutMillis = 4000
                socketTimeoutMillis = 4000
                requestTimeoutMillis = 4000
            }
        }
    }

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

    private val levelPattern = Regex("<level>(\\d+)</level>")
    private val hintRuPattern = Regex("<hint lang=\"ru\">([^<]+)</hint>")
    private val hintGenericPattern = Regex("<hint>([^<]+)</hint>")
    private val iconPattern = Regex("<icon>([^<]+)</icon>")

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

        // Coordinate fallback только для городов с реальным регионом Яндекса:
        // СПб (2) и Москва (213). Остальные честно получают «нет данных».
        if (city.latitude in 59.6..60.2 && city.longitude in 29.7..30.8) return 2
        if (city.latitude in 55.4..56.1 && city.longitude in 37.1..38.0) return 213

        return null
    }

    /**
     * Fetches live traffic data. If no data exists for the city or if network fails,
     * honestly returns score = null and description = "Нет данных о пробках".
     */
    suspend fun getTraffic(city: CityLocation): RealTrafficInfo = withContext(Dispatchers.Default) {
        val regionId = resolveRegionId(city)
            ?: return@withContext RealTrafficInfo(
                score = null,
                description = "Нет данных о пробках",
                iconColor = null,
                source = "None"
            )

        // Check cache first
        val now = nowMillis()
        val cached = cacheMutex.withLock { memoryCache[regionId] }
        if (cached != null && (now - cached.savedAt) < cacheTtlMillis) {
            return@withContext cached.info
        }

        try {
            val response = client.get("https://export.yandex.ru/bar/reginfo.xml?region=$regionId") {
                header("User-Agent", "WeatherApp/1.0 (Android; Clean-Core)")
            }
            if (response.status.isSuccess()) {
                val responseXml = response.bodyAsText()
                val parsed = parseXml(responseXml)
                if (parsed != null) {
                    cacheMutex.withLock { memoryCache[regionId] = CacheEntry(parsed, now) }
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

        val score = levelPattern.find(xml)?.groupValues?.getOrNull(1)?.toIntOrNull()

        val hint: String? = hintRuPattern.find(xml)?.groupValues?.getOrNull(1)?.trim()
            ?: hintGenericPattern.find(xml)?.groupValues?.getOrNull(1)?.trim()

        val iconColor = iconPattern.find(xml)?.groupValues?.getOrNull(1)?.trim()

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
            timestamp = nowMillis(),
            source = "Yandex Traffic"
        )
    }
}
