package com.koch.weather.data.provider

import com.koch.weather.data.api.executeAsync
import com.koch.weather.data.mapper.WeatherErrors
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
 *
 * При timeout выполняется ровно один повтор с увеличенным read-таймаутом
 * (connect 10s / read 25s): первый запрос после pm clear иногда не успевает.
 */
class MetNoProvider(
    private val client: OkHttpClient = defaultClient(),
    private val retryClient: OkHttpClient = retryClient(),
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

        val first = executeRequest(latitude, longitude, now, client, key)
        if (first is ProviderFetchResult.Success) return@withContext first
        val failure = (first as ProviderFetchResult.Failure).failure
        if (shouldRetry(failure)) {
            return@withContext executeRequest(latitude, longitude, clock(), retryClient, key)
        }
        first
    }

    private suspend fun executeRequest(
        latitude: Double,
        longitude: Double,
        now: Long,
        httpClient: OkHttpClient,
        key: String
    ): ProviderFetchResult {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact" +
            "?lat=${formatCoordinate(latitude)}&lon=${formatCoordinate(longitude)}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", WEATHER_HTTP_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        return try {
            httpClient.executeAsync(request).use { response ->
                if (!response.isSuccessful) {
                    return ProviderFetchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.HTTP_ERROR,
                            "MET Norway вернул ошибку (HTTP ${response.code})."
                        )
                    )
                }
                val body = response.body?.string()
                    ?: return ProviderFetchResult.Failure(
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
                    return ProviderFetchResult.Failure(
                        WeatherFailure(
                            WeatherErrorReason.PARSE_ERROR,
                            e.message ?: "MET Norway: ошибка разбора."
                        )
                    )
                }
                putCache(key, CacheEntry(parsed, now))
                ProviderFetchResult.Success(response = parsed, fetchedAt = now)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val mapped = WeatherErrors.from(e)
            ProviderFetchResult.Failure(
                WeatherFailure(
                    reason = mapped.reason,
                    messageRu = "MET Norway недоступен: ${e.message ?: mapped.messageRu}."
                )
            )
        }
    }

    private fun shouldRetry(failure: WeatherFailure): Boolean =
        failure.reason == WeatherErrorReason.TIMEOUT

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
            // Первая попытка короче, чтобы при cold-start timeout осталось время
            // на повтор в рамках общего 20-секундного предохранителя.
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        /** Повтор после timeout: увеличенный read timeout и уменьшенный connect. */
        fun retryClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
