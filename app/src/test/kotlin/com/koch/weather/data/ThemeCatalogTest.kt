package com.koch.weather.data

import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.ThemeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeCatalogTest {

    @Test
    fun build_returnsEveryThemeOnce_withHonestFlags() {
        val catalog = ThemeCatalog.build(ThemeCatalog.DEFAULT_UNLOCKED)
        assertEquals(AppThemeMode.values().size, catalog.size)
        assertEquals(AppThemeMode.values().toSet(), catalog.map { it.theme }.toSet())

        val unlocked = catalog.filter { it.isUnlocked }.map { it.theme }.toSet()
        assertEquals(ThemeCatalog.DEFAULT_UNLOCKED, unlocked)
        assertEquals(4, unlocked.size)

        val forest = catalog.first { it.theme == AppThemeMode.FOREST }
        assertTrue(forest.isUnlocked)
        assertFalse(forest.isLootboxExclusive)

        val cyberpunk = catalog.first { it.theme == AppThemeMode.CYBERPUNK }
        assertFalse(cyberpunk.isUnlocked)
        assertTrue(cyberpunk.isLootboxExclusive)
    }

    @Test
    fun build_withEmptyUnlocked_marksEverythingLocked() {
        val catalog = ThemeCatalog.build(emptySet())
        assertTrue(catalog.all { !it.isUnlocked })
        assertEquals(AppThemeMode.values().size, catalog.size)
    }

    @Test
    fun lootboxPool_excludesAlreadyUnlockedThemes() {
        val pool = ThemeCatalog.lootboxRewardPool(ThemeCatalog.DEFAULT_UNLOCKED)
        assertEquals(ThemeCatalog.LOOTBOX_EXCLUSIVE, pool.toSet())
        assertTrue(pool.none { it in ThemeCatalog.DEFAULT_UNLOCKED })
    }

    @Test
    fun lootboxPool_isEmptyWhenAllThemesUnlocked() {
        assertTrue(ThemeCatalog.lootboxRewardPool(AppThemeMode.values().toSet()).isEmpty())
    }

    @Test
    fun titlesAreRussianAndNonBlank() {
        ThemeCatalog.build(emptySet()).forEach {
            assertTrue(it.titleRu.isNotBlank())
            assertEquals(it.theme.titleRu, it.titleRu)
        }
    }
}
