package com.koch.weather.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koch.weather.data.model.*
import com.koch.weather.data.network.NetworkMonitor
import com.koch.weather.data.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainScreenViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WeatherRepository = WeatherRepository(application),
    private val networkMonitor: NetworkMonitor = NetworkMonitor(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        MainScreenUiState(
            isLoading = false,
            cities = repository.defaultCities,
            selectedCity = repository.defaultCities.first(),
            selectedCityIndex = 0,
            currentCityIndex = 0,
            weather = repository.getCachedWeatherFast(repository.defaultCities.first())
        )
    )
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        // Monitor network connection changes
        viewModelScope.launch(Dispatchers.IO) {
            var isFirstEmission = true
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { it.copy(isOffline = !isOnline) }

                if (!isFirstEmission && wasOffline && isOnline) {
                    refresh()
                }
                isFirstEmission = false
            }
        }

        // Initialize cities and weather on IO thread
        viewModelScope.launch(Dispatchers.IO) {
            val initialCities = repository.getInitialCities()
            val lastSavedCity = repository.getLastCity()
            val targetCity = lastSavedCity ?: initialCities.firstOrNull() ?: repository.defaultCities.first()
            val initialIndex = initialCities.indexOfFirst { it.name.equals(targetCity.name, ignoreCase = true) }
                .coerceAtLeast(0)

            val cachedWeather = repository.getCachedWeatherFast(targetCity)

            _uiState.update {
                it.copy(
                    cities = initialCities,
                    selectedCity = targetCity,
                    selectedCityIndex = initialIndex,
                    currentCityIndex = initialIndex,
                    weather = cachedWeather
                )
            }

            // Quietly fetch fresh data for initial city
            fetchWeatherQuietly(targetCity)
        }
    }

    /**
     * Instant city switch by index (0ms from memory/cache), then quiet network refresh.
     */
    fun selectCity(index: Int) {
        val cities = _uiState.value.cities
        if (index !in cities.indices) return
        val targetCity = cities[index]

        // 1. Instant cache hit (0ms)
        val cachedWeather = repository.getCachedWeatherFast(targetCity)

        // 2. Immediately update StateFlow (instant frame update, no blocking spinner)
        _uiState.update {
            it.copy(
                selectedCity = targetCity,
                selectedCityIndex = index,
                currentCityIndex = index,
                weather = cachedWeather,
                isLoading = false
            )
        }

        // 3. Quiet network fetch in background
        fetchWeatherQuietly(targetCity)
    }

    /**
     * Backward-compatible alias for UI components calling selectCityByIndex.
     */
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
            val cachedWeather = repository.getCachedWeatherFast(city)
            _uiState.update {
                it.copy(
                    selectedCity = city,
                    weather = cachedWeather,
                    isLoading = false
                )
            }
            fetchWeatherQuietly(city)
        }
    }

    fun refresh() {
        val city = _uiState.value.selectedCity ?: return
        fetchWeatherQuietly(city)
    }

    private fun fetchWeatherQuietly(city: CityLocation) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val isConnected = networkMonitor.isCurrentlyConnected()
            try {
                val freshData = repository.getWeatherData(city)
                _uiState.update {
                    if (it.selectedCity?.name == city.name) {
                        it.copy(
                            weather = freshData,
                            isLoading = false,
                            isOffline = !isConnected,
                            errorMessage = null
                        )
                    } else {
                        it
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    if (it.selectedCity?.name == city.name) {
                        it.copy(
                            isLoading = false,
                            isOffline = !isConnected,
                            errorMessage = e.message
                        )
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun searchCities(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300) // Debounce typing
            _uiState.update { it.copy(isSearching = true) }
            val results = repository.searchCities(query.trim())
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
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
            viewModelScope.launch(Dispatchers.IO) { repository.saveCities(currentCities) }
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
                selectedTab = BottomTab.HOME
            )
        }

        selectCity(targetIndex)
    }

    fun setTheme(theme: AppThemeMode) {
        _uiState.update { it.copy(currentTheme = theme) }
    }

    fun toggleTheme() {
        val nextTheme = when (_uiState.value.currentTheme) {
            AppThemeMode.BLUE_SKY -> AppThemeMode.COPPER_SUNSET
            AppThemeMode.COPPER_SUNSET -> AppThemeMode.FOREST
            AppThemeMode.FOREST -> AppThemeMode.MARS
            AppThemeMode.MARS -> AppThemeMode.BLUE_SKY
        }
        _uiState.update { it.copy(currentTheme = nextTheme) }
    }

    fun setSegment(segment: ConditionSegment) {
        _uiState.update { it.copy(selectedSegment = segment) }
    }

    fun setTab(tab: BottomTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setRating(rating: Float) {
        _uiState.update { it.copy(userRating = rating) }
    }

    fun onCloudsCleared() {
        _uiState.update { it.copy(isCloudsCleared = true) }
    }

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
