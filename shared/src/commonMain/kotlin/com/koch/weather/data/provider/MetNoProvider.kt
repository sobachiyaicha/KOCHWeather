package com.koch.weather.data.provider

import com.koch.weather.data.api.formatFixed
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherSource
import com.koch.weather.platform.nowMillis
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

/**
 * Основной источник: MET Norway locationforecast/2.0/compact.
 * api.met.no отвечает 403 на дефолтные User-Agent (okhttp/Dalvik/Java),
 * поэтому заголовок WEATHER_HTTP_USER_AGENT ставится явно на каждый запрос.
 *
 * При timeout выполняется ровно один повтор с увеличенным read-таймаутом
 * (connect 10s / read 25s): первый запрос после pm clear иногда не успевает.
 *
 * Замена OkHttp: два клиента [defaultClient]/[retryClient] на Ktor с теми же
 * таймаутами; ConcurrentHashMap → MutableMap + Mutex (единственная запись
 * [putCache] и чтение [fetchForecast] — корутинная, без блокирующих потоков).
 */
class MetNoProvider(
    private val client: HttpClient = defaultClient(),
    private val retryClient: HttpClient = retryClient(),
    private val policy: MetNoCachePolicy = MetNoCachePolicy(),
    private val clock: () -> Long = { nowMillis() },
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : WeatherProvider {

    override val source: WeatherSource = WeatherSource.MET_NO

    private data class CacheEntry(
        val response: WeatherApiResponse,
        val fetchedAt: Long
    )

    private val cacheMutex = Mutex()
    private val memoryCache = HashMap<String, CacheEntry>()

    override fun minRefreshIntervalMs(latitude: Double, longitude: Double): Long =
        policy.minRefreshIntervalMs(latitude, longitude)

    override suspend fun fetchForecast(
        latitude: Double,
        longitude: Double
    ): ProviderFetchResult = withContext(Dispatchers.Default) {
        val now = clock()
        val key = cacheKey(latitude, longitude)
        val cached = cacheMutex.withLock { memoryCache[key] }
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
        httpClient: HttpClient,
        key: String
    ): ProviderFetchResult {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact" +
            "?lat=${formatCoordinate(latitude)}&lon=${formatCoordinate(longitude)}"

        return try {
            val response = httpClient.get(url) {
                header("User-Agent", WEATHER_HTTP_USER_AGENT)
                header("Accept", "application/json")
            }
            if (!response.status.isSuccess()) {
                return ProviderFetchResult.Failure(
                    WeatherFailure(
                        WeatherErrorReason.HTTP_ERROR,
                        "MET Norway вернул ошибку (HTTP ${response.status.value})."
                    )
                )
            }
            val body = response.bodyAsText()
            if (body.isEmpty()) {
                return ProviderFetchResult.Failure(
                    WeatherFailure(
                        WeatherErrorReason.PARSE_ERROR,
                        "MET Norway вернул пустой ответ."
                    )
                )
            }
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val mapped = WeatherErrors.from(e)
            ProviderFetchResult.Failure(
                WeatherFailure(
                    reason = mapped.reason,
                    messageRu = "MET Norway: ${mapped.messageRu}"
                )
            )
        }
    }

    private fun shouldRetry(failure: WeatherFailure): Boolean =
        failure.reason == WeatherErrorReason.TIMEOUT

    private suspend fun putCache(key: String, entry: CacheEntry) {
        cacheMutex.withLock {
            if (memoryCache.size >= 100) {
                memoryCache.clear()
            }
            memoryCache[key] = entry
        }
    }

    private fun cacheKey(latitude: Double, longitude: Double): String =
        formatFixed(latitude, 4) + "," + formatFixed(longitude, 4)

    private fun formatCoordinate(value: Double): String = formatFixed(value, 4)

    companion object {
        // Таймауты 1:1 с OkHttp-версией. Константы вместо литералов: конфигурация
        // установленного HttpTimeout-плагина в ktor 3.6.1 не читается публично
        // (ClientPluginInstance.config — private), WeatherTimeoutConfigTest
        // проверяет именно эти числа.
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 6_000L
        const val DEFAULT_READ_TIMEOUT_MILLIS = 10_000L
        const val DEFAULT_CALL_TIMEOUT_MILLIS = 12_000L
        const val RETRY_CONNECT_TIMEOUT_MILLIS = 10_000L
        const val RETRY_READ_TIMEOUT_MILLIS = 25_000L
        const val RETRY_CALL_TIMEOUT_MILLIS = 35_000L

        fun defaultClient(): HttpClient = providerHttpClient(
            connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS,
            readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS,
            // Первая попытка короче, чтобы при cold-start timeout осталось время
            // на повтор в рамках общего 20-секундного предохранителя.
            callTimeoutMillis = DEFAULT_CALL_TIMEOUT_MILLIS
        )

        /** Повтор после timeout: увеличенный read timeout и уменьшенный connect. */
        fun retryClient(): HttpClient = providerHttpClient(
            connectTimeoutMillis = RETRY_CONNECT_TIMEOUT_MILLIS,
            readTimeoutMillis = RETRY_READ_TIMEOUT_MILLIS,
            callTimeoutMillis = RETRY_CALL_TIMEOUT_MILLIS
        )
    }
}
