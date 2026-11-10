package com.koch.weather.ui.main

import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.BottomTab
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.ConditionSegment
import com.koch.weather.data.model.MainScreenUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    }

    @Test
    fun uiState_copyUpdatesState() {
        val state = MainScreenUiState()
        val updated = state.copy(
            isOffline = true,
            currentTheme = AppThemeMode.COPPER_SUNSET,
            isGrassUploaded = true,
            selectedCityIndex = 1,
            currentCityIndex = 1
        )
        assertTrue(updated.isOffline)
        assertEquals(AppThemeMode.COPPER_SUNSET, updated.currentTheme)
        assertTrue(updated.isGrassUploaded)
        assertEquals(1, updated.selectedCityIndex)
        assertEquals(1, updated.currentCityIndex)
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
}
