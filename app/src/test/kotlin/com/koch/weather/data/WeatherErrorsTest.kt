package com.koch.weather.data

import com.koch.weather.data.api.OpenMeteoHttpException
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.WeatherErrorReason
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class WeatherErrorsTest {

    @Test
    fun timeout_mapsToTimeoutMessage() {
        val failure = WeatherErrors.from(SocketTimeoutException("timeout"))
        assertEquals(WeatherErrorReason.TIMEOUT, failure.reason)
        assertTrue(failure.messageRu.contains("не отвечает"))
        assertTrue(failure.messageRu.contains("повторите", ignoreCase = true))
    }

    @Test
    fun unknownHost_mapsToNoNetwork() {
        val failure = WeatherErrors.from(UnknownHostException("api.met.no"))
        assertEquals(WeatherErrorReason.NO_NETWORK, failure.reason)
        assertTrue(failure.messageRu.contains("интернет"))
    }

    @Test
    fun httpError_containsStatusCode() {
        val failure = WeatherErrors.from(OpenMeteoHttpException(503))
        assertEquals(WeatherErrorReason.HTTP_ERROR, failure.reason)
        assertTrue(failure.messageRu.contains("503"))
    }

    @Test
    fun serializationError_mapsToParseError() {
        val failure = WeatherErrors.from(SerializationException("bad json"))
        assertEquals(WeatherErrorReason.PARSE_ERROR, failure.reason)
        assertTrue(failure.messageRu.contains("разобрать"))
    }

    @Test
    fun genericIoError_mapsToNetwork() {
        val failure = WeatherErrors.from(IOException("broken pipe"))
        assertEquals(WeatherErrorReason.NO_NETWORK, failure.reason)
    }

    @Test
    fun nullError_mapsToUnknown() {
        val failure = WeatherErrors.from(null)
        assertEquals(WeatherErrorReason.UNKNOWN, failure.reason)
        assertTrue(failure.messageRu.isNotBlank())
    }

    @Test
    fun noNetwork_hasOfflineText() {
        val failure = WeatherErrors.noNetwork()
        assertEquals(WeatherErrorReason.NO_NETWORK, failure.reason)
        assertTrue(failure.messageRu.contains("Нет подключения"))
    }
}
