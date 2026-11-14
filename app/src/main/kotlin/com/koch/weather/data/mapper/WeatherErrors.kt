package com.koch.weather.data.mapper

import com.koch.weather.data.api.OpenMeteoEmptyBodyException
import com.koch.weather.data.api.OpenMeteoHttpException
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Превращает сетевые/парсинг-ошибки в понятное русское состояние с повтором.
 * URL Open-Meteo не меняется — это единственный источник прогноза.
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

        is SocketTimeoutException -> WeatherFailure(
            WeatherErrorReason.TIMEOUT,
            "Сервер погоды не отвечает. Проверьте соединение и повторите."
        )

        is UnknownHostException -> WeatherFailure(
            WeatherErrorReason.NO_NETWORK,
            "Нет соединения с сервером погоды. Проверьте интернет и повторите."
        )

        is ConnectException -> WeatherFailure(
            WeatherErrorReason.NO_NETWORK,
            "Не удалось подключиться к серверу погоды. Проверьте интернет и повторите."
        )

        is SerializationException -> WeatherFailure(
            WeatherErrorReason.PARSE_ERROR,
            "Не удалось разобрать ответ сервера погоды. Повторите попытку."
        )

        is IOException -> WeatherFailure(
            WeatherErrorReason.NO_NETWORK,
            "Ошибка сети. Проверьте подключение и повторите."
        )

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
