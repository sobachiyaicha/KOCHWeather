package com.koch.weather.ui.components

import com.koch.weather.data.model.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Листание тем в лутбоксе (item 4 фазы 5). Стрелки `◀ ▶` обязаны ходить ТОЛЬКО по
 * доступным темам: закрытую тему нельзя «примерить» и применить.
 *
 * Логика вынесена в [ThemeRevealState] без Compose UI, поэтому проверяется обычным
 * JVM-тестом — без скриншотов и без устройства.
 */
class ThemeRevealStateTest {

    private val available = listOf(AppThemeMode.BLUE_SKY, AppThemeMode.FOREST)

    @Test
    fun arrowsCycleOnlyAvailableThemes() {
        val state = ThemeRevealState(available, AppThemeMode.FOREST)

        repeat(6) {
            state.step(+1)
            assertTrue(
                "Стрелка вправо вывела на закрытую тему ${state.preview}",
                state.preview in available
            )
        }
        repeat(6) {
            state.step(-1)
            assertTrue(
                "Стрелка влево вывела на закрытую тему ${state.preview}",
                state.preview in available
            )
        }
    }

    @Test
    fun arrowsWrapAroundAndReachEveryAvailableTheme() {
        val state = ThemeRevealState(available, AppThemeMode.BLUE_SKY)
        val seen = mutableSetOf(state.preview)
        repeat(available.size * 2) {
            state.step(+1)
            seen += state.preview
        }
        assertEquals(available.toSet(), seen)
    }

    @Test
    fun neighboursAreAvailableThemesEvenWhenThereAreTwo() {
        val state = ThemeRevealState(available, AppThemeMode.BLUE_SKY)
        assertEquals(AppThemeMode.FOREST, state.below)
        assertEquals(AppThemeMode.FOREST, state.above)
    }

    @Test
    fun singleAvailableThemeKeepsPreviewOnIt() {
        val state = ThemeRevealState(listOf(AppThemeMode.MARS), AppThemeMode.MARS)
        state.step(+1)
        state.step(-1)
        assertEquals(AppThemeMode.MARS, state.preview)
    }

    @Test
    fun selectAnyDroppedTheme() {
        val state = ThemeRevealState(available, AppThemeMode.BLUE_SKY)
        state.select(AppThemeMode.FOREST)
        assertEquals(AppThemeMode.FOREST, state.preview)
    }
}
