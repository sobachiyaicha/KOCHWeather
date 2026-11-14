package com.koch.weather.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class LootRarity(val titleRu: String, val colorHex: Long) {
    COMMON("Обычный", 0xFF888888),
    RARE("Редкий", 0xFF2196F3),
    EPIC("Эпический", 0xFFAB47BC),
    LEGENDARY("Легендарный", 0xFFFFB300),
    MYTHIC("Мифический", 0xFFFF1744)
}

enum class LootRewardType {
    IPHONE,
    THEME,
    TOMORROW_WEATHER,
    UMBRELLA_DISCOUNT,
    RUBBER_BOOTS,
    COFFEE
}

@Immutable
@Serializable
data class LootReward(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: LootRewardType,
    val rarity: LootRarity,
    val unlockedThemeName: String? = null,
    val weatherForecastTomorrow: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class LootboxState(
    val isOpening: Boolean = false,
    val lastReward: LootReward? = null,
    val history: List<LootReward> = emptyList(),
    val totalOpened: Int = 0,
    val unlockedThemes: Set<AppThemeMode> = setOf(
        AppThemeMode.BLUE_SKY,
        AppThemeMode.COPPER_SUNSET,
        AppThemeMode.FOREST,
        AppThemeMode.MARS
    )
)
