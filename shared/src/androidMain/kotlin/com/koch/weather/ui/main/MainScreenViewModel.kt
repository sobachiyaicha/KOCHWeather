package com.koch.weather.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.koch.weather.R
import com.koch.weather.data.cache.WeatherCache
import com.koch.weather.data.mapper.WeatherErrors
import com.koch.weather.data.model.*
import com.koch.weather.data.network.NetworkMonitor
import com.koch.weather.data.repository.*
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

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
    private val adRepository: AdRepository by lazy { AdRepository() }

    /**
     * Id последнего показанного креатива поп-апа. Живёт в памяти ViewModel:
     * нужен только чтобы один и тот же креатив не выпал два раза подряд.
     */
    private var lastAdPopupCreativeId: String? = null

    /** Ротация баннера главной: индекс фото1/фото2, растёт по таймеру. */
    private var adBannerSequence: Int = 0

    /**
     * Счётчик нажатий «Другой факт» за сессию. Живёт только в памяти ViewModel:
     * при перезапуске приложения снова 0 и кнопка «Другой факт» возвращается.
     */
    private var anotherFactTapCount: Int = 0

    /**
     * Замок на время подготовки поп-апа: чтение флага `first_ad_shown` идёт в IO,
     * поэтому два быстрых события «облака стёрты» не должны поставить два поп-апа.
     */
    private val adPopupGuard = AtomicBoolean(false)
    private var adPopupJob: Job? = null
    private var adBannerJob: Job? = null

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
    private var retryJob: Job? = null
    private var searchJob: Job? = null
    private var taxiJob: Job? = null
    private var foodJob: Job? = null
    private var cloudTimerJob: Job? = null
    private var savedCitiesJob: Job? = null

    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            onAppForeground()
        }

        override fun onStop(owner: LifecycleOwner) {
            onAppBackground()
        }
    }

    init {
        // Авто-триггеры retry без ручной проводки из UI: ON_START/ON_STOP процесса.
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            onAppForeground()
        }
        // Monitor network connection changes
        viewModelScope.launch(Dispatchers.IO) {
            var isFirstEmission = true
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = _uiState.value.isOffline
                if (!isOnline) {
                    retryJob?.cancel()
                }
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
                            },
                            isRetrying = false,
                            nextRetryAt = null
                        )
                    } else {
                        state.copy(isOffline = false)
                    }
                }

                if (!isFirstEmission && wasOffline && isOnline &&
                    _uiState.value.isAppForeground
                ) {
                    // Возврат сети = немедленный реальный forceRefresh.
                    retryNow()
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
            val restoredTheme = ThemeRestorePolicy.resolve(prefs.themeId, unlocked)
            _uiState.update { state ->
                // Флаги вуали здесь не трогаем: цикл задаёт таймер ниже, а
                // сохранённый в DataStore флаг «стёрто» не выключает его навсегда.
                state.copy(
                    availableThemes = unlocked,
                    themeCatalog = ThemeCatalog.build(unlocked),
                    currentTheme = restoredTheme ?: state.currentTheme
                )
            }
            startCloudLockTimer()
        }

        // Initialize cities, cache, static content and only then network — всё на IO.
        viewModelScope.launch(Dispatchers.IO) {
            val initialCities = repository.getInitialCities()
            val savedSelectedCity = repository.getSelectedCity()
            val lastSavedCity = repository.getLastCity()
            // D-2: выбранный город переживает перезапуск. Ключ отдельный от
            // «последнего прогноза»; город, которого нет в списке, не возвращаем —
            // падаем на гео-город. Для старой установки без выбора сохраняем
            // прежнее поведение (последний прогноз), затем гео-город.
            val targetCity = SelectedCityRestorePolicy.resolve(
                savedSelected = savedSelectedCity,
                legacyLastCity = lastSavedCity,
                cities = initialCities
            ) ?: initialCities.firstOrNull() ?: repository.defaultCities.first()
            val initialIndex = initialCities.indexOfFirst {
                CityCatalog.isSamePlace(it, targetCity)
            }.coerceAtLeast(0)

            // Последний реальный прогноз из DataStore (не синтетика).
            val cachedWeather = repository.getCachedWeather(targetCity)
            val cachedDomain = cachedWeather?.data
            val savedCities = repository.getSavedCitiesWeather(initialCities)

            val restaurants = foodRepository.getRestaurants()
            val flightOffers = flightRepository.getFlightOffers()
            // Факт не привязан к городу и дню года: индекс выбирается заново на
            // каждый запуск приложения и живёт в состоянии до конца сессии.
            val factIndex = FactOfDayPolicy.initialIndex(aiFactsRepository.getFactCount())
            val imageUrls = imageRepository.getUrlsByCategory()
            // История дропов переживает перезапуск: отдаём её в state и не теряем счётчик.
            val lootboxHistory = lootboxRepository.getHistory()
            // Потраченные попытки тоже персистятся: лимит не сбрасывается рестартом.
            val lootboxAttemptsUsed = lootboxRepository.getAttemptsUsed()

            _uiState.update {
                it.copy(
                    cities = initialCities,
                    selectedCity = targetCity,
                    selectedCityIndex = initialIndex,
                    currentCityIndex = initialIndex,
                    isSelectedCityCurrentLocation = DefaultCities.isGeoCity(targetCity),
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
                        WeatherUiReducer.cachedAttribution(cachedWeather.source)
                    } else {
                        WeatherSource.NONE.attributionRu
                    },
                    savedCities = savedCities,
                    isLoadingSavedCities = false,
                    foodState = it.foodState.copy(restaurants = restaurants),
                    flightState = it.flightState.copy(offers = flightOffers),
                    aiFact = aiFactsRepository.getFactAt(factIndex),
                    aiFactIndex = factIndex,
                    imageUrls = imageUrls,
                    // Креативы поп-апа и баннера живут в default-состоянии (фото1);
                    // выбор «льва» на первый показ делает requestAdPopup().
                    lootboxState = it.lootboxState.copy(
                        history = lootboxHistory,
                        totalOpened = lootboxHistory.size,
                        attemptsUsed = lootboxAttemptsUsed
                    )
                )
            }

            // Сетевой запрос стартует только после первого обновления состояния.
            fetchWeatherQuietly(targetCity)
        }

        // Баннер главной ротируется между фото1 и фото2 всё время жизни экрана.
        startAdBannerRotation()
    }

    /**
     * Цикл облачной вуали: первый показ — через [CLOUD_FIRST_SHOW_DELAY_MS] после
     * запуска, дальше после каждого стирания — снова через случайные
     * [CLOUD_RETURN_MIN_DELAY_MS]..[CLOUD_RETURN_MAX_DELAY_MS]. Флаг «стёрто» больше
     * не выключает вуаль навсегда: таймер сбрасывает его и ставит
     * `isCloudLocked = true`.
     *
     * Таймер всегда один: новый запуск отменяет предыдущий, корутина живёт в
     * viewModelScope и умирает вместе с ViewModel (утечки нет).
     */
    private fun startCloudLockTimer(delayMs: Long = CLOUD_FIRST_SHOW_DELAY_MS) {
        cloudTimerJob?.cancel()
        cloudTimerJob = viewModelScope.launch(Dispatchers.IO) {
            delay(delayMs)
            _uiState.update { state ->
                state.copy(isCloudLocked = true, isCloudsCleared = false)
            }
        }
    }

    /** Задержка возврата вуали после стирания: 20–30 с включительно. */
    private fun cloudReturnDelayMs(): Long =
        Random.nextLong(CLOUD_RETURN_MIN_DELAY_MS, CLOUD_RETURN_MAX_DELAY_MS + 1)

    /**
     * Instant city switch by index: реальный прогноз из памяти/DataStore,
     * затем тихое сетевое обновление. Синтетика не подставляется.
     */
    fun selectCity(index: Int) {
        val cities = _uiState.value.cities
        if (index !in cities.indices) return
        val targetCity = cities[index]
        retryJob?.cancel()

        // Мгновенно меняем только выбранный город и честный LOADING.
        // Память/DataStore читаются в IO внутри preloadCachedWeather.
        _uiState.update {
            it.copy(
                selectedCity = targetCity,
                selectedCityIndex = index,
                currentCityIndex = index,
                // «ТЕКУЩЕЕ МЕСТО» — только для гео-города, а не для любого выбранного.
                isSelectedCityCurrentLocation = DefaultCities.isGeoCity(targetCity),
                weather = null,
                weatherState = WeatherDataState.LOADING,
                isWeatherUnavailable = false,
                weatherErrorMessage = null,
                weatherLastUpdated = null,
                weatherSource = WeatherSource.NONE,
                weatherAttributionRu = WeatherSource.NONE.attributionRu,
                weatherSourceNote = null,
                isRetrying = false,
                nextRetryAt = null,
                retryAttempt = 0,
                isLoading = true,
                isRefreshing = true,
                // Пробки предыдущего города не должны влиять на цену такси.
                taxiState = it.taxiState.withRoute(
                    distanceKm = it.taxiState.selectedDestination.distanceKm,
                    trafficScore = null
                )
            )
        }

        preloadCachedWeather(targetCity)
        persistSelectedCity(targetCity)
        fetchWeatherQuietly(targetCity)
    }

    /**
     * Выбор города пишется в отдельный ключ DataStore. Запуск на Main (а запись —
     * на IO): при быстрых свайпах вызовы уходят в DataStore в порядке выбора,
     * последний выбор побеждает. «Последний прогноз» (`last_city_json`) не трогаем.
     */
    private fun persistSelectedCity(city: CityLocation) {
        viewModelScope.launch {
            repository.saveSelectedCity(city)
        }
    }

    fun selectCityByIndex(index: Int) {
        selectCity(index)
    }

    fun selectCity(city: CityLocation) {
        val index = _uiState.value.cities.indexOfFirst { CityCatalog.isSamePlace(it, city) }
        if (index >= 0) {
            selectCity(index)
        } else {
            addCity(city)
        }
    }

    fun loadWeather(city: CityLocation) {
        val index = _uiState.value.cities.indexOfFirst { CityCatalog.isSamePlace(it, city) }
        if (index >= 0) {
            selectCity(index)
        } else {
            retryJob?.cancel()
            _uiState.update {
                it.copy(
                    selectedCity = city,
                    isSelectedCityCurrentLocation = DefaultCities.isGeoCity(city),
                    weather = null,
                    weatherState = WeatherDataState.LOADING,
                    isWeatherUnavailable = false,
                    weatherErrorMessage = null,
                    weatherLastUpdated = null,
                    weatherSource = WeatherSource.NONE,
                    weatherAttributionRu = WeatherSource.NONE.attributionRu,
                    weatherSourceNote = null,
                    isRetrying = false,
                    nextRetryAt = null,
                    retryAttempt = 0,
                    isLoading = true,
                    isRefreshing = true,
                    taxiState = it.taxiState.withRoute(
                        distanceKm = it.taxiState.selectedDestination.distanceKm,
                        trafficScore = null
                    )
                )
            }
            preloadCachedWeather(city)
            fetchWeatherQuietly(city)
        }
    }

    /** Явный/автоматический retry: сбрасывает backoff и сразу идёт в сеть. */
    private fun retryNow() {
        val state = _uiState.value
        if (!state.isAppForeground) return
        val city = state.selectedCity ?: return
        retryJob?.cancel()
        val retry = WeatherRetryPolicy.onManualRetry()
        _uiState.update {
            it.copy(
                isRetrying = retry.isRetrying,
                nextRetryAt = retry.nextRetryAt,
                retryAttempt = retry.attempt
            )
        }
        fetchWeatherQuietly(city, forceRefresh = true)
    }

    fun refresh() {
        retryNow()
    }

    /** Явная кнопка «Повторить»: немедленный forceRefresh, backoff сброшен. */
    fun retryWeather() {
        retryNow()
    }

    /** UI должен звать на ON_START/ON_RESUME: экран снова активен. */
    fun onAppForeground() {
        val wasForeground = _uiState.value.isAppForeground
        _uiState.update { it.copy(isAppForeground = true) }
        if (wasForeground) return
        val state = _uiState.value
        val needsRetry = state.weather == null ||
            state.weatherState == WeatherDataState.UNAVAILABLE ||
            state.retryAttempt > 0
        if (needsRetry) {
            retryNow()
        } else if (state.nextRetryAt != null) {
            scheduleRetryJob(state.nextRetryAt)
        }
    }

    /** UI должен звать на ON_STOP/ON_PAUSE: фон не долбит сеть. */
    fun onAppBackground() {
        if (!_uiState.value.isAppForeground) return
        retryJob?.cancel()
        loadJob?.cancel()
        trafficJob?.cancel()
        savedCitiesJob?.cancel()
        _uiState.update {
            it.copy(
                isAppForeground = false,
                isRetrying = false,
                nextRetryAt = null,
                isRefreshing = false
            )
        }
    }

    /** Планирует единственный backoff-retry; при смене города/фоне отменяется. */
    private fun scheduleRetryJob(nextRetryAt: Long) {
        retryJob?.cancel()
        val delayMs = (nextRetryAt - System.currentTimeMillis()).coerceAtLeast(0L)
        retryJob = viewModelScope.launch(Dispatchers.IO) {
            delay(delayMs)
            val state = _uiState.value
            if (!state.isAppForeground || !state.isRetrying ||
                state.nextRetryAt != nextRetryAt
            ) {
                return@launch
            }
            val city = state.selectedCity ?: return@launch
            _uiState.update { it.copy(nextRetryAt = null) }
            fetchWeatherQuietly(city, forceRefresh = true)
        }
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
                        weatherAttributionRu = WeatherUiReducer.cachedAttribution(cached.source),
                        weatherSourceNote = null
                    )
                } else {
                    state
                }
            }
        }
    }

    private fun fetchWeatherQuietly(city: CityLocation, forceRefresh: Boolean = false) {
        loadJob?.cancel()
        trafficJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val isConnected = networkMonitor.isCurrentlyConnected()
            _uiState.update { state ->
                if (state.selectedCity?.name == city.name) {
                    state.copy(
                        isRefreshing = true,
                        isLoading = state.weather == null,
                        nextRetryAt = null
                    )
                } else {
                    state
                }
            }

            val result = try {
                repository.getWeatherData(
                    city = city,
                    isOnline = isConnected,
                    forceRefresh = forceRefresh
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                WeatherLoadResult.Unavailable(WeatherErrors.from(e))
            }

            _uiState.update { state ->
                if (state.selectedCity?.name != city.name) {
                    return@update state
                }
                // isRefreshing снимаем здесь же: иначе флаг «залипал» и кнопка «Повторить»
                // навсегда оставалась в состоянии «Обновляем…» и была disabled (run 4).
                val applied = WeatherUiReducer.apply(state, result, isConnected)
                    .copy(isRefreshing = false)
                    .let { appliedState ->
                        // Реальные пробки нового прогноза сразу влияют на цену такси.
                        appliedState.copy(
                            taxiState = appliedState.taxiState.withRoute(
                                distanceKm = appliedState.taxiState.selectedDestination.distanceKm,
                                trafficScore = appliedState.weather?.trafficScore
                            )
                        )
                    }
                val previousRetry = WeatherRetryState(
                    attempt = state.retryAttempt,
                    isRetrying = state.isRetrying,
                    nextRetryAt = state.nextRetryAt
                )
                val now = System.currentTimeMillis()
                val retry = when (result) {
                    is WeatherLoadResult.Live -> WeatherRetryPolicy.onSuccess()

                    is WeatherLoadResult.Unavailable -> if (!isConnected) {
                        WeatherRetryPolicy.onBackground(previousRetry)
                    } else {
                        WeatherRetryPolicy.onFailure(
                            state = previousRetry,
                            now = now,
                            hasCache = applied.weather != null,
                            foreground = state.isAppForeground
                        )
                    }

                    is WeatherLoadResult.Cached -> when {
                        !isConnected -> WeatherRetryPolicy.onBackground(previousRetry)
                        result.reason?.reason == WeatherErrorReason.CACHE_POLICY ||
                            result.reason == null -> WeatherRetryPolicy.onSuccess()
                        else -> WeatherRetryPolicy.onFailure(
                            state = previousRetry,
                            now = now,
                            hasCache = true,
                            foreground = state.isAppForeground
                        )
                    }
                }
                applied.copy(
                    isRetrying = retry.isRetrying,
                    nextRetryAt = retry.nextRetryAt,
                    retryAttempt = retry.attempt
                )
            }

            val afterResult = _uiState.value
            val nextRetryAt = afterResult.nextRetryAt
            if (afterResult.selectedCity?.name == city.name &&
                afterResult.isAppForeground &&
                nextRetryAt != null
            ) {
                scheduleRetryJob(nextRetryAt)
            }

            val savedCities = repository.getSavedCitiesWeather(_uiState.value.cities)
            _uiState.update { it.copy(savedCities = savedCities) }

            // Пробки — отдельная корутина: прогноз уже на экране, цифры придут позже.
            if (result is WeatherLoadResult.Live) {
                trafficJob = viewModelScope.launch(Dispatchers.IO) {
                    val traffic = repository.getTraffic(city)
                    _uiState.update { state ->
                        if (state.selectedCity?.name == city.name && state.weather != null) {
                            val weatherWithTraffic = state.weather.copy(
                                trafficScore = traffic.score,
                                trafficAdvice = traffic.description
                            )
                            state.copy(
                                weather = weatherWithTraffic,
                                taxiState = state.taxiState.withRoute(
                                    distanceKm = state.taxiState.selectedDestination.distanceKm,
                                    trafficScore = weatherWithTraffic.trafficScore
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

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
        super.onCleared()
    }

    /**
     * «Список»: сначала честный кэш из памяти/DataStore (как раньше), затем
     * в фоне догружаем погоду для городов без данных (D4 с устройства).
     *
     * Новый вызов отменяет предыдущую догрузку; при уходе с вкладки её
     * отменяет [selectTab]. В сеть идут только строки без данных, максимум
     * 12 городов и не более 3 параллельных запросов.
     */
    fun refreshSavedCities() {
        savedCitiesJob?.cancel()
        savedCitiesJob = viewModelScope.launch(Dispatchers.IO) {
            val cities = _uiState.value.cities.take(SavedCitiesWeatherSync.MAX_CITIES)
            val cached = repository.getSavedCitiesWeather(cities)
            _uiState.update { it.copy(savedCities = cached, isLoadingSavedCities = false) }

            val pending = SavedCitiesWeatherSync.pendingCities(cached)
            if (pending.isEmpty()) return@launch
            // Без сети не ждём провайдерских таймаутов: «Нет данных» честно.
            if (!networkMonitor.isCurrentlyConnected()) return@launch

            SavedCitiesWeatherSync.loadPending(
                cities = pending,
                load = { city ->
                    when (val result = repository.getWeatherData(city = city, isOnline = true)) {
                        is WeatherLoadResult.Live -> result.data
                        is WeatherLoadResult.Cached -> result.data
                        is WeatherLoadResult.Unavailable -> null
                    }
                },
                onLoaded = { city, weather ->
                    _uiState.update { state ->
                        state.copy(
                            savedCities = SavedCitiesWeatherSync.applyLoaded(
                                saved = state.savedCities,
                                city = city,
                                weather = weather
                            )
                        )
                    }
                }
            )
        }
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
        // Вуаль возвращается не «один раз за запуск»: после каждого стирания
        // снова показываем её через случайные 20–30 с.
        startCloudLockTimer(cloudReturnDelayMs())
    }

    /** Явный сброс (например, тестовая кнопка): снова показать вуаль. */
    fun lockClouds() {
        // Вуаль уже на экране — запланированный возврат больше не нужен.
        cloudTimerJob?.cancel()
        _uiState.update { it.copy(isCloudLocked = true, isCloudsCleared = false) }
        viewModelScope.launch(Dispatchers.IO) {
            cache.setCloudsCleared(false)
        }
    }

    fun onCloudsCleared() {
        unlockClouds()
        // Полноэкранная реклама показывается по событию «облака стёрты»,
        // а не по ручному вызову из UI: один поп-ап на одно стирание.
        requestAdPopup()
    }

    // ------------------------------------------------------------------------
    // Navigation Tabs (5 Tabs: HOME, MAP, THEMES, LIST, SEARCH)
    // ------------------------------------------------------------------------
    fun selectTab(tab: BottomTab) {
        // Догрузка «Списка» не должна жить после ухода с вкладки.
        if (tab != BottomTab.LIST) savedCitiesJob?.cancel()
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

    /**
     * Полноэкранный рекламный поп-ап (ТЗ п. 4–5).
     *
     * Вызывается только по событию «облака стёрты» ([onCloudsCleared]).
     * Первый показ в жизни пользователя — обязательно ролик `ad_lion`:
     * флаг `first_ad_shown` в DataStore выставляется сразу после выбора,
     * поэтому повторно лев «первым» не выйдет даже после force-stop.
     * Дальше креатив выбирается случайно: 50 % видео / 50 % фото, и последний
     * показанный id исключается — один и тот же креатив не идёт два раза подряд.
     *
     * Замок [adPopupGuard] и проверка `isAdPopupVisible` дают ровно один
     * поп-ап на одно стирание: два события подряд не наложат два диалога.
     */
    fun requestAdPopup() {
        if (_uiState.value.isAdPopupVisible) return
        if (!adPopupGuard.compareAndSet(false, true)) return
        adPopupJob?.cancel()
        adPopupJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val firstShown = cache.isFirstAdShown()
                val creative = if (firstShown) {
                    adRepository.popupCreative(lastShownId = lastAdPopupCreativeId)
                } else {
                    cache.setFirstAdShown(true)
                    adRepository.firstShowCreative()
                }
                // Запоминаем показанное: следующий выбор исключит этот id.
                lastAdPopupCreativeId = creative.id
                _uiState.update { state ->
                    state.copy(adCreative = creative, isAdPopupVisible = true)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // DataStore недоступен: реклама не должна ронять главный экран.
            } finally {
                adPopupGuard.set(false)
            }
        }
    }

    /** Закрытие поп-апа: крестик или автозакрытие после конца видео. */
    fun dismissAdPopup() {
        _uiState.update { it.copy(isAdPopupVisible = false) }
    }

    /**
     * Баннер главной ротируется между фото1 и фото2 (ТЗ п. 3). Таймер один,
     * живёт в `viewModelScope`: уход с главной и пересоздание композиции
     * не плодят таймеры, а повторный запуск стартует с фото1.
     */
    private fun startAdBannerRotation() {
        adBannerJob?.cancel()
        adBannerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(AD_BANNER_ROTATION_MS)
                adBannerSequence += 1
                val creative = adRepository.bannerCreativeFor(adBannerSequence)
                _uiState.update { state -> state.copy(adBannerCreative = creative) }
            }
        }
    }

    // ------------------------------------------------------------------------
    // AI Facts
    // ------------------------------------------------------------------------
    /**
     * «Другой факт».
     *
     * 1-е нажатие — как раньше, новый текстовый факт. 2-е, 3-е и 4-е нажатие
     * показывают картинку-совет `advice_1` → `advice_2` → `advice_3` вместо текста;
     * после 4-го нажатия кнопка «Другой факт» скрывается до перезапуска приложения
     * ([anotherFactTapCount] живёт в памяти VM, в DataStore не пишется).
     */
    fun refreshAiFact() {
        val tap = anotherFactTapCount + 1
        anotherFactTapCount = tap
        _uiState.update { state ->
            when {
                tap in 2..4 -> state.copy(
                    aiFactImageRes = ANOTHER_FACT_IMAGES[tap - 2],
                    isAnotherFactAvailable = tap < 4
                )
                // Кнопка уже скрыта — защита от повторных вызовов через тап по блоку.
                tap > 4 -> state
                else -> {
                    val nextIndex = FactOfDayPolicy.nextIndex(
                        currentIndex = state.aiFactIndex,
                        size = aiFactsRepository.getFactCount()
                    )
                    state.copy(
                        aiFactIndex = nextIndex,
                        aiFact = aiFactsRepository.getFactAt(nextIndex)
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Lootbox Service
    // ------------------------------------------------------------------------
    fun openLootbox() {
        // «Замок» ставится атомарно ДО запуска корутины: второй быстрый тап видит
        // isOpening = true и не запускает второй дроп. Раньше флаг выставлялся
        // внутри IO-корутины, поэтому два тапа успевали выдать две награды,
        // а вторая запись истории терялась при перезаписи.
        while (true) {
            val state = _uiState.value
            if (state.lootboxState.isOpening) return
            val opening = state.copy(lootboxState = state.lootboxState.copy(isOpening = true))
            if (_uiState.compareAndSet(state, opening)) break
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(1200) // Spin animation delay
                // Только реальный прогноз из уже полученных данных: завтрашний день,
                // а если его нет — сегодняшний (тогда приз честно скажет «на сегодня»).
                val daily = _uiState.value.weather?.daily
                val tomorrow = daily?.getOrNull(1) ?: daily?.getOrNull(0)
                val reward = lootboxRepository.openLootbox(tomorrow)
                val unlockedThemes = lootboxRepository.getUnlockedThemes()
                // Счётчик попыток — из репозитория: додеп вернул попытку, реклама
                // добавляла её отдельным методом, а списание уже персистнуто.
                val attemptsUsed = lootboxRepository.getAttemptsUsed()
                _uiState.update { state ->
                    state.copy(
                        // Дроп только разблокирует тему. currentTheme не трогаем:
                        // фон переключает setTheme() по кнопке «Применить», а не лутбокс.
                        availableThemes = unlockedThemes,
                        themeCatalog = ThemeCatalog.build(unlockedThemes),
                        // seq растёт на каждый дроп: UI ловит один «выстрел» анимации.
                        // null = попытки исчерпаны: приз не выдаём, но «замок» отпускаем,
                        // а attemptsLeft в состоянии станет 0 — по нему UI показывает
                        // «Попытки закончились».
                        lootboxState = if (reward == null) {
                            state.lootboxState.copy(
                                isOpening = false,
                                attemptsUsed = attemptsUsed
                            )
                        } else {
                            state.lootboxState
                                .withDrop(reward, unlockedThemes)
                                .copy(attemptsUsed = attemptsUsed)
                        }
                    )
                }
            } finally {
                // Снимаем замок и при отмене/ошибке, чтобы кнопка не залипла
                // в loading. После withDrop флаг уже false — тогда это no-op.
                _uiState.update { state ->
                    if (!state.lootboxState.isOpening) state
                    else state.copy(lootboxState = state.lootboxState.copy(isOpening = false))
                }
            }
        }
    }

    /**
     * Кнопка «+1 попытка за рекламу»: начисление персистентное (DataStore),
     * верхняя граница бонусов — в [LootboxAttemptsPolicy]. Сценарий призов
     * не сдвигается: индекс следующего дропа считается по истории.
     */
    fun grantAdAttempt() {
        viewModelScope.launch(Dispatchers.IO) {
            lootboxRepository.grantAdAttempt()
            val attemptsUsed = lootboxRepository.getAttemptsUsed()
            _uiState.update { state ->
                state.copy(
                    lootboxState = state.lootboxState.copy(attemptsUsed = attemptsUsed)
                )
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
        _uiState.update {
            // Цена и время — всегда для текущего маршрута и реальных пробок.
            val taxi = it.taxiState.withRoute(
                distanceKm = it.taxiState.selectedDestination.distanceKm,
                trafficScore = it.weather?.trafficScore
            )
            it.copy(taxiState = taxi.copy(selectedTariff = tariff))
        }
    }

    fun selectTaxiDestination(destination: TaxiDestination) {
        _uiState.update {
            val taxi = it.taxiState.withRoute(
                distanceKm = destination.distanceKm,
                trafficScore = it.weather?.trafficScore
            )
            it.copy(taxiState = taxi.copy(selectedDestination = destination))
        }
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
                        searchError = result.failure.messageWithTimeRu
                    )
                }
            }
        }
    }

    /** Отмена активного поиска (кнопка «Отмена» в UI); Call.cancel() внутри. */
    fun cancelSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(isSearching = false) }
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

    /**
     * Добавление города из поиска или из списка (item 6).
     *
     * Дедупликация по имени+координатам: повторный выбор того же места не создаёт
     * дубликат, а просто выбирает существующий город. Новый город сразу становится
     * выбранным, сохраняется в `saved_cities_json` и его погода грузится.
     */
    fun addCity(city: CityLocation) {
        val current = _uiState.value
        val plan = CityCatalog.planAdd(current.cities, city)
        if (plan == null) {
            // Лимит списка не ломаем: честно сообщаем, почему город не добавлен.
            _uiState.update {
                it.copy(
                    searchError = "В списке уже ${CityCatalog.MAX_SAVED_CITIES} городов — " +
                        "больше не помещается."
                )
            }
            return
        }

        val selected = plan.cities[plan.selectedIndex]
        // Выбор города уводит с «Списка» на главную: фоновую догрузку списка
        // останавливаем, чтобы её ответы не конкурировали с загрузкой города.
        savedCitiesJob?.cancel()
        _uiState.update {
            it.copy(
                cities = plan.cities,
                selectedCity = selected,
                selectedCityIndex = plan.selectedIndex,
                currentCityIndex = plan.selectedIndex,
                isSelectedCityCurrentLocation = DefaultCities.isGeoCity(selected),
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                searchError = null,
                selectedTab = BottomTab.HOME
            )
        }

        if (plan.isNew) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveCities(plan.cities)
                val saved = repository.getSavedCitiesWeather(plan.cities)
                _uiState.update { it.copy(savedCities = saved) }
            }
        }

        selectCity(plan.selectedIndex)
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    /**
     * Пересчёт цен/времени такси под конкретный маршрут и честные пробки.
     * trafficScore == null — данных нет, наценки нет.
     */
    private fun TaxiState.withRoute(distanceKm: Double, trafficScore: Int?): TaxiState = copy(
        priceByTariff = TaxiPricing.priceByTariff(distanceKm, trafficScore),
        durationMinByTariff = TaxiPricing.durationMinByTariff(distanceKm)
    )

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

    private companion object {
        /**
         * Картинки «Другого факта»: i-я картинка показывается на (i + 2)-м нажатии
         * (2-е → advice_1, 3-е → advice_2, 4-е → advice_3).
         */
        val ANOTHER_FACT_IMAGES = intArrayOf(
            R.drawable.advice_1,
            R.drawable.advice_2,
            R.drawable.advice_3
        )

        /** Первый показ облачной вуали после запуска приложения. */
        const val CLOUD_FIRST_SHOW_DELAY_MS = 4_000L

        /** Нижняя граница возврата вуали после стирания. */
        const val CLOUD_RETURN_MIN_DELAY_MS = 20_000L

        /** Верхняя граница возврата вуали после стирания. */
        const val CLOUD_RETURN_MAX_DELAY_MS = 30_000L

        /** Период ротации баннера главной: фото1 ↔ фото2. */
        const val AD_BANNER_ROTATION_MS = 8_000L
    }
}
