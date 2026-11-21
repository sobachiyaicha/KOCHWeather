package com.koch.weather.data.mapper

import io.ktor.client.engine.darwin.DarwinHttpRequestException

/**
 * iOS: Darwin-движок оборачивает NSError в DarwinHttpRequestException (подкласс
 * IOException). DNS-ошибка — NSURLErrorCannotFindHost == -1003; это макрос из
 * NSURLError.h, в Kotlin/Native-биндингах константы нет, поэтому числовое значение.
 */
actual fun isUnknownHostFailure(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        val darwin = current as? DarwinHttpRequestException
        if (darwin != null &&
            darwin.origin.domain == "NSURLErrorDomain" &&
            darwin.origin.code == -1003L
        ) {
            return true
        }
        current = current.cause
    }
    return false
}
