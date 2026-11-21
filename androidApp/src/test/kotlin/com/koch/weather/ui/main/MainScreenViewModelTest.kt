package com.koch.weather.ui.main

import com.koch.weather.data.model.*
import com.koch.weather.platform.AppContainer
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
        assertFalse(state.isPremiumPurchasePending)
        assertTrue(state.showAds)
        assertEquals(4, state.availableThemes.size)
        assertTrue(state.aiFact.isNotEmpty())
        assertFalse(state.lootboxState.isOpening)
        assertFalse(state.taxiState.isSheetOpen)
        assertFalse(state.foodState.isSheetOpen)
        assertFalse(state.flightState.isSheetOpen)
    }

    @Test
    fun defaultUiState_hasHonestEmptyWeatherState() {
        val state = MainScreenUiState()
        assertNull(state.weather)
        assertEquals(WeatherDataState.LOADING, state.weatherState)
        assertFalse(state.isWeatherUnavailable)
        assertNull(state.weatherErrorMessage)
        assertNull(state.weatherLastUpdated)
        assertFalse(state.isRefreshing)
        assertEquals(WeatherSource.NONE, state.weatherSource)
        assertEquals(WeatherSource.NONE.attributionRu, state.weatherAttributionRu)
        assertNull(state.weatherSourceNote)
        assertTrue(state.savedCities.isEmpty())
        assertFalse(state.isLoadingSavedCities)
        assertEquals(6, state.themeCatalog.size)
        assertNull(state.searchError)
        assertTrue(state.imageUrls.isEmpty())
    }

    @Test
    fun viewModelConstructor_hasNoHeavyDependencies() {
        // ANR Run 2: конструктор VM не должен принимать 11 репозиториев/DataStore/OkHttp.
        // Фаза 2 (KMP, ui-porter): AndroidViewModel(Application) заменён общим
        // ViewModel с ЕДИНСТВЕННЫМ лёгким параметром AppContainer (замена прежнего
        // Application-контракта; замысел гварда — «один лёгкий параметр, без
        // репозиториев» — сохранён).
        val constructors = MainScreenViewModel::class.java.constructors
        assertTrue(constructors.isNotEmpty())
        assertTrue(
            "Ожидался только (AppContainer), получено: " +
                constructors.joinToString { it.parameterTypes.joinToString() },
            constructors.all { it.parameterTypes.size == 1 }
        )
        assertTrue(
            constructors.any { it.parameterTypes.singleOrNull() == AppContainer::class.java }
        )
        // Начальное состояние не требует сети: погоды ещё нет, экран в LOADING.
        val initial = MainScreenUiState(
            isLoading = true,
            weather = null,
            weatherState = WeatherDataState.LOADING
        )
        assertNull(initial.weather)
        assertEquals(WeatherDataState.LOADING, initial.weatherState)
    }

    @Test
    fun premiumPurchaseRequiresExplicitPendingConfirmation() {
        val initial = MainScreenUiState()
        assertFalse(initial.isPremiumPurchased)
        assertFalse(initial.isPremiumPurchasePending)
        assertTrue(initial.showAds)

        val awaitingConfirmation = initial.copy(isPremiumPurchasePending = true)
        assertTrue(awaitingConfirmation.isPremiumPurchasePending)
        assertFalse(awaitingConfirmation.isPremiumPurchased)

        val confirmed = awaitingConfirmation.copy(
            isPremiumPurchasePending = false,
            isPremiumPurchased = true,
            showAds = false
        )
        assertTrue(confirmed.isPremiumPurchased)
        assertFalse(confirmed.isPremiumPurchasePending)
        assertFalse(confirmed.showAds)
    }

    @Test
    fun unavailableWeatherState_isExplicit() {
        val state = MainScreenUiState().copy(
            weather = null,
            weatherState = WeatherDataState.UNAVAILABLE,
            isWeatherUnavailable = true,
            weatherErrorMessage = "Нет данных о погоде",
            isOffline = true
        )
        assertTrue(state.isWeatherUnavailable)
        assertEquals(WeatherDataState.UNAVAILABLE, state.weatherState)
        assertNull(state.weather)
        assertEquals("Нет данных о погоде", state.weatherErrorMessage)
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
    fun appThemeMode_hasAllSixThemes_andNoForest() {
        val themes = AppThemeMode.values()
        assertEquals(6, themes.size)
        assertTrue(themes.contains(AppThemeMode.BLUE_SKY))
        assertTrue(themes.contains(AppThemeMode.COPPER_SUNSET))
        assertTrue(themes.none { it.name == "FOREST" })
        assertTrue(themes.contains(AppThemeMode.MARS))
        assertTrue(themes.contains(AppThemeMode.CYBERPUNK))
        assertTrue(themes.contains(AppThemeMode.BLOODY_SUNSET))
        assertTrue(themes.contains(AppThemeMode.AURORA_BOREALIS))
    }

    @Test
    fun defaultCities_arePetersburgMoscowSochiDubai() {
        val defaultCities = DefaultCities.cities
        assertEquals(
            listOf("Санкт-Петербург", "Москва", "Сочи", "Дубай"),
            defaultCities.map { it.name }
        )
        assertEquals("Россия", defaultCities[0].country)
        assertEquals("Россия", defaultCities[2].country)
        assertEquals("Краснодарский край", defaultCities[2].region)
        assertEquals(43.5855, defaultCities[2].latitude, 0.0001)
        assertEquals(39.7231, defaultCities[2].longitude, 0.0001)
        // Гео-город — только первый, и у него подпись «текущее место».
        assertTrue(defaultCities[0].isCurrentLocation)
        assertFalse(defaultCities[1].isCurrentLocation)
        assertFalse(DefaultCities.isGeoCity(defaultCities[3]))
        assertTrue(DefaultCities.isGeoCity(defaultCities[0]))
        // Прежние дефолты с чужой страной в каталог не возвращаются.
        assertTrue(defaultCities.all { it.country == "Россия" || it.country == "ОАЭ" })
    }

    @Test
    fun defaultUmbrellas_containsExpectedItems() {
        val umbrellas = DefaultUmbrellas.items
        assertTrue(umbrellas.size >= 4)
        assertTrue(umbrellas.any { it.name.contains("Sky Blue") })
        assertTrue(umbrellas.any { it.priceRu.contains("649") })
        assertEquals(649, DefaultUmbrellas.minPriceRu)
        assertEquals("649 ₽", DefaultUmbrellas.minPriceLabel)
    }

    @Test
    fun taxiRepository_priceCalculationIncludesSurge() {
        val taxiRepo = TaxiRepository()
        val destinations = taxiRepo.getDestinations()
        assertTrue(destinations.isNotEmpty())

        val basePrice = taxiRepo.calculatePriceRu(TaxiTariffType.ECONOMY, distanceKm = 10.0, trafficScore = null)
        val surgePrice = taxiRepo.calculatePriceRu(TaxiTariffType.ECONOMY, distanceKm = 10.0, trafficScore = 9)
        assertTrue(surgePrice > basePrice)
        assertEquals(521, basePrice)
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

        val sochi = offers.first { it.toCity.contains("Сочи") }
        assertEquals("Сочи (AER)", sochi.toCity)
        assertEquals("Россия", sochi.airline)
        assertTrue(sochi.flightNumber.startsWith("FV-") || sochi.flightNumber.startsWith("SU-"))
        assertEquals("09:40", sochi.departureTime)
        assertEquals("12:20", sochi.arrivalTime)
        assertEquals("2 ч 40 мин", sochi.durationText)
        assertEquals(6900, sochi.priceRu)
        assertEquals("Солнечно, +26", sochi.weatherAtDestination)
        // Прежнего оффера с чужой страной больше нет.
        assertTrue(offers.none { it.toCity.contains("(MSQ)") })

        val seats = flightRepo.getAvailableSeats()
        assertTrue(seats.contains("12A"))
        assertTrue(seats.contains("14F"))
    }

    @Test
    fun aiFactsRepository_hasFacts() {
        val aiRepo = AiFactsRepository()
        val facts = aiRepo.getAllFacts()
        assertTrue("Нужно минимум 16 фактов", facts.size >= 16)
        assertTrue(facts.none { it.isBlank() })
        assertEquals("Чтобы точно узнать погоду, нужно посмотреть в окно", aiRepo.getDefaultFact())
        assertTrue(facts.any { it.contains("окно") })
        assertTrue(facts.any { it.contains("носки") })
        assertTrue(facts.any { it.contains("Питере") })
        assertNotNull(aiRepo.getRandomFact())
        assertTrue(facts.toSet().size == facts.size)
    }

    @Test
    fun uiState_hasAdCreativeAndCurrentLocationContract() {
        val state = MainScreenUiState()
        assertNotNull(state.adCreative)
        assertEquals(AdCatalog.creatives.first(), state.adCreative)
        // Баннер стартует с фото1 и не открывает поп-ап сам по себе (ТЗ п. 3/5).
        assertEquals(AdCatalog.bannerCreatives.first(), state.adBannerCreative)
        assertFalse(state.isAdPopupVisible)
        assertFalse(state.isSelectedCityCurrentLocation)
        assertTrue(state.lootboxState.history.isEmpty())
        assertEquals(0, state.lootboxState.lastRewardSeq)
    }
}
