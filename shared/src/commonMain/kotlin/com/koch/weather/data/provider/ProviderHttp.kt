package com.koch.weather.data.provider

import com.koch.weather.platform.httpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

/**
 * Ktor-замена OkHttp-билдеров провайдеров.
 *
 * Округление таймаутов 1:1 с исходником:
 *  - connectTimeout → connectTimeoutMillis;
 *  - readTimeout    → socketTimeoutMillis;
 *  - callTimeout    → requestTimeoutMillis;
 *  - writeTimeout   → аналога в Ktor нет; у провайдеров только GET без тела,
 *    поэтому write-лимит на поведение не влиял (OkHttp-движок keep-ает свой
 *    предохранитель из httpClientEngine()).
 *
 * Каждый провайдер получает собственный клиент на движке платформы
 * (OkHttp/Darwin), как раньше — собственный OkHttpClient.
 */
internal fun providerHttpClient(
    connectTimeoutMillis: Long,
    readTimeoutMillis: Long,
    callTimeoutMillis: Long
): HttpClient = HttpClient(httpClientEngine()) {
    install(HttpTimeout) {
        this.connectTimeoutMillis = connectTimeoutMillis
        this.socketTimeoutMillis = readTimeoutMillis
        this.requestTimeoutMillis = callTimeoutMillis
    }
}
