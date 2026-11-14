package com.koch.weather.data

import com.koch.weather.data.model.WeatherApiResponse
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.model.WeatherSource
import com.koch.weather.data.provider.ProviderFetchResult
import com.koch.weather.data.provider.WeatherProvider
import com.koch.weather.data.provider.WeatherProviderChain
import com.koch.weather.data.provider.WeatherProviderChainResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderChainTest {

    private class FakeProvider(
        override val source: WeatherSource,
        private val result: ProviderFetchResult
    ) : WeatherProvider {
        override suspend fun fetchForecast(latitude: Double, longitude: Double) = result
    }

    private class ThrowingProvider(override val source: WeatherSource) : WeatherProvider {
        override suspend fun fetchForecast(latitude: Double, longitude: Double): ProviderFetchResult =
            throw IllegalStateException("boom")
    }

    private class HangingProvider(override val source: WeatherSource) : WeatherProvider {
        override suspend fun fetchForecast(latitude: Double, longitude: Double): ProviderFetchResult {
            delay(60_000)
            return ProviderFetchResult.Failure(
                WeatherFailure(WeatherErrorReason.UNKNOWN, "never")
            )
        }
    }

    private val response = WeatherApiResponse()

    @Test
    fun firstProviderSuccess_doesNotCallSecond() = runTest {
        val chain = WeatherProviderChain(
            listOf(
                FakeProvider(WeatherSource.MET_NO, ProviderFetchResult.Success(response, 111L)),
                FakeProvider(WeatherSource.OPEN_METEO, ProviderFetchResult.Success(response, 222L))
            )
        )
        val result = chain.fetch(59.9, 30.3) as WeatherProviderChainResult.Success
        assertEquals(WeatherSource.MET_NO, result.source)
        assertEquals(111L, result.fetchedAt)
        assertTrue(result.failedAttempts.isEmpty())
    }

    @Test
    fun metNoFailure_fallsBackToOpenMeteo_andKeepsReason() = runTest {
        val failure = WeatherFailure(WeatherErrorReason.TIMEOUT, "MET Norway не отвечает")
        val chain = WeatherProviderChain(
            listOf(
                FakeProvider(WeatherSource.MET_NO, ProviderFetchResult.Failure(failure)),
                FakeProvider(WeatherSource.OPEN_METEO, ProviderFetchResult.Success(response, 222L))
            )
        )
        val result = chain.fetch(59.9, 30.3) as WeatherProviderChainResult.Success
        assertEquals(WeatherSource.OPEN_METEO, result.source)
        assertEquals(1, result.failedAttempts.size)
        assertEquals(WeatherSource.MET_NO, result.failedAttempts.first().source)
        assertEquals(WeatherErrorReason.TIMEOUT, result.failedAttempts.first().failure.reason)
    }

    @Test
    fun allProvidersFail_returnsAllFailed() = runTest {
        val chain = WeatherProviderChain(
            listOf(
                FakeProvider(
                    WeatherSource.MET_NO,
                    ProviderFetchResult.Failure(WeatherFailure(WeatherErrorReason.NO_NETWORK, "met down"))
                ),
                FakeProvider(
                    WeatherSource.OPEN_METEO,
                    ProviderFetchResult.Failure(WeatherFailure(WeatherErrorReason.TIMEOUT, "open-meteo down"))
                )
            )
        )
        val result = chain.fetch(59.9, 30.3)
        assertTrue(result is WeatherProviderChainResult.AllFailed)
        assertEquals(2, (result as WeatherProviderChainResult.AllFailed).failedAttempts.size)
    }

    @Test
    fun overallTimeout_returnsAllFailedWithinTwentySeconds() = runTest {
        val chain = WeatherProviderChain(
            listOf(
                HangingProvider(WeatherSource.MET_NO),
                HangingProvider(WeatherSource.OPEN_METEO)
            )
        )
        val result = chain.fetch(59.9, 30.3)
        assertTrue(result is WeatherProviderChainResult.AllFailed)
        val attempt = (result as WeatherProviderChainResult.AllFailed).failedAttempts.first()
        assertEquals(WeatherErrorReason.TIMEOUT, attempt.failure.reason)
        assertTrue(attempt.failure.messageRu.contains("20"))
    }

    @Test
    fun throwingProvider_isCaughtAndNextProviderTried() = runTest {
        val chain = WeatherProviderChain(
            listOf(
                ThrowingProvider(WeatherSource.MET_NO),
                FakeProvider(WeatherSource.OPEN_METEO, ProviderFetchResult.Success(response, 333L))
            )
        )
        val result = chain.fetch(59.9, 30.3) as WeatherProviderChainResult.Success
        assertEquals(WeatherSource.OPEN_METEO, result.source)
        assertEquals(1, result.failedAttempts.size)
    }
}
