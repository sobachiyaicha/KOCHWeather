package com.koch.weather.data

import com.koch.weather.data.model.AdCatalog
import com.koch.weather.data.model.AdMediaType
import com.koch.weather.data.model.EmojiTextGuard
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.repository.AdRepository
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Реклама (ТЗ): пул из шести медиа-креативов, «лев» первым показом,
 * баннер только из фото1/фото2, подпись «КУПИТЬ СЕЙЧАС!» у трёх фото.
 */
class AdCatalogTest {

    private val repository = AdRepository()

    @Test
    fun sixCreatives_mediaPool() {
        val creatives = repository.getCreatives()
        assertEquals(6, creatives.size)
        assertEquals(6, creatives.map { it.id }.toSet().size)
        assertEquals(6, repository.count())
        assertEquals(
            listOf("ad_photo1", "ad_photo2", "ad_slippers", "ad_son", "ad_lion", "ad_slippers_video"),
            creatives.map { it.id }
        )
        assertEquals(4, creatives.count { it.type == AdMediaType.PHOTO })
        assertEquals(2, creatives.count { it.type == AdMediaType.VIDEO })
        // Медиа-ресурс обязателен у каждого креатива: пустого креатива быть не может.
        assertTrue(creatives.all { it.mediaRes != 0 })
    }

    @Test
    fun buyLabel_isOnlyOnThreePhotos_andHasNoEmoji() {
        assertEquals(
            setOf("ad_photo1", "ad_photo2", "ad_slippers"),
            repository.getCreatives().filter { it.hasBuyLabel }.map { it.id }.toSet()
        )
        assertTrue(
            repository.getCreatives()
                .filter { it.hasBuyLabel }
                .all { it.type == AdMediaType.PHOTO }
        )
        assertTrue(AdCatalog.BUY_LABEL_RU.contains("КУПИТЬ СЕЙЧАС"))
        assertFalse(EmojiTextGuard.containsEmoji(AdCatalog.BUY_LABEL_RU))
    }

    @Test
    fun banner_usesOnlyPhoto1AndPhoto2() {
        assertEquals(listOf("ad_photo1", "ad_photo2"), AdCatalog.bannerCreatives.map { it.id })
        assertTrue(AdCatalog.bannerCreatives.all { it.type == AdMediaType.PHOTO })
        // Ротация баннера детерминированная и ходит по кругу фото1 → фото2 → фото1.
        assertEquals(AdCatalog.photo1, repository.bannerCreativeFor(0))
        assertEquals(AdCatalog.photo2, repository.bannerCreativeFor(1))
        assertEquals(AdCatalog.photo1, repository.bannerCreativeFor(2))
        assertFalse(AdCatalog.BANNER_DISCOUNT_RU.isBlank())
        assertFalse(AdCatalog.BANNER_NOTE_RU.isBlank())
        assertFalse(EmojiTextGuard.containsEmoji(AdCatalog.BANNER_DISCOUNT_RU))
    }

    @Test
    fun firstShow_isLionVideo() {
        assertEquals(AdCatalog.lion, repository.firstShowCreative())
        assertEquals("ad_lion", repository.firstShowCreative().id)
        assertEquals(AdMediaType.VIDEO, repository.firstShowCreative().type)
    }

    @Test
    fun rotation_isDeterministic() {
        val creatives = repository.getCreatives()
        assertEquals(creatives[0], repository.creativeFor(0))
        assertEquals(creatives[1], repository.creativeFor(1))
        assertEquals(creatives[2], repository.creativeFor(2))
        // Полный круг возвращает первый креатив.
        assertEquals(creatives[0], repository.creativeFor(6))
        // Один и тот же вход — один и тот же креатив (никакой случайности).
        assertEquals(repository.creativeFor(9), repository.creativeFor(9))
        assertNull(AdCatalog.byId("нет-такого-креатива"))
        assertNotNull(AdCatalog.byId("ad_son"))
    }

    @Test
    fun uiState_alwaysHasCreative() {
        val state = MainScreenUiState()
        assertNotNull(state.adCreative)
        assertEquals(AdCatalog.creatives.first(), state.adCreative)
        assertEquals(AdCatalog.bannerCreatives.first(), state.adBannerCreative)
        assertFalse(state.isAdPopupVisible)
    }

    @Test
    fun popupCreative_isHalfVideoHalfPhoto() {
        // Детерминированный Random с фиксированным seed: доля видео около 50 %.
        val random = Random(2026)
        val draws = (1..400).map { repository.popupCreative(lastShownId = null, random = random) }
        val videos = draws.count { it.type == AdMediaType.VIDEO }
        assertEquals(400, draws.size)
        assertTrue("видео $videos из 400 — ожидали около половины", videos in 140..260)
        // Каждый креатив выпал хотя бы раз: пул не «залипает» на паре значений.
        assertEquals(6, draws.map { it.id }.toSet().size)
    }

    @Test
    fun popupCreative_neverRepeatsPreviousCreative() {
        val random = Random(7)
        var previous = repository.popupCreative(random = random)
        repeat(200) {
            val next = repository.popupCreative(lastShownId = previous.id, random = random)
            assertNotEquals(previous.id, next.id)
            assertNotNull(AdCatalog.byId(next.id))
            previous = next
        }
    }

    @Test
    fun popupCreative_neverReturnsLastShownId() {
        // Любой последний креатив исключается из выбора при любом seed.
        AdCatalog.creatives.forEach { last ->
            repeat(200) { seed ->
                val next = AdCatalog.popupCreative(lastShownId = last.id, random = Random(seed))
                assertNotEquals("после ${last.id} выпал он же", last.id, next.id)
            }
        }
    }
}
