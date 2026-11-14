package com.koch.weather.data.provider

import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Основной источник: MET Norway locationforecast/2.0/compact.
 * api.met.no отвечает 403 на дефолтные User-Agent (okhttp/Dalvik/Java),
 * поэтому заголовок WEATHER_HTTP_USER_AGENT ставится явно на каждый запрос.
 */
class MetNoProvider(
    private val client: OkHttpClient = defaultClient(),
    private val policy: MetNoCachePolicy = MetNoCachePolicy(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val timeZone: () -> TimeZone = { TimeZone.getDefault() }
) : WeatherProvider {

    override val source: WeatherSource = WeatherSource.MET_NO

    private data class CacheEntry(
        val response: WeatherApiResponse,
        val fetchedAt: Long
    )

    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    override fun minRefreshIntervalMs(latitude: Double, longitude: Double): Long =
        policy.minRefreshIntervalMs(latitude, longitude)

    /** Решение «отдать DataStore-кэш без запроса» по политике MET Norway. */
    fun shouldUseCachedForecast(
        sourceId: String?,
        savedAt: Long,
        now: Long,
        latitude: Double,
        longitude: Double
    ): Boolean = policy.shouldUseCache(sourceId, savedAt, now, latitude, longitude)

    override suspend fun fetchForecast(
        latitude: Double,
        longitude: Double
    ): ProviderFetchResult = withContext(Dispatchers.IO) {
        val now = clock()
        val key = cacheKey(latitude, longitude)
        val cached = memoryCache[key]
        if (cached != null && now - cached.fetchedAt < minRefreshIntervalMs(latitude, longitude)) {
            return@withContext ProviderFetchResult.Success(
                response = cached.response,
                fetchedAt = cached.fetchedAt,
                fromProviderCache = true
            )
        }

        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact" +
            "?lat=${formatCoordinate(latitude)}&lon=${formatCoordinate(longitude)}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", WEATHER_HTTP_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ProviderFetchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.HTTP_ERROR,
                            "MET Norway вернул ошибку (HTTP ${response.code})."
                        )
                    )
                }
                val body = response.body?.string()
                    ?: return@withContext ProviderFetchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.PARSE_ERROR,
                            "MET Norway вернул пустой ответ."
                        )
                    )
                val parsed = try {
                    MetNoForecastParser.parse(
                        body = body,
                        nowMillis = now,
                        latitude = latitude,
                        longitude = longitude,
                        timeZone = timeZone()
                    )
                } catch (e: MetNoParseException) {
                    return@withContext ProviderFetchResult.Failure(
                        WeatherFailure(WeatherErrorReason.PARSE_ERROR, e.message ?: "MET Norway: ошибка разбора.")
                    )
                }
                putCache(key, CacheEntry(parsed, now))
                ProviderFetchResult.Success(response = parsed, fetchedAt = now)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ProviderFetchResult.Failure(
                WeatherFailure(
                    WeatherErrorReason.NO_NETWORK,
                    "MET Norway недоступен: ${e.message ?: "ошибка сети"}."
                )
            )
        }
    }

    private fun putCache(key: String, entry: CacheEntry) {
        if (memoryCache.size >= 100) {
            memoryCache.clear()
        }
        memoryCache[key] = entry
    }

    private fun cacheKey(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.4f,%.4f", latitude, longitude)

    private fun formatCoordinate(value: Double): String =
        String.format(Locale.US, "%.4f", value)

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
