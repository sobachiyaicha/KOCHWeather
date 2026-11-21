package com.koch.weather.data.mapper

import com.koch.weather.data.api.OpenMeteoEmptyBodyException
import com.koch.weather.data.api.OpenMeteoHttpException
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.SerializationException

/**
 * Превращает сетевые/парсинг-ошибки в понятное русское состояние с повтором.
 * URL Open-Meteo не меняется — это единственный источник прогноза.
 *
 * Замена java.net/java.io: Ktor-исключения (на JVM это typealias java.net.SocketTimeoutException
 * и java.io.IOException — поведение 1:1 с Android-версией). Отдельного типа
 * UnknownHostException в Ktor 3 нет: java.net.UnknownHostException — подкласс
 * java.io.IOException, DNS-ошибку внутри IOException различает [isUnknownHostFailure].
 */
object WeatherErrors {

    fun from(error: Throwable?): WeatherFailure = when (error) {
        null -> WeatherFailure(
            WeatherErrorReason.UNKNOWN,
            "Не удалось получить прогноз. Повторите попытку."
        )

        is OpenMeteoHttpException -> WeatherFailure(
            WeatherErrorReason.HTTP_ERROR,
            "Сервер погоды вернул ошибку (HTTP ${error.code}). Попробуйте позже."
        )

        is OpenMeteoEmptyBodyException -> WeatherFailure(
            WeatherErrorReason.PARSE_ERROR,
            "Сервер погоды вернул пустой ответ. Повторите попытку."
        )

        // Ktor-аналог callTimeout (12 с): общий лимит запроса истёк.
        is HttpRequestTimeoutException -> WeatherFailure(
            WeatherErrorReason.TIMEOUT,
            "Сервер погоды не отвечает. Проверьте соединение и повторите."
        )

        is SocketTimeoutException -> {
            // Connect timeout не повторяем: если оба провайдера не установили
            // соединение, «нет данных» должно появиться сразу, а не через 30 с.
            val isConnectTimeout = error.message?.contains("connect", ignoreCase = true) == true
            if (isConnectTimeout) {
                WeatherFailure(
                    WeatherErrorReason.CONNECT_TIMEOUT,
                    "Не удалось подключиться к серверу погоды. Проверьте интернет и повторите."
                )
            } else {
                WeatherFailure(
                    WeatherErrorReason.TIMEOUT,
                    "Сервер погоды не отвечает. Проверьте соединение и повторите."
                )
            }
        }

        is ConnectTimeoutException -> WeatherFailure(
            WeatherErrorReason.NO_NETWORK,
            "Не удалось подключиться к серверу погоды. Проверьте интернет и повторите."
        )

        is SerializationException -> WeatherFailure(
            WeatherErrorReason.PARSE_ERROR,
            "Не удалось разобрать ответ сервера погоды. Повторите попытку."
        )

        is IOException ->
            if (isUnknownHostFailure(error)) {
                WeatherFailure(
                    WeatherErrorReason.NO_NETWORK,
                    "Нет соединения с сервером погоды. Проверьте интернет и повторите."
                )
            } else {
                WeatherFailure(
                    WeatherErrorReason.NO_NETWORK,
                    "Ошибка сети. Проверьте подключение и повторите."
                )
            }

        else -> WeatherFailure(
            WeatherErrorReason.UNKNOWN,
            "Не удалось получить прогноз. Повторите попытку."
        )
    }

    fun noNetwork(): WeatherFailure = WeatherFailure(
        WeatherErrorReason.NO_NETWORK,
        "Нет подключения к сети. Проверьте интернет и повторите."
    )

    fun invalidData(message: String): WeatherFailure =
        WeatherFailure(WeatherErrorReason.INVALID_DATA, message)
}
