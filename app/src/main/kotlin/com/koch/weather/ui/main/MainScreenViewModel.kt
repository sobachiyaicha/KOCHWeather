package com.koch.weather.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koch.weather.data.cache.WeatherCache
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.*
import com.koch.weather.data.network.NetworkMonitor
import com.koch.weather.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Тяжёлые зависимости создаются лениво и инициализируются в IO-корутине.
 * Конструктор ViewModel вызывается на main-потоке, поэтому он не должен
 * трогать DataStore, ConnectivityManager, OkHttp и репозитории.
 */
class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeatherRepository by lazy {
        WeatherRepository(getApplication<Application>())
    }
    private val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(getApplication<Application>())
    }
    private val cache: WeatherCache by lazy {
        WeatherCache(getApplication<Application>())
    }
    private val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(cache)
    }
    private val lootboxRepository: LootboxRepository by lazy {
        LootboxRepository(cache)
    }
    private val taxiRepository: TaxiRepository by lazy { TaxiRepository() }
    private val foodRepository: FoodRepository by lazy { FoodRepository() }
    private val flightRepository: FlightRepository by lazy { FlightRepository() }
    private val aiFactsRepository: AiFactsRepository by lazy { AiFactsRepository() }
    private val imageRepository: ImageRepository by lazy { ImageRepository() }

    // Первый кадр не ждёт ни DataStore, ни сети: только лёгкое начальное состояние.
    private val _uiState = MutableStateFlow(
        MainScreenUiState(
            isLoading = true,
            weather = null,
            weatherState = WeatherDataState.LOADING,
            isWeatherUnavailable = false
        )
    )
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var trafficJob: Job? = null
    private var searchJob: Job? = null
    private var taxiJob: Job? = null
    private var foodJob: Job? = null
    private var cloudTimerJob: Job? = null

    init {
        // Monitor network connection changes
        viewModelScope.launch(Dispatchers.IO) {
            var isFirstEmission = true
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { state ->
                    if (!isOnline) {
                        // Уже показанный реальный прогноз перестаёт быть «живым».
                        val offlineWeather = state.weather?.copy(
                            isOffline = true,
                            dataSourceLabel = null
                        )
                        state.copy(
                            isOffline = true,
                            weather = offlineWeather,
                            weatherState = if (offlineWeather != null) {
                                WeatherDataState.CACHED
                            } else {
                                state.weatherState
                            }
                        )
                    } else {
                        state.copy(isOffline = false)
                    }
                }

                if (!isFirstEmission && wasOffline && isOnline) {
                    refresh()
                }
                isFirstEmission = false
            }
        }

        // Observe premium status from DataStore
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionRepository.isPremiumFlow.collect { isPremium ->
                _uiState.update { state ->
                    state.copy(
                        isPremiumPurchased = isPremium,
                        isPremiumPurchasePending = if (isPremium) {
                            false
                        } else {
                            state.isPremiumPurchasePending
                        },
                        showAds = !isPremium
                    )
                }
            }
        }

        // Load unlocked themes, persisted theme and cloud-veil flag.
        viewModelScope.launch(Dispatchers.IO) {
            val unlocked = lootboxRepository.getUnlockedThemes()
            val prefs = cache.getUiPreferences()
            val restoredTheme = ThemePreferenceCodec.decode(prefs.themeId)
                ?.takeIf { it in unlocked }
            _uiState.update { state ->
                // Не откатываем очистку облаков, если пользователь успел стереть
                // вуаль раньше, чем DataStore вернул сохранённый флаг.
                val cloudsCleared = prefs.cloudsCleared || state.isCloudsCleared
                state.copy(
                    availableThemes = unlocked,
                    themeCatalog = ThemeCatalog.build(unlocked),
                    currentTheme = restoredTheme ?: state.currentTheme,
                    isCloudsCleared = cloudsCleared,
                    isCloudLocked = if (cloudsCleared) false else state.isCloudLocked
                )
            }
            startCloudLockTimer()
        }

        // Initialize cities, cache, static content and only then network — всё на IO.
        viewModelScope.launch(Dispatchers.IO) {
            val initialCities = repository.getInitialCities()
            val lastSavedCity = repository.getLastCity()
            val targetCity = lastSavedCity ?: initialCities.firstOrNull() ?: repository.defaultCities.first()
            val initialIndex = initialCities.indexOfFirst { it.name.equals(targetCity.name, ignoreCase = true) }
                .coerceAtLeast(0)

            // Последний реальный прогноз из DataStore (не синтетика).
            val cachedWeather = repository.getCachedWeather(targetCity)
            val cachedDomain = cachedWeather?.data
            val savedCities = repository.getSavedCitiesWeather(initialCities)

            val restaurants = foodRepository.getRestaurants()
            val flightOffers = flightRepository.getFlightOffers()
            val fact = aiFactsRepository.getDefaultFact()
            val imageUrls = imageRepository.getUrlsByCategory()

            _uiState.update {
                it.copy(
                    cities = initialCities,
                    selectedCity = targetCity,
                    selectedCityIndex = initialIndex,
                    currentCityIndex = initialIndex,
                    weather = cachedDomain,
                    weatherState = if (cachedDomain != null) {
                        WeatherDataState.CACHED
                    } else {
                        WeatherDataState.LOADING
                    },
                    isWeatherUnavailable = false,
                    isOffline = if (cachedDomain != null) true else it.isOffline,
                    weatherLastUpdated = cachedDomain?.lastUpdated,
                    weatherErrorMessage = null,
                    weatherSource = cachedWeather?.source ?: WeatherSource.NONE,
                    weatherAttributionRu = if (cachedWeather != null) {
                        cachedAttribution(cachedWeather.source)
                    } else {
                        WeatherSource.NONE.attributionRu
                    },
                    savedCities = savedCities,
                    isLoadingSavedCities = false,
                    foodState = it.foodState.copy(restaurants = restaurants),
                    flightState = it.flightState.copy(offers = flightOffers),
                    aiFact = fact,
                    imageUrls = imageUrls
                )
            }

            // Сетевой запрос стартует только после первого обновления состояния.
            fetchWeatherQuietly(targetCity)
        }

    }

    private fun startCloudLockTimer() {
        cloudTimerJob?.cancel()
        cloudTimerJob = viewModelScope.launch(Dispatchers.IO) {
            delay(15_000)
            _uiState.update { state ->
                if (!state.isCloudsCleared) {
                    state.copy(isCloudLocked = true)
                } else {
                    state
                }
            }
        }
    }

    /**
     * Instant city switch by index: реальный прогноз из памяти/DataStore,
     * затем тихое сетевое обновление. Синтетика не подставляется.
     */
    fun selectCity(index: Int) {
        val cities = _uiState.value.cities
        if (index !in cities.indices) return
        val targetCity = cities[index]

        // Мгновенно меняем только выбранный город и честный LOADING.
        // Память/DataStore читаются в IO внутри preloadCachedWeather.
        _uiState.update {
            it.copy(
                selectedCity = targetCity,
                selectedCityIndex = index,
                currentCityIndex = index,
                weather = null,
                weatherState = WeatherDataState.LOADING,
                isWeatherUnavailable = false,
                weatherErrorMessage = null,
                weatherLastUpdated = null,
                weatherSource = WeatherSource.NONE,
                weatherAttributionRu = WeatherSource.NONE.attributionRu,
                weatherSourceNote = null,
                isLoading = true,
                isRefreshing = true
            )
        }

        preloadCachedWeather(targetCity)
        fetchWeatherQuietly(targetCity)
    }

    fun selectCityByIndex(index: Int) {
        selectCity(index)
    }

    fun selectCity(city: CityLocation) {
        val index = _uiState.value.cities.indexOfFirst { it.name.equals(city.name, ignoreCase = true) }
        if (index >= 0) {
            selectCity(index)
        } else {
            addCity(city)
        }
    }

    fun loadWeather(city: CityLocation) {
        val index = _uiState.value.cities.indexOfFirst { it.name.equals(city.name, ignoreCase = true) }
        if (index >= 0) {
            selectCity(index)
        } else {
            _uiState.update {
                it.copy(
                    selectedCity = city,
                    weather = null,
                    weatherState = WeatherDataState.LOADING,
                    isWeatherUnavailable = false,
                    weatherErrorMessage = null,
                    weatherLastUpdated = null,
                    weatherSource = WeatherSource.NONE,
                    weatherAttributionRu = WeatherSource.NONE.attributionRu,
                    weatherSourceNote = null,
                    isLoading = true,
                    isRefreshing = true
                )
            }
            preloadCachedWeather(city)
            fetchWeatherQuietly(city)
        }
    }

    fun refresh() {
        val city = _uiState.value.selectedCity ?: return
        fetchWeatherQuietly(city)
    }

    /** Явная кнопка «Повторить» на экране ошибки/офлайна. */
    fun retryWeather() {
        refresh()
    }

    /** Подгружает последний реальный прогноз из DataStore, пока идёт сеть. */
    private fun preloadCachedWeather(city: CityLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            val cached = repository.getCachedWeather(city) ?: return@launch
            _uiState.update { state ->
                if (state.selectedCity?.name == city.name && state.weather == null) {
                    state.copy(
                        weather = cached.data,
                        weatherState = WeatherDataState.CACHED,
                        isOffline = true,
                        isWeatherUnavailable = false,
                        weatherErrorMessage = null,
                        weatherLastUpdated = cached.data.lastUpdated,
                        weatherSource = cached.source,
                        weatherAttributionRu = cachedAttribution(cached.source),
                        weatherSourceNote = null
                    )
                } else {
                    state
                }
            }
        }
    }

    private fun fetchWeatherQuietly(city: CityLocation) {
        loadJob?.cancel()
        trafficJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val isConnected = networkMonitor.isCurrentlyConnected()
            _uiState.update { state ->
                if (state.selectedCity?.name == city.name) {
                    state.copy(isRefreshing = true, isLoading = state.weather == null)
                } else {
                    state
                }
            }

            val result = try {
                repository.getWeatherData(city, isOnline = isConnected)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                WeatherLoadResult.Unavailable(WeatherErrors.from(e))
            }

            _uiState.update { state ->
                if (state.selectedCity?.name != city.name) {
                    return@update state
                }
                when (result) {
                    is WeatherLoadResult.Live -> state.copy(
                        weather = result.data,
                        weatherState = WeatherDataState.LIVE,
                        isOffline = false,
                        isWeatherUnavailable = false,
                        weatherErrorMessage = null,
                        weatherLastUpdated = result.data.lastUpdated,
                        weatherSource = result.source,
                        weatherAttributionRu = result.source.attributionRu,
                        weatherSourceNote = result.fallbackNote,
                        isLoading = false,
                        isRefreshing = false
                    )

                    is WeatherLoadResult.Cached -> state.copy(
                        weather = result.data,
                        weatherState = WeatherDataState.CACHED,
                        isOffline = true,
                        isWeatherUnavailable = false,
                        weatherErrorMessage = result.reason?.messageRu
                            ?: "Нет связи. Показан сохранённый прогноз.",
                        weatherLastUpdated = result.data.lastUpdated,
                        weatherSource = result.source,
                        weatherAttributionRu = cachedAttribution(result.source),
                        weatherSourceNote = result.reason?.messageRu,
                        isLoading = false,
                        isRefreshing = false
                    )

                    is WeatherLoadResult.Unavailable -> {
                        val failure = if (!isConnected) WeatherErrors.noNetwork() else result.failure
                        // Уже показанный реальный прогноз не удаляем: помечаем офлайновым.
                        val keptWeather = state.weather?.copy(
                            isOffline = true,
                            dataSourceLabel = null
                        )
                        state.copy(
                            weather = keptWeather,
                            weatherState = if (keptWeather != null) {
                                WeatherDataState.CACHED
                            } else {
                                WeatherDataState.UNAVAILABLE
                            },
                            isOffline = true,
                            isWeatherUnavailable = keptWeather == null,
                            weatherErrorMessage = failure.messageRu,
                            weatherLastUpdated = keptWeather?.lastUpdated,
                            weatherSource = if (keptWeather != null) {
                                state.weatherSource
                            } else {
                                WeatherSource.NONE
                            },
                            weatherAttributionRu = if (keptWeather != null) {
                                cachedAttribution(state.weatherSource)
                            } else {
                                WeatherSource.NONE.attributionRu
                            },
                            weatherSourceNote = failure.messageRu,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
            }

            val savedCities = repository.getSavedCitiesWeather(_uiState.value.cities)
            _uiState.update { it.copy(savedCities = savedCities) }

            // Пробки — отдельная корутина: прогноз уже на экране, цифры придут позже.
            if (result is WeatherLoadResult.Live) {
                trafficJob = viewModelScope.launch(Dispatchers.IO) {
                    val traffic = repository.getTraffic(city)
                    _uiState.update { state ->
                        if (state.selectedCity?.name == city.name && state.weather != null) {
                            state.copy(
                                weather = state.weather.copy(
                                    trafficScore = traffic.score,
                                    trafficAdvice = traffic.description
                                )
                            )
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }

    /** Перечитать температуры сохранённых городов из памяти/DataStore (R13). */
    fun refreshSavedCities() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedCities = repository.getSavedCitiesWeather(_uiState.value.cities)
            _uiState.update { it.copy(savedCities = savedCities) }
        }
    }

    private fun cachedAttribution(source: WeatherSource): String = when (source) {
        WeatherSource.CACHE, WeatherSource.NONE -> "Данные: сохранённый прогноз"
        else -> "Данные: ${source.displayNameRu} (сохранённый прогноз)"
    }

    // ------------------------------------------------------------------------
    // Card 1 Folding
    // ------------------------------------------------------------------------
    fun toggleHourlyExpanded() {
        _uiState.update { it.copy(isHourlyExpanded = !it.isHourlyExpanded) }
    }

    fun setHourlyExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isHourlyExpanded = expanded) }
    }

    // ------------------------------------------------------------------------
    // Cloud Screen Lock
    // ------------------------------------------------------------------------
    fun unlockClouds() {
        _uiState.update { it.copy(isCloudLocked = false, isCloudsCleared = true) }
        viewModelScope.launch(Dispatchers.IO) {
            cache.setCloudsCleared(true)
        }
    }

    /** Явный сброс (например, тестовая кнопка): снова показать вуаль. */
    fun lockClouds() {
        _uiState.update { it.copy(isCloudLocked = true, isCloudsCleared = false) }
        viewModelScope.launch(Dispatchers.IO) {
            cache.setCloudsCleared(false)
        }
    }

    fun onCloudsCleared() {
        unlockClouds()
    }

    // ------------------------------------------------------------------------
    // Navigation Tabs (5 Tabs: HOME, MAP, THEMES, LIST, SEARCH)
    // ------------------------------------------------------------------------
    fun selectTab(tab: BottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setTab(tab: BottomTab) {
        selectTab(tab)
    }

    // ------------------------------------------------------------------------
    // Theme Management
    // ------------------------------------------------------------------------
    fun setTheme(theme: AppThemeMode) {
        if (theme !in _uiState.value.availableThemes) return
        _uiState.update { it.copy(currentTheme = theme) }
        viewModelScope.launch(Dispatchers.IO) {
            cache.setCurrentTheme(ThemePreferenceCodec.encode(theme))
        }
    }

    fun toggleTheme() {
        val list = _uiState.value.availableThemes.toList()
        if (list.isEmpty()) return
        val currentIndex = list.indexOf(_uiState.value.currentTheme)
        val nextTheme = list[(currentIndex + 1) % list.size]
        setTheme(nextTheme)
    }

    // ------------------------------------------------------------------------
    // Segments
    // ------------------------------------------------------------------------
    fun setSegment(segment: ConditionSegment) {
        _uiState.update { it.copy(selectedSegment = segment) }
    }

    // ------------------------------------------------------------------------
    // Rating Widget
    // ------------------------------------------------------------------------
    fun rateWeather(stars: Int) {
        val clamped = stars.coerceIn(1, 5)
        val currentAvg = _uiState.value.rating
        val updatedAvg = ((currentAvg * 9f + clamped) / 10f)
        _uiState.update {
            it.copy(
                userRating = clamped,
                rating = kotlin.math.round(updatedAvg * 10f) / 10f,
                showRatingThankYou = true
            )
        }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(showRatingThankYou = false) }
        }
    }

    fun dismissRatingThankYou() {
        _uiState.update { it.copy(showRatingThankYou = false) }
    }

    fun setRating(rating: Float) {
        rateWeather(kotlin.math.round(rating).toInt().coerceIn(1, 5))
    }

    // ------------------------------------------------------------------------
    // Umbrella Store
    // ------------------------------------------------------------------------
    fun setUmbrellaShopOpen(open: Boolean) {
        _uiState.update { it.copy(isUmbrellaShopOpen = open) }
    }

    fun openUmbrellaShop() {
        setUmbrellaShopOpen(true)
    }

    fun closeUmbrellaShop() {
        setUmbrellaShopOpen(false)
    }

    // ------------------------------------------------------------------------
    // Premium Subscription & Ads
    // ------------------------------------------------------------------------
    /** Шаг 1: пользователь нажал «Купить» — показываем подтверждение в UI. */
    fun requestPremiumPurchase() {
        _uiState.update { it.copy(isPremiumPurchasePending = true) }
    }

    fun cancelPremiumPurchase() {
        _uiState.update { it.copy(isPremiumPurchasePending = false) }
    }

    /**
     * Шаг 2: запись в DataStore выполняется только после подтверждения.
     * Без предварительного requestPremiumPurchase() метод ничего не делает,
     * чтобы клик/открытие пейволла не активировали подписку.
     */
    fun confirmPremiumPurchase() {
        if (!_uiState.value.isPremiumPurchasePending) return
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionRepository.purchasePremium()
            _uiState.update {
                it.copy(
                    isPremiumPurchased = true,
                    isPremiumPurchasePending = false,
                    showAds = false
                )
            }
        }
    }

    /** @deprecated используйте requestPremiumPurchase + confirmPremiumPurchase. */
    @Deprecated("Use requestPremiumPurchase() + confirmPremiumPurchase()")
    fun purchasePremium() {
        confirmPremiumPurchase()
    }

    fun resetPremium() {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionRepository.resetPremium()
            _uiState.update {
                it.copy(
                    isPremiumPurchased = false,
                    isPremiumPurchasePending = false,
                    showAds = true
                )
            }
        }
    }

    // ------------------------------------------------------------------------
    // AI Facts
    // ------------------------------------------------------------------------
    fun refreshAiFact() {
        viewModelScope.launch(Dispatchers.IO) {
            val fact = aiFactsRepository.getRandomFact()
            _uiState.update { it.copy(aiFact = fact) }
        }
    }

    // ------------------------------------------------------------------------
    // Lootbox Service
    // ------------------------------------------------------------------------
    fun openLootbox() {
        if (_uiState.value.lootboxState.isOpening) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(lootboxState = it.lootboxState.copy(isOpening = true)) }
            delay(1200) // Spin animation delay
            // Только реальный прогноз из уже полученных данных; иначе награда
            // «прогноз на завтра» не выдаётся (см. LootboxRepository).
            val tomorrowDesc = _uiState.value.weather?.daily?.getOrNull(1)?.let {
                "${WmoHelper.getDescriptionRu(it.weatherCode)}, днем ${it.maxTemp}°, ночью ${it.minTemp}°"
            }
            val reward = lootboxRepository.openLootbox(tomorrowDesc)
            val unlockedThemes = lootboxRepository.getUnlockedThemes()
            val newTheme = ThemePreferenceCodec.decode(reward.unlockedThemeName)
            _uiState.update { state ->
                state.copy(
                    availableThemes = unlockedThemes,
                    themeCatalog = ThemeCatalog.build(unlockedThemes),
                    currentTheme = newTheme ?: state.currentTheme,
                    lootboxState = state.lootboxState.copy(
                        isOpening = false,
                        lastReward = reward,
                        history = listOf(reward) + state.lootboxState.history,
                        totalOpened = state.lootboxState.totalOpened + 1,
                        unlockedThemes = unlockedThemes
                    )
                )
            }
            if (newTheme != null) {
                cache.setCurrentTheme(ThemePreferenceCodec.encode(newTheme))
            }
        }
    }

    // ------------------------------------------------------------------------
    // Taxi Service (2x2 Grid)
    // ------------------------------------------------------------------------
    fun openTaxiSheet() {
        _uiState.update { it.copy(taxiState = it.taxiState.copy(isSheetOpen = true)) }
    }

    fun closeTaxiSheet() {
        _uiState.update { it.copy(taxiState = it.taxiState.copy(isSheetOpen = false)) }
    }

    fun selectTaxiTariff(tariff: TaxiTariffType) {
        _uiState.update { it.copy(taxiState = it.taxiState.copy(selectedTariff = tariff)) }
    }

    fun selectTaxiDestination(destination: TaxiDestination) {
        _uiState.update { it.copy(taxiState = it.taxiState.copy(selectedDestination = destination)) }
    }

    fun orderTaxi() {
        val tariff = _uiState.value.taxiState.selectedTariff
        val dest = _uiState.value.taxiState.selectedDestination
        // null = «нет данных о пробках»; выдуманный балл не подставляем.
        val trafficScore = _uiState.value.weather?.trafficScore

        taxiJob?.cancel()
        taxiJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(taxiState = it.taxiState.copy(isOrdering = true)) }
            taxiRepository.createRideSimulation(tariff, dest, trafficScore).collect { ride ->
                _uiState.update {
                    it.copy(
                        taxiState = it.taxiState.copy(
                            activeRide = ride,
                            isOrdering = ride.status != TaxiRideStatus.COMPLETED
                        )
                    )
                }
            }
        }
    }

    fun cancelTaxi() {
        taxiJob?.cancel()
        _uiState.update {
            it.copy(
                taxiState = it.taxiState.copy(
                    activeRide = null,
                    isOrdering = false
                )
            )
        }
    }

    // ------------------------------------------------------------------------
    // Food Delivery Service (2x2 Grid)
    // ------------------------------------------------------------------------
    fun openFoodSheet() {
        // Открываем шторку сразу, каталог читаем в IO (без блокировки main).
        _uiState.update { it.copy(foodState = it.foodState.copy(isSheetOpen = true)) }
        viewModelScope.launch(Dispatchers.IO) {
            val restaurants = foodRepository.getRestaurants()
            val defaultRest = _uiState.value.foodState.selectedRestaurant ?: restaurants.firstOrNull()
            val menu = defaultRest?.let { foodRepository.getMenuItems(it.id) } ?: emptyList()
            _uiState.update {
                it.copy(
                    foodState = it.foodState.copy(
                        restaurants = restaurants,
                        selectedRestaurant = defaultRest,
                        menuItems = menu
                    )
                )
            }
        }
    }

    fun closeFoodSheet() {
        _uiState.update { it.copy(foodState = it.foodState.copy(isSheetOpen = false)) }
    }

    fun selectRestaurant(restaurant: Restaurant) {
        _uiState.update {
            it.copy(
                foodState = it.foodState.copy(
                    selectedRestaurant = restaurant,
                    menuItems = emptyList(),
                    cart = emptyMap() // Clear cart when switching restaurant
                )
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val menu = foodRepository.getMenuItems(restaurant.id)
            _uiState.update { state ->
                if (state.foodState.selectedRestaurant?.id == restaurant.id) {
                    state.copy(foodState = state.foodState.copy(menuItems = menu))
                } else {
                    state
                }
            }
        }
    }

    fun addToCart(item: FoodItem) {
        val currentCart = _uiState.value.foodState.cart.toMutableMap()
        currentCart[item] = (currentCart[item] ?: 0) + 1
        _uiState.update { it.copy(foodState = it.foodState.copy(cart = currentCart)) }
    }

    fun removeFromCart(item: FoodItem) {
        val currentCart = _uiState.value.foodState.cart.toMutableMap()
        val count = currentCart[item] ?: 0
        if (count > 1) {
            currentCart[item] = count - 1
        } else {
            currentCart.remove(item)
        }
        _uiState.update { it.copy(foodState = it.foodState.copy(cart = currentCart)) }
    }

    fun clearCart() {
        _uiState.update { it.copy(foodState = it.foodState.copy(cart = emptyMap())) }
    }

    fun orderFood() {
        val restaurant = _uiState.value.foodState.selectedRestaurant ?: return
        val cart = _uiState.value.foodState.cart
        if (cart.isEmpty()) return

        foodJob?.cancel()
        foodJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(foodState = it.foodState.copy(isOrdering = true)) }
            foodRepository.createOrderSimulation(restaurant, cart).collect { order ->
                _uiState.update {
                    it.copy(
                        foodState = it.foodState.copy(
                            activeOrder = order,
                            isOrdering = order.status != FoodOrderStatus.DELIVERED,
                            cart = if (order.status == FoodOrderStatus.COOKING) emptyMap() else it.foodState.cart
                        )
                    )
                }
            }
        }
    }

    fun cancelFoodOrder() {
        foodJob?.cancel()
        _uiState.update {
            it.copy(foodState = it.foodState.copy(activeOrder = null, isOrdering = false))
        }
    }

    // ------------------------------------------------------------------------
    // Flight Service (2x2 Grid)
    // ------------------------------------------------------------------------
    fun openFlightSheet() {
        _uiState.update { state ->
            state.copy(
                flightState = state.flightState.copy(
                    isSheetOpen = true,
                    selectedOffer = state.flightState.selectedOffer
                        ?: state.flightState.offers.firstOrNull()
                )
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val offers = flightRepository.getFlightOffers()
            _uiState.update { state ->
                state.copy(
                    flightState = state.flightState.copy(
                        offers = offers,
                        selectedOffer = state.flightState.selectedOffer ?: offers.firstOrNull()
                    )
                )
            }
        }
    }

    fun closeFlightSheet() {
        _uiState.update { it.copy(flightState = it.flightState.copy(isSheetOpen = false)) }
    }

    fun selectFlightOffer(offer: FlightOffer) {
        _uiState.update { it.copy(flightState = it.flightState.copy(selectedOffer = offer)) }
    }

    fun selectSeat(seat: String) {
        _uiState.update { it.copy(flightState = it.flightState.copy(selectedSeat = seat)) }
    }

    fun bookFlight(passengerName: String = "Пассажир 2025") {
        val offer = _uiState.value.flightState.selectedOffer ?: return
        val seat = _uiState.value.flightState.selectedSeat
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(flightState = it.flightState.copy(isBookingInProgress = true)) }
            delay(1500)
            val booking = flightRepository.bookFlight(offer, seat, passengerName)
            _uiState.update {
                it.copy(
                    flightState = it.flightState.copy(
                        activeBooking = booking,
                        isBookingInProgress = false
                    )
                )
            }
        }
    }

    // ------------------------------------------------------------------------
    // City Search & Management
    // ------------------------------------------------------------------------
    fun searchCities(query: String) {
        _uiState.update { it.copy(searchQuery = query, searchError = null) }
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _uiState.update {
                it.copy(searchResults = emptyList(), isSearching = false, searchError = null)
            }
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300) // Debounce typing
            if (_uiState.value.searchQuery != query) return@launch
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            val result = try {
                repository.searchCities(query.trim())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                CitySearchResult.Failure(WeatherErrors.from(e))
            }

            if (_uiState.value.searchQuery != query) return@launch
            when (result) {
                is CitySearchResult.Success -> _uiState.update {
                    it.copy(
                        searchResults = result.cities,
                        isSearching = false,
                        searchError = null
                    )
                }

                is CitySearchResult.Failure -> _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        searchError = result.failure.messageRu
                    )
                }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                searchError = null
            )
        }
    }

    fun addCity(city: CityLocation) {
        val currentCities = _uiState.value.cities.toMutableList()
        val existingIndex = currentCities.indexOfFirst { it.name.equals(city.name, ignoreCase = true) }
        val targetIndex: Int

        if (existingIndex >= 0) {
            targetIndex = existingIndex
        } else {
            currentCities.add(city)
            targetIndex = currentCities.lastIndex
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveCities(currentCities)
                val saved = repository.getSavedCitiesWeather(currentCities)
                _uiState.update { it.copy(savedCities = saved) }
            }
        }

        _uiState.update {
            it.copy(
                cities = currentCities,
                selectedCity = city,
                selectedCityIndex = targetIndex,
                currentCityIndex = targetIndex,
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                searchError = null,
                selectedTab = BottomTab.HOME
            )
        }

        selectCity(targetIndex)
    }

    // ------------------------------------------------------------------------
    // Grass & Congrats Modals
    // ------------------------------------------------------------------------
    fun setGrassModal(open: Boolean) {
        _uiState.update { it.copy(isGrassModalOpen = open) }
    }

    fun onGrassUploaded() {
        _uiState.update { it.copy(isGrassUploaded = true, isGrassModalOpen = false) }
    }

    fun setCongratsModal(open: Boolean) {
        _uiState.update { it.copy(isCongratsModalOpen = open) }
    }
}
