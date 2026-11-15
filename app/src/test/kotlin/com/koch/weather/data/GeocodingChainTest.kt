package com.koch.weather.data

import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.CitySearchResult
import com.koch.weather.data.model.WeatherErrorReason
import com.koch.weather.data.model.WeatherFailure
import com.koch.weather.data.provider.GeocodingChain
import com.koch.weather.data.provider.GeocodingProvider
import com.koch.weather.data.provider.GeocodingSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingChainTest {

    private class FakeGeocoding(
        override val source: GeocodingSource,
        private val result: CitySearchResult
    ) : GeocodingProvider {
        override suspend fun search(query: String): CitySearchResult = result
    }

    private class HangingGeocoding(
        override val source: GeocodingSource
    ) : GeocodingProvider {
        override suspend fun search(query: String): CitySearchResult {
            delay(60_000)
            return CitySearchResult.Success(emptyList())
        }
    }

    private val spb = CityLocation("Санкт-Петербург", 59.9386, 30.3141, "Россия")
    private val moscow = CityLocation("Москва", 55.7558, 37.6173, "Россия")

    @Test
    fun nonEmptyResultFromFirstProvider_isReturned() = runTest {
        val chain = GeocodingChain(
            listOf(
                FakeGeocoding(GeocodingSource.PHOTON, CitySearchResult.Success(listOf(spb))),
                FakeGeocoding(GeocodingSource.NOMINATIM, CitySearchResult.Success(listOf(moscow)))
            )
        )
        val result = chain.search("Петербург") as CitySearchResult.Success
        assertEquals(listOf(spb), result.cities)
    }

    @Test
    fun emptyResult_fallsThroughToNextProvider() = runTest {
        val chain = GeocodingChain(
            listOf(
                FakeGeocoding(GeocodingSource.PHOTON, CitySearchResult.Success(emptyList())),
                FakeGeocoding(GeocodingSource.NOMINATIM, CitySearchResult.Success(listOf(moscow)))
            )
        )
        val result = chain.search("Москва") as CitySearchResult.Success
        assertEquals(listOf(moscow), result.cities)
    }

    @Test
    fun failure_fallsThroughToNextProvider() = runTest {
        val chain = GeocodingChain(
            listOf(
                FakeGeocoding(
                    GeocodingSource.PHOTON,
                    CitySearchResult.Failure(WeatherFailure(WeatherErrorReason.NO_NETWORK, "photon down"))
                ),
                FakeGeocoding(GeocodingSource.NOMINATIM, CitySearchResult.Success(listOf(spb)))
            )
        )
        val result = chain.search("СПб") as CitySearchResult.Success
        assertEquals(listOf(spb), result.cities)
    }

    @Test
    fun allEmpty_returnsEmptySuccess_notError() = runTest {
        val chain = GeocodingChain(
            listOf(
                FakeGeocoding(GeocodingSource.PHOTON, CitySearchResult.Success(emptyList())),
                FakeGeocoding(GeocodingSource.NOMINATIM, CitySearchResult.Success(emptyList()))
            )
        )
        val result = chain.search("zzzz") as CitySearchResult.Success
        assertTrue(result.cities.isEmpty())
    }

    @Test
    fun allFail_returnsFailureWithReasons() = runTest {
        val chain = GeocodingChain(
            listOf(
                FakeGeocoding(
                    GeocodingSource.PHOTON,
                    CitySearchResult.Failure(WeatherFailure(WeatherErrorReason.NO_NETWORK, "photon down"))
                ),
                FakeGeocoding(
                    GeocodingSource.NOMINATIM,
                    CitySearchResult.Failure(WeatherFailure(WeatherErrorReason.TIMEOUT, "nominatim timeout"))
                )
            )
        )
        val result = chain.search("СПб")
        assertTrue(result is CitySearchResult.Failure)
        val message = (result as CitySearchResult.Failure).failure.messageRu
        assertTrue(message.contains("Photon"))
        assertTrue(message.contains("Nominatim"))
    }

    @Test
    fun searchBudget_returnsRussianTimeoutWithTime() = runTest {
        val chain = GeocodingChain(
            listOf(
                HangingGeocoding(GeocodingSource.PHOTON),
                HangingGeocoding(GeocodingSource.NOMINATIM)
            )
        )
        val result = chain.search("Санкт-Петербург")
        assertTrue(result is CitySearchResult.Failure)
        val failure = (result as CitySearchResult.Failure).failure
        assertEquals(WeatherErrorReason.TIMEOUT, failure.reason)
        assertTrue(failure.messageRu.contains("12"))
        assertTrue(failure.messageRu.contains("Поиск", ignoreCase = true))
        assertTrue(failure.messageWithTimeRu.contains("попытка в"))
    }

    @Test
    fun blankQuery_returnsEmptyWithoutNetwork() = runTest {
        val chain = GeocodingChain(emptyList())
        val result = chain.search("   ") as CitySearchResult.Success
        assertTrue(result.cities.isEmpty())
    }
}
