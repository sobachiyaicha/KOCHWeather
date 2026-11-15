package com.koch.weather.data

import com.koch.weather.data.model.AdAction
import com.koch.weather.data.model.AdCatalog
import com.koch.weather.data.model.EmojiTextGuard
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.repository.AdRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Item 9 фазы 5: реклама — 4 разных креатива, детерминированная ротация,
 * CTA ведёт в разные функции, без эмодзи, disclaimer обязателен.
 */
class AdCatalogTest {

    private val repository = AdRepository()

    @Test
    fun fourCreatives_withAllCallToActions() {
        val creatives = repository.getCreatives()
        assertEquals(4, creatives.size)
        assertEquals(4, creatives.map { it.id }.toSet().size)
        assertEquals(4, repository.count())
        assertEquals(
            AdAction.values().toSet(),
            creatives.map { it.action }.toSet()
        )
        assertTrue(creatives.any { it.action == AdAction.PAYWALL && it.imageCategory?.name == "AD_LAPTOP" })
        assertTrue(creatives.any { it.action == AdAction.LOOTBOX && it.imageCategory?.name == "IPHONE" })
        assertTrue(creatives.any { it.action == AdAction.UMBRELLA_SHOP && it.imageCategory == null })
        assertTrue(creatives.any { it.action == AdAction.FOOD && it.imageCategory?.name == "FOOD" })
    }

    @Test
    fun copy_isRussianAndHasNoEmoji() {
        repository.getCreatives().forEach { creative ->
            listOf(
                creative.brandRu,
                creative.headline,
                creative.body,
                creative.ctaLabel,
                creative.disclaimerRu
            ).forEach { text ->
                assertTrue("Пустой текст рекламы ${creative.id}", text.isNotBlank())
                assertFalse(
                    "Эмодзи в рекламе ${creative.id}: $text",
                    EmojiTextGuard.containsEmoji(text)
                )
            }
            assertTrue(
                "Нет пометки «Реклама» у ${creative.id}",
                creative.disclaimerRu.contains("Реклама")
            )
        }
        assertTrue(repository.getCreatives().any { it.headline.contains("iPhone 16 Pro Max") })
        assertTrue(repository.getCreatives().any { it.headline.contains("Зонтики от 649 ₽") })
        assertTrue(repository.getCreatives().any { it.headline.contains("Бургер за 30 минут") })
    }

    @Test
    fun rotation_isDeterministic_andShiftsWithDayAndOpenCount() {
        val creatives = repository.getCreatives()
        assertEquals(creatives[0], repository.creativeFor(0, 0))
        assertEquals(creatives[1], repository.creativeFor(0, 1))
        assertEquals(creatives[0], repository.creativeFor(0, 4))
        assertEquals(creatives[1], repository.creativeFor(1, 0))
        // Один и тот же вход — один и тот же креатив (никакой случайности).
        assertEquals(repository.creativeFor(12, 3), repository.creativeFor(12, 3))
        assertNotEquals(repository.creativeFor(12, 0), repository.creativeFor(12, 1))
    }

    @Test
    fun uiState_alwaysHasCreative() {
        val state = MainScreenUiState()
        assertNotNull(state.adCreative)
        assertEquals(AdCatalog.creatives.first(), state.adCreative)
    }
}
