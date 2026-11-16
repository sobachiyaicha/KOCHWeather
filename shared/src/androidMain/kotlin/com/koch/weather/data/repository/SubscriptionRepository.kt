package com.koch.weather.data.repository

import com.koch.weather.data.cache.WeatherCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SubscriptionRepository(private val cache: WeatherCache) {

    val isPremiumFlow: Flow<Boolean> = cache.isPremiumFlow

    suspend fun isPremium(): Boolean = withContext(Dispatchers.IO) {
        cache.isPremium()
    }

    suspend fun purchasePremium(): Boolean = withContext(Dispatchers.IO) {
        cache.setPremium(true)
        true
    }

    suspend fun resetPremium(): Boolean = withContext(Dispatchers.IO) {
        cache.setPremium(false)
        false
    }
}
