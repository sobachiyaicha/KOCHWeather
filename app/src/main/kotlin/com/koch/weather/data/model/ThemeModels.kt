package com.koch.weather.data.model

/**
 * Одна строка каталога тем для экрана «Вид» (R15).
 *
 * [isUnlocked] приходит только из LootboxRepository/DataStore. Каталог не
 * придумывает «выпавшие» темы: если тема не сохранена как разблокированная,
 * она показывается закрытой.
 */
data class ThemeCatalogItem(
    val theme: AppThemeMode,
    val titleRu: String,
    val isUnlocked: Boolean,
    val isLootboxExclusive: Boolean
)

object ThemeCatalog {

    /** Темы, доступные без лутбоксов. Это единственный источник правды для дефолтов. */
    val DEFAULT_UNLOCKED: Set<AppThemeMode> = setOf(
        AppThemeMode.BLUE_SKY,
        AppThemeMode.COPPER_SUNSET,
        AppThemeMode.FOREST,
        AppThemeMode.MARS
    )

    /** Темы, которые можно получить только из лутбокса. */
    val LOOTBOX_EXCLUSIVE: Set<AppThemeMode> =
        AppThemeMode.values().toSet() - DEFAULT_UNLOCKED

    fun build(unlocked: Set<AppThemeMode>): List<ThemeCatalogItem> =
        AppThemeMode.values().map { theme ->
            ThemeCatalogItem(
                theme = theme,
                titleRu = theme.titleRu,
                isUnlocked = theme in unlocked,
                isLootboxExclusive = theme in LOOTBOX_EXCLUSIVE
            )
        }

    /** Пул наград «новая тема»: только реально закрытые темы. */
    fun lootboxRewardPool(unlocked: Set<AppThemeMode>): List<AppThemeMode> =
        AppThemeMode.values().filter { it !in unlocked }
}
