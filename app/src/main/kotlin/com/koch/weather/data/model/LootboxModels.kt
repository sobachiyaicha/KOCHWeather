package com.koch.weather.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    /** Погодный приз: реальный вид погоды из прогноза на завтра. */
    WEATHER_TOMORROW,
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
    /** WMO-код погоды из реального прогноза; null у не-погодных наград. */
    val weatherCode: Int? = null,
    /** «от +12° до +18°» — из реального суточного прогноза. */
    val temperatureRangeRu: String? = null,
    /** «завтра» или «сегодня» — согласовано с тем днём, на который выдан приз. */
    val dayLabelRu: String = "завтра",
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class LootboxState(
    val isOpening: Boolean = false,
    val lastReward: LootReward? = null,
    val history: List<LootReward> = emptyList(),
    val totalOpened: Int = 0,
    /**
     * Растёт на каждый дроп: UI ловит ровно один «выстрел» анимации/вибрации
     * по изменению значения, даже если награда совпала с предыдущей.
     */
    val lastRewardSeq: Int = 0,
    val unlockedThemes: Set<AppThemeMode> = setOf(
        AppThemeMode.BLUE_SKY,
        AppThemeMode.COPPER_SUNSET,
        AppThemeMode.FOREST,
        AppThemeMode.MARS
    )
) {
    /** Применяет выпавшую награду: история до 20, счётчик открытий и seq растут. */
    fun withDrop(reward: LootReward, themes: Set<AppThemeMode> = unlockedThemes): LootboxState =
        copy(
            isOpening = false,
            lastReward = reward,
            history = (listOf(reward) + history).take(MAX_HISTORY),
            totalOpened = totalOpened + 1,
            lastRewardSeq = lastRewardSeq + 1,
            unlockedThemes = themes
        )

    companion object {
        const val MAX_HISTORY: Int = 20
    }
}

/**
 * Сериализация истории дропов для DataStore. Хранит не больше 20 последних
 * наград; битый/пустой JSON не роняет приложение — история просто пустая.
 */
object LootboxHistoryCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(history: List<LootReward>): String =
        json.encodeToString(history.take(LootboxState.MAX_HISTORY))

    fun decode(raw: String?): List<LootReward> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<LootReward>>(raw).take(LootboxState.MAX_HISTORY)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
