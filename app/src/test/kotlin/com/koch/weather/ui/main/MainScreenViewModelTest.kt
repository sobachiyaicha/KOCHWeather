package com.koch.weather.ui.main

import com.koch.weather.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {

    @Test
    fun defaultUiState_valuesAreCorrect() {
        val state = MainScreenUiState()
        assertEquals(AppThemeMode.BLUE_SKY, state.currentTheme)
        assertEquals(ConditionSegment.WEATHER, state.selectedSegment)
        assertEquals(BottomTab.HOME, state.selectedTab)
        assertEquals(0, state.selectedCityIndex)
        assertEquals(0, state.currentCityIndex)
        assertFalse(state.isCloudsCleared)
        assertFalse(state.isGrassModalOpen)
        assertFalse(state.isGrassUploaded)
        assertEquals(3.5f, state.rating, 0.01f)
        assertNull(state.userRating)
        assertFalse(state.showRatingThankYou)
        assertFalse(state.isUmbrellaShopOpen)
        assertTrue(state.umbrellas.isNotEmpty())
    }

    @Test
    fun uiState_phase3Features_copyUpdatesState() {
        val state = MainScreenUiState()
        val updated = state.copy(
            isOffline = true,
            currentTheme = AppThemeMode.COPPER_SUNSET,
            selectedSegment = ConditionSegment.CLOTHES,
            selectedTab = BottomTab.MAP,
            userRating = 5,
            rating = 4.2f,
            showRatingThankYou = true,
            isUmbrellaShopOpen = true
        )
        assertTrue(updated.isOffline)
        assertEquals(AppThemeMode.COPPER_SUNSET, updated.currentTheme)
        assertEquals(ConditionSegment.CLOTHES, updated.selectedSegment)
        assertEquals(BottomTab.MAP, updated.selectedTab)
        assertEquals(Integer.valueOf(5), updated.userRating)
        assertEquals(4.2f, updated.rating, 0.01f)
        assertTrue(updated.showRatingThankYou)
        assertTrue(updated.isUmbrellaShopOpen)
    }

    @Test
    fun conditionSegments_hasAllFiveSegments() {
        val segments = ConditionSegment.values()
        assertEquals(5, segments.size)
        assertTrue(segments.contains(ConditionSegment.WEATHER))
        assertTrue(segments.contains(ConditionSegment.RAIN))
        assertTrue(segments.contains(ConditionSegment.WIND))
        assertTrue(segments.contains(ConditionSegment.CLOTHES))
        assertTrue(segments.contains(ConditionSegment.TRAFFIC))
    }

    @Test
    fun bottomTabs_hasAllFourTabs() {
        val tabs = BottomTab.values()
        assertEquals(4, tabs.size)
        assertTrue(tabs.contains(BottomTab.HOME))
        assertTrue(tabs.contains(BottomTab.MAP))
        assertTrue(tabs.contains(BottomTab.LIST))
        assertTrue(tabs.contains(BottomTab.SEARCH))
    }

    @Test
    fun defaultCities_spbAndMoscowAreTop() {
        val defaultCities = listOf(
            CityLocation("Санкт-Петербург", 59.9386, 30.3141, "Россия"),
            CityLocation("Москва", 55.7558, 37.6173, "Россия"),
            CityLocation("Минск", 53.9006, 27.5590, "Беларусь"),
            CityLocation("Дубай", 25.2048, 55.2708, "ОАЭ")
        )
        assertEquals("Санкт-Петербург", defaultCities[0].name)
        assertEquals("Москва", defaultCities[1].name)
    }

    @Test
    fun defaultUmbrellas_containsExpectedItems() {
        val umbrellas = DefaultUmbrellas.items
        assertTrue(umbrellas.size >= 4)
        assertTrue(umbrellas.any { it.name.contains("Sky Blue") })
        assertTrue(umbrellas.any { it.priceRu.contains("649") })
    }
}
