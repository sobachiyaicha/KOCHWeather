package com.koch.weather.platform

import com.koch.weather.data.cache.cacheIoDispatcher
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path

/**
 * Единая точка сборки зависимостей для androidApp и iOS MainViewController.
 *
 * Конструктор без platform-типов: всё содержимое собирается из expect-функций
 * этого же пакета, поэтому контейнер объявляется в commonMain одинаково
 * на Android (`AppContainer()` из Application/MainActivity) и iOS
 * (`AppContainer()` из MainViewController.kt).
 *
 * HTTP-клиент общий для всех провайдеров: таймауты 6/10/12 с (01-APP-SPEC §7.1 —
 * connect/read/call) и единый User-Agent (MET Norway/OSM отвечают 403 на
 * дефолтные UA). Движок — [httpClientEngine]: OkHttp на Android, Darwin на iOS
 * (HttpTimeout поддерживается обоими движками).
 *
 * DataStore: путь [dataStoreFile] для
 * `PreferenceDataStoreFactory.createWithPath { dataStoreFile() }` (cache-porter).
 * Ключи и JSON — без изменений (01-APP-SPEC §8): пользовательские данные
 * переживают обновление, android-путь совпадает с прежним weather_cache.
 *
 * applicationScope — скоуп DataStore-актора (WeatherCache → PreferenceDataStoreFactory)
 * и фоновых задач репозиториев/синка городов; корутины переживают уход экрана,
 * но не живут дольше процесса. Диспетчер — [cacheIoDispatcher], НЕ общий
 * Dispatchers.Default (bug-hunter, phase 2): актор DataStore 1.1.1
 * (`SimpleActor.offer` → `scope.launch(consumer)`) обязан жить в одном пуле с
 * callerContext'ом всех `edit`/`data` (WeatherCache оборачивает каждый вызов
 * в `withContext(cacheIoDispatcher())`). Если актор rides общий Default,
 * потеря wakeup'а воркера на iOS 26.4 симуляторе замораживает consumer'а
 * навсегда — SimpleActor оставляет remainingMessages>0, новые `offer` больше
 * не стартуют consumer, все `edit` висят на `ack.await()` до конца процесса.
 * Android: cacheIoDispatcher() = Dispatchers.IO — ровно дефолтный scope
 * эталонного делегата `preferencesDataStore` (CoroutineScope(IO + SupervisorJob)).
 */
class AppContainer(
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + cacheIoDispatcher())
) {
    /** Полный путь к файлу Preferences DataStore (okio.Path — createWithPath требует его). */
    val dataStoreFile: Path by lazy { dataStoreFile() }

    /** Общий HTTP-клиент: таймауты и User-Agent одинаковы для всех провайдеров. */
    val httpClient: HttpClient by lazy {
        HttpClient(httpClientEngine()) {
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = READ_TIMEOUT_MILLIS
                requestTimeoutMillis = CALL_TIMEOUT_MILLIS
            }
            install(UserAgent) { agent = appUserAgent() }
        }
    }

    companion object {
        /** 01-APP-SPEC §7.1: таймауты Open-Meteo 6/10/12 с (connect/read/call). */
        const val CONNECT_TIMEOUT_MILLIS = 6_000L
        const val READ_TIMEOUT_MILLIS = 10_000L
        const val CALL_TIMEOUT_MILLIS = 12_000L
    }
}
