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

        val reward = when {
            // 0.001% Mythic iPhone 16 Pro Max
            roll <= 0.001 -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "📱 iPhone 16 Pro Max (1 ТБ)",
                    subtitle = "НЕВЕРОЯТНО! Вы сорвали куш с шансом 0.001%! Заберите у курьера.",
                    type = LootRewardType.IPHONE,
                    rarity = LootRarity.MYTHIC,
                    iconEmoji = "📱"
                )
            }

            // ~15% New Themes
            roll in 0.001..15.0 -> {
                val themeRoll = Random.nextInt(4)
                val (theme, themeName, icon) = when (themeRoll) {
                    0 -> Triple(AppThemeMode.CYBERPUNK, "Неоновый киберпанк", "🌆")
                    1 -> Triple(AppThemeMode.AURORA_BOREALIS, "Северное сияние", "🌌")
                    2 -> Triple(AppThemeMode.BLOODY_SUNSET, "Кровавый закат", "🌅")
                    else -> Triple(AppThemeMode.FOREST, "Изумрудный лес", "🌲")
                }
                cache.unlockTheme(theme.name)
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "🎨 Тема: «$themeName»",
                    subtitle = "Новое эксклюзивное оформление разблокировано во вкладке «Вид»!",
                    type = LootRewardType.THEME,
                    rarity = LootRarity.LEGENDARY,
                    iconEmoji = icon,
                    unlockedThemeName = theme.name
                )
            }

            // ~25% Tomorrow Weather Forecast from Open-Meteo
            roll in 15.0..40.0 -> {
                val forecastDesc = tomorrowWeatherText ?: "Переменная облачность, без осадков"
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "🌧 Прогноз на завтра: $forecastDesc",
                    subtitle = "Вам выпал персональный реальный прогноз прямо со спутника Open-Meteo!",
                    type = LootRewardType.TOMORROW_WEATHER,
                    rarity = LootRarity.RARE,
                    iconEmoji = "🛰️",
                    weatherForecastTomorrow = forecastDesc
                )
            }

            // ~25% Umbrella discount
            roll in 40.0..65.0 -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "🌂 Промокод: Зонт со скидкой 50%",
                    subtitle = "Используйте промокод WEATHER2025 в магазине зонтиков!",
                    type = LootRewardType.UMBRELLA_DISCOUNT,
                    rarity = LootRarity.EPIC,
                    iconEmoji = "🌂"
                )
            }

            // ~20% Rubber boots
            roll in 65.0..85.0 -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "🧦 Резиновые сапоги «Стиль 2025»",
                    subtitle = "Непромокаемая обувь высшей категории. Лужи больше не страшны!",
                    type = LootRewardType.RUBBER_BOOTS,
                    rarity = LootRarity.COMMON,
                    iconEmoji = "🧦"
                )
            }

            // ~15% Hot coffee
            else -> {
                LootReward(
                    id = UUID.randomUUID().toString(),
                    title = "☕ Горячий капучино в Surf Coffee",
                    subtitle = "Согревающий напиток для идеальной дождливой погоды.",
                    type = LootRewardType.COFFEE,
                    rarity = LootRarity.COMMON,
                    iconEmoji = "☕"
                )
            }
        }

        history.add(0, reward)
        reward
    }

    suspend fun getUnlockedThemes(): Set<AppThemeMode> = withContext(Dispatchers.IO) {
        val saved = cache.getUnlockedThemes()
        val defaultSet = setOf(
            AppThemeMode.BLUE_SKY,
            AppThemeMode.COPPER_SUNSET,
            AppThemeMode.FOREST,
            AppThemeMode.MARS
        )
        val unlockedFromCache = saved.mapNotNull { name ->
            try {
                AppThemeMode.valueOf(name)
            } catch (_: Exception) {
                null
            }
        }.toSet()
        defaultSet + unlockedFromCache
    }

    fun getHistory(): List<LootReward> = history.toList()
}
