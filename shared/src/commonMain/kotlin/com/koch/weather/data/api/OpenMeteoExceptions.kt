package com.koch.weather.data.api

import io.ktor.utils.io.errors.IOException

/** HTTP-ошибка Open-Meteo (4xx/5xx). */
class OpenMeteoHttpException(val code: Int) : IOException("Open-Meteo HTTP $code")

/** Сервер вернул пустое тело ответа. */
class OpenMeteoEmptyBodyException : IOException("Open-Meteo returned an empty body")
