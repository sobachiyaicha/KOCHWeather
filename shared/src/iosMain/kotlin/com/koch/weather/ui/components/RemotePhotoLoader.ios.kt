@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.koch.weather.ui.components

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.koch.weather.data.model.ImageCatalog
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import kotlin.concurrent.atomics.AtomicReference

/**
 * iOS-actual: тот же состав, что на Android (Ktor-клиент с User-Agent
 * [ImageCatalog.WIKIMEDIA_USER_AGENT] — дословно строка Android-исходника;
 * движок — Darwin из iosMain-зависимостей). Default-конструктор Builder без
 * context: disk-кеш не настраивается — memory-кеш по умолчанию
 * (отличие от Coil 2 задокументировано в отчёте ui-porter). Процессный синглтон.
 */
private val sharedLoader = AtomicReference<ImageLoader?>(null)

actual fun wikimediaImageLoader(): ImageLoader {
    sharedLoader.load()?.let { return it }
    val client = HttpClient {
        install(UserAgent) { agent = ImageCatalog.WIKIMEDIA_USER_AGENT }
    }
    val loader = ImageLoader.Builder(PlatformContext.INSTANCE)
        .components {
            add(KtorNetworkFetcherFactory(httpClient = client))
        }
        .build()
    sharedLoader.compareAndSet(null, loader)
    return sharedLoader.load() ?: loader
}
