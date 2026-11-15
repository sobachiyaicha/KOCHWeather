package com.koch.weather.data.repository

import com.koch.weather.data.cache.WeatherCache
import com.koch.weather.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Лутбоксы: награда, разблокировка тем и история дропов в DataStore.
 *
 * Погодный приз строится по реальному суточному прогнозу ([openLootbox] принимает
 * `DailyItem` завтрашнего дня). Если прогноза нет, погодный приз не выпадает —
 * остальные веса пересчитываются в [LootboxPrizeFactory].
 *
 * История — последние 20 наград в `lootbox_history_json`: она переживает
 * перезапуск приложения, а не живёт в памяти процесса.
 */
class LootboxRepository(private val cache: WeatherCache) {

    suspend fun openLootbox(tomorrow: DailyItem?): LootReward = withContext(Dispatchers.IO) {
        val unlockedBeforeRoll = getUnlockedThemes()
        val themePool = ThemeCatalog.lootboxRewardPool(unlockedBeforeRoll)
        val themeIndex = if (themePool.isEmpty()) 0 else Random.nextInt(themePool.size)
        val reward = LootboxPrizeFactory.draw(
            roll = Random.nextDouble(0.0, 100.0),
            tomorrow = tomorrow,
            themePool = themePool,
            themeIndex = themeIndex
        )

        reward.unlockedThemeName?.let { cache.unlockTheme(it) }

        val updatedHistory =
            (listOf(reward) + getHistory()).take(LootboxState.MAX_HISTORY)
        cache.saveLootboxHistory(LootboxHistoryCodec.encode(updatedHistory))
        reward
    }

    suspend fun getUnlockedThemes(): Set<AppThemeMode> = withContext(Dispatchers.IO) {
        val saved = cache.getUnlockedThemes()
        val unlockedFromCache = saved.mapNotNull { name ->
            try {
                AppThemeMode.valueOf(name)
            } catch (_: Exception) {
                null
            }
        }.toSet()
        ThemeCatalog.DEFAULT_UNLOCKED + unlockedFromCache
    }

    /** Каталог для экрана «Вид»: isUnlocked только из DataStore + дефолтные темы. */
    suspend fun getThemeCatalog(): List<ThemeCatalogItem> =
        ThemeCatalog.build(getUnlockedThemes())

    /** История дропов из DataStore, последние [LootboxState.MAX_HISTORY] штук. */
    suspend fun getHistory(): List<LootReward> = withContext(Dispatchers.IO) {
        LootboxHistoryCodec.decode(cache.getLootboxHistory())
    }
}
