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

    private class DelayedProvider(
        override val source: WeatherSource,
        private val delayMs: Long,
        private val result: ProviderFetchResult
    ) : WeatherProvider {
        override suspend fun fetchForecast(latitude: Double, longitude: Double): ProviderFetchResult {
            delay(delayMs)
            return result
        }
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
    fun fastMetWinsRace_againstSlowOpenMeteo() = runTest {
        val chain = WeatherProviderChain(
            listOf(
                DelayedProvider(
                    WeatherSource.MET_NO,
                    0,
                    ProviderFetchResult.Success(response, 111L)
                ),
                DelayedProvider(
                    WeatherSource.OPEN_METEO,
                    10_000,
                    ProviderFetchResult.Success(response, 222L)
                )
            )
        )
        val result = chain.fetch(59.9, 30.3) as WeatherProviderChainResult.Success
        assertEquals(WeatherSource.MET_NO, result.source)
        assertEquals(111L, result.fetchedAt)
    }

    @Test
    fun fastOpenMeteoWinsRace_againstSlowMet() = runTest {
        val chain = WeatherProviderChain(
            listOf(
                DelayedProvider(
                    WeatherSource.MET_NO,
                    10_000,
                    ProviderFetchResult.Success(response, 111L)
                ),
                DelayedProvider(
                    WeatherSource.OPEN_METEO,
                    0,
                    ProviderFetchResult.Success(response, 222L)
                )
            )
        )
        val result = chain.fetch(59.9, 30.3) as WeatherProviderChainResult.Success
        assertEquals(WeatherSource.OPEN_METEO, result.source)
        assertEquals(222L, result.fetchedAt)
    }

    @Test
    fun metNoFailure_doesNotBlockOpenMeteoSuccess() = runTest {
        val failure = WeatherFailure(WeatherErrorReason.TIMEOUT, "MET Norway не отвечает")
        val chain = WeatherProviderChain(
            listOf(
                FakeProvider(WeatherSource.MET_NO, ProviderFetchResult.Failure(failure)),
                FakeProvider(WeatherSource.OPEN_METEO, ProviderFetchResult.Success(response, 222L))
            )
        )
        val result = chain.fetch(59.9, 30.3) as WeatherProviderChainResult.Success
        assertEquals(WeatherSource.OPEN_METEO, result.source)
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
    fun bothConnectTimeouts_returnAllFailedWithoutWaitingBudget() = runTest {
        val chain = WeatherProviderChain(
            listOf(
                FakeProvider(
                    WeatherSource.MET_NO,
                    ProviderFetchResult.Failure(
                        WeatherFailure(WeatherErrorReason.CONNECT_TIMEOUT, "connect timed out")
                    )
                ),
                FakeProvider(
                    WeatherSource.OPEN_METEO,
                    ProviderFetchResult.Failure(
                        WeatherFailure(WeatherErrorReason.CONNECT_TIMEOUT, "connect timed out")
                    )
                )
            )
        )
        val result = chain.fetch(59.9, 30.3)
        assertTrue(result is WeatherProviderChainResult.AllFailed)
        val attempts = (result as WeatherProviderChainResult.AllFailed).failedAttempts
        assertEquals(2, attempts.size)
        assertTrue(attempts.all { it.failure.reason == WeatherErrorReason.CONNECT_TIMEOUT })
    }

    @Test
    fun overallTimeout_returnsAllFailedWithinThirtySeconds() = runTest {
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
        assertTrue(attempt.failure.messageRu.contains("30"))
        assertEquals(30_000L, WeatherProviderChain.TOTAL_TIMEOUT_MS)
    }

    @Test
    fun throwingProvider_doesNotBreakSuccessfulRace() = runTest {
        val chain = WeatherProviderChain(
            listOf(
                ThrowingProvider(WeatherSource.MET_NO),
                FakeProvider(WeatherSource.OPEN_METEO, ProviderFetchResult.Success(response, 333L))
            )
        )
        val result = chain.fetch(59.9, 30.3) as WeatherProviderChainResult.Success
        assertEquals(WeatherSource.OPEN_METEO, result.source)
    }
}
