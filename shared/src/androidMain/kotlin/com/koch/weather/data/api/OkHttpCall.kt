package com.koch.weather.data.api

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Асинхронный вызов OkHttp с отменой: при отмене корутины (например, общий
 * таймаут провайдеров или уход с экрана) Call.cancel() обрывает запрос.
 */
suspend fun OkHttpClient.executeAsync(request: Request): Response =
    suspendCancellableCoroutine { continuation ->
        val call: Call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) {
                    continuation.resume(response)
                } else {
                    response.close()
                }
            }
        })
    }
