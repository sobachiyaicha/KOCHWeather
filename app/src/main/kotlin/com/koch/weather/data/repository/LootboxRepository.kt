package com.koch.weather.data.repository

import com.koch.weather.data.cache.WeatherCache
import com.koch.weather.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

class LootboxRepository(private val cache: WeatherCache) {

    private val history = mutableListOf<LootReward>()

    suspend fun openLootbox(tomorrowWeatherText: String?): LootReward = withContext(Dispatchers.IO) {
        val roll = Random.nextDouble() * 100.0
        val unlockedBeforeRoll = getUnlockedThemes()
        val themePool = ThemeCatalog.lootboxRewardPool(unlockedBeforeRoll)

        val reward = when {
            // 0.001% Mythic iPhone 16 Pro Max
            roll <= 0.001 -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "iPhone 16 Pro Max (1 ТБ)",
                    subtitle = "НЕВЕРОЯТНО! Вы сорвали куш с шансом 0.001%! Заберите у курьера.",
                    type = LootRewardType.IPHONE,
                    rarity = LootRarity.MYTHIC,
                )
            }

            // ~15% New Themes: только реально закрытые темы, без повторных «выпадений».
            roll in 0.001..15.0 && themePool.isNotEmpty() -> {
                val theme = themePool[Random.nextInt(themePool.size)]
                cache.unlockTheme(theme.name)
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "Тема: «${theme.titleRu}»",
                    subtitle = "Новое эксклюзивное оформление разблокировано во вкладке «Вид»!",
                    type = LootRewardType.THEME,
                    rarity = LootRarity.LEGENDARY,
                    unlockedThemeName = theme.name
                )
            }

            // ~25% Tomorrow Weather Forecast: только реальный прогноз, без заглушки.
            roll in 15.0..40.0 && tomorrowWeatherText != null -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "Прогноз на завтра: $tomorrowWeatherText",
                    subtitle = "Вам выпал персональный реальный прогноз прямо со спутника Open-Meteo!",
                    type = LootRewardType.TOMORROW_WEATHER,
                    rarity = LootRarity.RARE,
                    weatherForecastTomorrow = tomorrowWeatherText
                )
            }

            // ~25% Umbrella discount
            roll in 40.0..65.0 -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "Промокод: Зонт со скидкой 50%",
                    subtitle = "Используйте промокод WEATHER2025 в магазине зонтиков!",
                    type = LootRewardType.UMBRELLA_DISCOUNT,
                    rarity = LootRarity.EPIC,
                )
            }

            // ~20% Rubber boots
            roll in 65.0..85.0 -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "Резиновые сапоги «Стиль 2025»",
                    subtitle = "Непромокаемая обувь высшей категории. Лужи больше не страшны!",
                    type = LootRewardType.RUBBER_BOOTS,
                    rarity = LootRarity.COMMON,
                )
            }

            // ~15% Hot coffee
            else -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "Горячий капучино в Surf Coffee",
                    subtitle = "Согревающий напиток для идеальной дождливой погоды.",
                    type = LootRewardType.COFFEE,
                    rarity = LootRarity.COMMON,
                )
            }
        }

        history.add(0, reward)
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

    fun getHistory(): List<LootReward> = history.toList()
}
