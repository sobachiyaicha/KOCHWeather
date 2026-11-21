@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package com.koch.weather.ui.components

import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.koch.weather.data.model.ImageCatalog
import com.koch.weather.platform.AndroidPlatform
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import kotlin.concurrent.atomics.AtomicReference

/**
 * Android-actual: 1:1 с исходным WikimediaImageLoader (OkHttp-интерцептор с
 * User-Agent → Ktor-клиент с плагином UserAgent; тот же процессный синглтон).
 * Coil 3 на Android требует context для default-кеша — берём appContext хоста.
 * Double-check через `synchronized` недоступен в common: при гонке может создаться
 * второй лоадер, лишний экземпляр отбрасывается (AtomicReference).
 */
private val sharedLoader = AtomicReference<ImageLoader?>(null)

actual fun wikimediaImageLoader(): ImageLoader {
    sharedLoader.load()?.let { return it }
    val context = AndroidPlatform.requireAppContext()
    val client = HttpClient {
        install(UserAgent) { agent = ImageCatalog.WIKIMEDIA_USER_AGENT }
    }
    val loader = ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient = client))
        }
        .build()
    sharedLoader.compareAndSet(null, loader)
    return sharedLoader.load() ?: loader
}
