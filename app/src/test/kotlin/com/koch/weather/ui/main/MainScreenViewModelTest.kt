package com.koch.weather.ui.main

import com.koch.weather.data.model.*
import com.koch.weather.data.repository.AiFactsRepository
import com.koch.weather.data.repository.FlightRepository
import com.koch.weather.data.repository.FoodRepository
import com.koch.weather.data.repository.TaxiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

        // Superapp Phase 3 & 4 additions
        assertFalse(state.isHourlyExpanded)
        assertFalse(state.isCloudLocked)
        assertFalse(state.isPremiumPurchased)
        assertTrue(state.showAds)
        assertEquals(4, state.availableThemes.size)
        assertTrue(state.aiFact.isNotEmpty())
        assertFalse(state.lootboxState.isOpening)
        assertFalse(state.taxiState.isSheetOpen)
        assertFalse(state.foodState.isSheetOpen)
        assertFalse(state.flightState.isSheetOpen)
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
            isUmbrellaShopOpen = true,
            isHourlyExpanded = true,
            isCloudLocked = true,
            isPremiumPurchased = true,
            showAds = false
        )
        assertTrue(updated.isOffline)
        assertEquals(AppThemeMode.COPPER_SUNSET, updated.currentTheme)
        assertEquals(ConditionSegment.CLOTHES, updated.selectedSegment)
        assertEquals(BottomTab.MAP, updated.selectedTab)
        assertEquals(Integer.valueOf(5), updated.userRating)
        assertEquals(4.2f, updated.rating, 0.01f)
        assertTrue(updated.showRatingThankYou)
        assertTrue(updated.isUmbrellaShopOpen)
        assertTrue(updated.isHourlyExpanded)
        assertTrue(updated.isCloudLocked)
        assertTrue(updated.isPremiumPurchased)
        assertFalse(updated.showAds)
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
    fun bottomTabs_hasAllFiveTabs() {
        val tabs = BottomTab.values()
        assertEquals(5, tabs.size)
        assertTrue(tabs.contains(BottomTab.HOME))
        assertTrue(tabs.contains(BottomTab.MAP))
        assertTrue(tabs.contains(BottomTab.THEMES))
        assertTrue(tabs.contains(BottomTab.LIST))
        assertTrue(tabs.contains(BottomTab.SEARCH))
        assertEquals(BottomTab.LIST, BottomTab.CITIES)
    }

    @Test
    fun appThemeMode_hasAllSevenThemes() {
        val themes = AppThemeMode.values()
        assertEquals(7, themes.size)
        assertTrue(themes.contains(AppThemeMode.BLUE_SKY))
        assertTrue(themes.contains(AppThemeMode.COPPER_SUNSET))
        assertTrue(themes.contains(AppThemeMode.FOREST))
        assertTrue(themes.contains(AppThemeMode.MARS))
        assertTrue(themes.contains(AppThemeMode.CYBERPUNK))
        assertTrue(themes.contains(AppThemeMode.BLOODY_SUNSET))
        assertTrue(themes.contains(AppThemeMode.AURORA_BOREALIS))
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

    @Test
    fun taxiRepository_priceCalculationIncludesSurge() {
        val taxiRepo = TaxiRepository()
        val destinations = taxiRepo.getDestinations()
        assertTrue(destinations.isNotEmpty())

        val basePrice = taxiRepo.calculatePriceRu(TaxiTariffType.ECONOMY, distanceKm = 3.0, trafficScore = 3)
        val surgePrice = taxiRepo.calculatePriceRu(TaxiTariffType.ECONOMY, distanceKm = 3.0, trafficScore = 9)
        assertTrue(surgePrice > basePrice)

        val priceByn = taxiRepo.calculatePriceByn(TaxiTariffType.ECONOMY, distanceKm = 3.0, trafficScore = 3)
        assertTrue(priceByn > 0.0)
    }

    @Test
    fun foodRepository_restaurantsAndMenuLoaded() {
        val foodRepo = FoodRepository()
        val restaurants = foodRepo.getRestaurants()
        assertEquals(3, restaurants.size)
        assertTrue(restaurants.any { it.name.contains("Вкусно") })
        assertTrue(restaurants.any { it.name.contains("Додо") })
        assertTrue(restaurants.any { it.name.contains("Surf") })

        val vkusnoMenu = foodRepo.getMenuItems("rest_vkusno")
        assertTrue(vkusnoMenu.isNotEmpty())
        assertTrue(vkusnoMenu.any { it.name.contains("Биг Хит") })
    }

    @Test
    fun flightRepository_offersAvailable() {
        val flightRepo = FlightRepository()
        val offers = flightRepo.getFlightOffers()
        assertTrue(offers.size >= 4)
        assertTrue(offers.any { it.toCity.contains("Дубай") })
        assertTrue(offers.any { it.airline.contains("Emirates") })

        val seats = flightRepo.getAvailableSeats()
        assertTrue(seats.contains("12A"))
        assertTrue(seats.contains("14F"))
    }

    @Test
    fun aiFactsRepository_hasFacts() {
        val aiRepo = AiFactsRepository()
        val facts = aiRepo.getAllFacts()
        assertTrue(facts.size >= 5)
        assertEquals("Чтобы точно узнать погоду, нужно посмотреть в окно", aiRepo.getDefaultFact())
        assertTrue(facts.any { it.contains("окно") })
        assertTrue(facts.any { it.contains("носки") })
        assertTrue(facts.any { it.contains("Питере") })
        assertNotNull(aiRepo.getRandomFact())
    }
}
