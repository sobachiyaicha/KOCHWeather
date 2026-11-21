package com.koch.weather.data.repository

import com.koch.weather.data.cache.WeatherCache
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.LootReward
import com.koch.weather.data.model.LootboxAttemptsPolicy
import com.koch.weather.data.model.LootboxHistoryCodec
import com.koch.weather.data.model.LootboxPrizeFactory
import com.koch.weather.data.model.LootboxState
import com.koch.weather.data.model.ThemeCatalog
import com.koch.weather.data.model.ThemeCatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Лутбоксы: награда, разблокировка тем, история дропов и лимит попыток в DataStore.
 *
 * Сценарий дропов задаёт [LootboxPrizeFactory]: индекс приза = число уже
 * выпавших наград, поэтому первые четыре дропа (начиная с «мать с окна»)
 * идут строго по порядку, дальше — взвешенный пул. Бонусные попытки
 * (додеп, реклама) сценарий не сдвигают: индекс по-прежнему `history.size`.
 *
 * Погодный приз строится по реальному суточному прогнозу ([openLootbox]
 * принимает `DailyItem` завтрашнего дня). Если прогноза нет, погодный приз
 * не выпадает — веса пересчитываются в фабрике.
 *
 * Попытки: 5 бесплатных на пользователя, счётчик персистится отдельным ключом
 * DataStore (`lootbox_attempts_used`). Когда они кончились, [openLootbox]
 * возвращает null — без исключений; UI показывает «Попытки закончились».
 * Приз «шанс на додеп» возвращает попытку ([LootReward.grantsExtraAttempt]),
 * реклама даёт +1 попытку через [grantAdAttempt] (не больше
 * [LootboxAttemptsPolicy.AD_ATTEMPTS_MAX] бонусных). Премиум-пользователю
 * лимит не писан (флаг уже есть в кэше).
 *
 * История — последние 20 наград в `lootbox_history_json`: она переживает
 * перезапуск приложения, а не живёт в памяти процесса.
 */
class LootboxRepository(private val cache: WeatherCache) {

    /**
     * Крутит лутбокс. null — попытки исчерпаны: награда не выдаётся, история
     * и счётчик не меняются (исключений не бросаем).
     */
    suspend fun openLootbox(tomorrow: DailyItem?): LootReward? = withContext(Dispatchers.Default) {
        val isPremium = cache.isPremium()
        val usedBefore = cache.getLootboxAttemptsUsed()
        if (!LootboxAttemptsPolicy.canOpen(usedBefore, isPremium)) {
            return@withContext null
        }

        val history = getHistory()
        val unlockedBeforeRoll = getUnlockedThemes()
        val themePool = ThemeCatalog.lootboxRewardPool(unlockedBeforeRoll)
        val themeIndex = if (themePool.isEmpty()) 0 else Random.nextInt(themePool.size)
        val reward = LootboxPrizeFactory.draw(
            historySize = history.size,
            roll = Random.nextDouble(0.0, 100.0),
            tomorrow = tomorrow,
            themePool = themePool,
            themeIndex = themeIndex
        )

        reward.unlockedThemeName?.let { cache.unlockTheme(it) }

        // Попытка списывается только за реально выданный приз; додеп её возвращает.
        if (!isPremium) {
            cache.saveLootboxAttemptsUsed(
                LootboxAttemptsPolicy.afterDrop(
                    attemptsUsed = usedBefore,
                    isPremium = false,
                    grantsExtraAttempt = reward.grantsExtraAttempt
                )
            )
        }

        val updatedHistory =
            (listOf(reward) + history).take(LootboxState.MAX_HISTORY)
        cache.saveLootboxHistory(LootboxHistoryCodec.encode(updatedHistory))
        reward
    }

    /**
     * +1 попытка за просмотр рекламы (персистентно). Возвращает true, если
     * попытка начислена; false — бонусный запас уже выбран
     * ([LootboxAttemptsPolicy.AD_ATTEMPTS_MAX]). Сценарий призов не сдвигается:
     * индекс считается по истории, а не по счётчику попыток.
     */
    suspend fun grantAdAttempt(): Boolean = withContext(Dispatchers.Default) {
        val used = cache.getLootboxAttemptsUsed()
        if (!LootboxAttemptsPolicy.canGrantAdAttempt(used)) {
            return@withContext false
        }
        cache.saveLootboxAttemptsUsed(LootboxAttemptsPolicy.grantAdAttempt(used))
        true
    }

    /** Сколько бесплатных попыток уже потрачено (0 для новой установки, минус = бонусы). */
    suspend fun getAttemptsUsed(): Int = withContext(Dispatchers.Default) {
        cache.getLootboxAttemptsUsed()
    }

    /** Остались ли попытки у этого пользователя (премиум — всегда да). */
    suspend fun hasAttemptsLeft(): Boolean = withContext(Dispatchers.Default) {
        LootboxAttemptsPolicy.canOpen(cache.getLootboxAttemptsUsed(), cache.isPremium())
    }

    suspend fun getUnlockedThemes(): Set<AppThemeMode> = withContext(Dispatchers.Default) {
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
    suspend fun getHistory(): List<LootReward> = withContext(Dispatchers.Default) {
        LootboxHistoryCodec.decode(cache.getLootboxHistory())
    }
}
