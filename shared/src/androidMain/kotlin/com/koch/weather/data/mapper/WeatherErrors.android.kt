package com.koch.weather.data.mapper

import java.net.UnknownHostException

/**
 * Android: DNS-ошибка — java.net.UnknownHostException (на OkHttp приходит как
 * самостоятельное исключение или в cause-цепочке, напр. под Ktor).
 */
actual fun isUnknownHostFailure(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        if (current is UnknownHostException) return true
        current = current.cause
    }
    return false
}
