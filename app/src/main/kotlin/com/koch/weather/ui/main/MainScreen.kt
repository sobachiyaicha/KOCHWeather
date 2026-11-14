package com.koch.weather.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koch.weather.BuildConfig
import com.koch.weather.data.model.AdAction
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.BottomTab
import com.koch.weather.data.model.ConditionSegment
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.ImageCategory
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.WeatherDataState
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.backgroundRes
import com.koch.weather.ui.components.AdPromoDialog
import com.koch.weather.ui.components.CityListScreen
import com.koch.weather.ui.components.CitySearchScreen
import com.koch.weather.ui.components.CloudVeilOverlay
import com.koch.weather.ui.components.DailyDetailDialog
import com.koch.weather.ui.components.DataSourceFooter
import com.koch.weather.ui.components.FlightBookingDialog
import com.koch.weather.ui.components.FoodBottomSheet
import com.koch.weather.ui.components.FpsOverlay
import com.koch.weather.ui.components.GrassLockDialog
import com.koch.weather.ui.components.LootboxDialog
import com.koch.weather.ui.components.PaywallDialog
import com.koch.weather.ui.components.TaxiBottomSheet
import com.koch.weather.ui.components.ThemeGalleryScreen
import com.koch.weather.ui.components.UmbrellaShopDialog
import com.koch.weather.ui.components.WeatherMapScreen
import com.koch.weather.ui.design.KochNavBar
import com.koch.weather.ui.design.KochSkyBackground
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.ProvideKochColors
import com.koch.weather.ui.theme.getThemePalette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatWeatherTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

/** Порядок блоков ленты: стабильные списки, не пересоздаются на каждую рекомпозицию. */
private val CleanFeedOrder: List<String> = listOf(
    "services_grid", "hourly_card", "daily_card", "ai_fact_block", "premium_banner", "ad_banner"
)
private val PromoFeedOrder: List<String> = listOf(
    "ai_fact_block", "premium_banner", "ad_banner", "services_grid", "hourly_card", "daily_card"
)

/**
 * Снимок города для `AnimatedContent` (M8 r15). Раньше `targetState` был индексом, а
 * content-лямбда читала `uiState.weather`/`uiState.selectedCityIndex` снаружи — оба слоя
 * перехода рисовали уже новое состояние (в `r14_03_swipe_a/b` оба слоя были «Москва»/
 * «Санкт-Петербург»). Теперь слой рендерится из своего снимка: выходящий держит прежний
 * город, входящий — новый.
 */
private data class CitySnapshot(
    val index: Int,
    val weather: WeatherDomainData,
    val citiesCount: Int,
    val isCurrentLocation: Boolean
)

/** Тот же снимок для карточки условий: сегмент и оценка тоже не должны «перескакивать». */
private data class HourlySnapshot(
    val index: Int,
    val weather: WeatherDomainData,
    val selectedSegment: ConditionSegment,
    val userRating: Int?,
    val averageRating: Float,
    val showRatingThankYou: Boolean
)

/**
 * Переход между городами (item 5): лёгкий слайд по направлению + fade 280 мс.
 * `SizeTransform(clip = false)` — шапка/карточка не обрезаются во время смены высоты.
 * Направление берётся из индекса снимка: сами снимки несравнимы по порядку.
 */
private fun <T> AnimatedContentTransitionScope<T>.citySwitchTransition(
    indexOf: (T) -> Int
): ContentTransform {
    val durationMs = 280
    val forward = indexOf(targetState) > indexOf(initialState)
    val enter = slideInHorizontally(animationSpec = tween(durationMs)) { width ->
        if (forward) width / 4 else -width / 4
    } + fadeIn(animationSpec = tween(durationMs))
    val exit = slideOutHorizontally(animationSpec = tween(durationMs)) { width ->
        if (forward) -width / 4 else width / 4
    } + fadeOut(animationSpec = tween(durationMs))
    return ContentTransform(
        enter,
        exit,
        0f,
        SizeTransform(clip = false)
    )
}

// JVM-эрозия: две перегрузки по типу получателя дают один и тот же JVM-дескриптор,
// поэтому у слоёв разные имена функций, а не перегрузка.
private fun AnimatedContentTransitionScope<CitySnapshot>.cityHeaderTransition(): ContentTransform =
    citySwitchTransition { it.index }

private fun AnimatedContentTransitionScope<HourlySnapshot>.cityHourlyTransition(): ContentTransform =
    citySwitchTransition { it.index }

/** Один фон темы на вкладку: полоса под навбаром не остаётся прозрачной. */
@Composable
private fun TabBackground(
    background: Brush,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(background)) {
        content()
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isPaywallOpen by remember { mutableStateOf(false) }
    var isAdPromoOpen by remember { mutableStateOf(false) }
    var isLootboxOpen by remember { mutableStateOf(false) }
    var selectedDayDetail by remember { mutableStateOf<DailyItem?>(null) }
    // Облачная вуаль — один раз за сессию. Латч живёт здесь, чтобы не сбрасываться при
    // уходе с главной на другую вкладку (там MainScreenContent выходит из композиции).
    var cloudVeilDismissed by rememberSaveable { mutableStateOf(false) }
    // Позиция ленты живёт на уровне MainScreen: повторный тап по «Главная» скроллит наверх,
    // а возврат с другой вкладки по-прежнему открывает главную сверху.
    val homeListState = remember { LazyListState() }
    val coroutineScope = rememberCoroutineScope()

    // «Список» must show fresh cached values every time the tab is opened.
    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab == BottomTab.LIST) {
            viewModel.refreshSavedCities()
        }
    }

    // Системный «назад» на не-HOME вкладке возвращает на главную, а не закрывает приложение.
    BackHandler(enabled = uiState.selectedTab != BottomTab.HOME) {
        if (uiState.selectedTab == BottomTab.SEARCH) viewModel.clearSearch()
        viewModel.setTab(BottomTab.HOME)
    }

    // Провайдер темы поднят на уровень MainScreen: листы и полноэкранные страницы открываются
    // из этого composable, а не из MainScreenContent, и обязаны видеть ту же палитру.
    ProvideKochColors(uiState.currentTheme) {
        val themeColors = getThemePalette(uiState.currentTheme)

        val onTabSelected: (BottomTab) -> Unit = { tab ->
            when {
                // Переход на другую вкладку — обычная навигация.
                tab != uiState.selectedTab -> viewModel.setTab(tab)
                // Повторный тап по активной «Главной» — к началу ленты.
                tab == BottomTab.HOME -> coroutineScope.launch {
                    homeListState.animateScrollToItem(0)
                }
                // Повторный тап по активной не-главной вкладке ничего не делает.
                else -> Unit
            }
        }

        // ОДИН Scaffold на весь MainScreen: навбар не пропадает ни на одной вкладке
        // (item 3). Контент вкладки живёт внутри content, бар — в bottomBar.
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Box(
                    modifier = Modifier
                        // D-1 (feature-qa, run 15): при `enableEdgeToEdge()` клавиатура —
                        // оверлей над окном, `adjustResize` окно не сжимает. Навбар оставался
                        // под IME: тап «в его зону» попадал в клавиатуру/поле и печатал
                        // («Moscow,» на вкладке «Поиск»). Поднимаем бар над клавиатурой;
                        // `union` берёт max(IME, навбар) — двойного отступа нет.
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    KochNavBar(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = onTabSelected
                    )
                }
            }
        ) { innerPadding ->
            val bottomContentPadding = innerPadding.calculateBottomPadding()
            when (uiState.selectedTab) {
                BottomTab.HOME -> MainScreenContent(
                    uiState = uiState,
                    listState = homeListState,
                    bottomContentPadding = bottomContentPadding,
                    cloudVeilDismissed = cloudVeilDismissed,
                    onCloudVeilDismissed = { cloudVeilDismissed = true },
                    onRefresh = { viewModel.refresh() },
                    onRetryWeather = { viewModel.retryWeather() },
                    onSelectCityIndex = { viewModel.selectCityByIndex(it) },
                    onSegmentSelected = { viewModel.setSegment(it) },
                    onRatingChanged = { stars -> viewModel.rateWeather(stars) },
                    onCloudsCleared = { viewModel.onCloudsCleared() },
                    onRefreshAiFact = { viewModel.refreshAiFact() },
                    onOpenPaywall = { isPaywallOpen = true },
                    onOpenAdPromo = {
                        // Креатив переключается на стороне VM (dayOfYear + число открытий).
                        viewModel.openAdPromo()
                        isAdPromoOpen = true
                    },
                    onOpenFlight = { viewModel.openFlightSheet() },
                    onOpenLootbox = { isLootboxOpen = true },
                    onOpenTaxi = { viewModel.openTaxiSheet() },
                    onOpenFood = { viewModel.openFoodSheet() },
                    onOpenCityPicker = {
                        // Тап по шапке открывает полноэкранный «Список» — один язык вместо двух диалогов.
                        viewModel.setTab(BottomTab.LIST)
                    },
                    onOpenUmbrellaShop = { viewModel.openUmbrellaShop() },
                    onOpenGrassModal = { viewModel.setGrassModal(true) },
                    onDayClick = { day -> selectedDayDetail = day },
                    onNextCity = {
                        if (uiState.cities.isNotEmpty()) {
                            val nextIndex = (uiState.selectedCityIndex + 1) % uiState.cities.size
                            viewModel.selectCityByIndex(nextIndex)
                        }
                    },
                    onPrevCity = {
                        if (uiState.cities.isNotEmpty()) {
                            val prevIndex = if (uiState.selectedCityIndex - 1 < 0) {
                                uiState.cities.size - 1
                            } else {
                                uiState.selectedCityIndex - 1
                            }
                            viewModel.selectCityByIndex(prevIndex)
                        }
                    }
                )

                BottomTab.MAP -> {
                    val mapCity = uiState.selectedCity
                        ?: uiState.cities.getOrNull(uiState.selectedCityIndex)
                        ?: uiState.cities.firstOrNull()
                    TabBackground(background = themeColors.gradientBrush) {
                        if (mapCity != null) {
                            WeatherMapScreen(
                                cityName = uiState.weather?.cityName ?: mapCity.name,
                                latitude = mapCity.latitude,
                                longitude = mapCity.longitude,
                                currentTemp = uiState.weather?.currentTemp,
                                conditionRu = uiState.weather?.conditionDescription,
                                modifier = Modifier.padding(bottom = bottomContentPadding)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = bottomContentPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Нет данных о городе для карты",
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Выберите город на вкладке «Главная»",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                BottomTab.LIST -> TabBackground(background = themeColors.gradientBrush) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomContentPadding)
                    ) {
                        CityListScreen(
                            cities = uiState.savedCities,
                            selectedCityName = uiState.selectedCity?.name,
                            isLoading = uiState.isLoadingSavedCities,
                            background = themeColors.gradientBrush,
                            attribution = uiState.weatherAttributionRu,
                            attributionNote = uiState.weatherSourceNote,
                            onRefresh = { viewModel.refreshSavedCities() },
                            onSelectCity = { savedCity -> viewModel.addCity(savedCity.city) },
                            onAddCity = { viewModel.setTab(BottomTab.SEARCH) }
                        )
                    }
                }

                BottomTab.SEARCH -> TabBackground(background = themeColors.gradientBrush) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomContentPadding)
                    ) {
                        CitySearchScreen(
                            query = uiState.searchQuery,
                            results = uiState.searchResults,
                            savedCities = uiState.cities,
                            isSearching = uiState.isSearching,
                            errorMessage = uiState.searchError,
                            background = themeColors.gradientBrush,
                            onQueryChange = { viewModel.searchCities(it) },
                            onSelectCity = { city -> viewModel.addCity(city) }
                        )
                    }
                }

                BottomTab.THEMES -> TabBackground(background = themeColors.gradientBrush) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomContentPadding)
                    ) {
                        ThemeGalleryScreen(
                            themes = uiState.themeCatalog,
                            currentTheme = uiState.currentTheme,
                            background = themeColors.gradientBrush,
                            attribution = uiState.weatherAttributionRu,
                            attributionNote = uiState.weatherSourceNote,
                            onSelectTheme = { theme -> viewModel.setTheme(theme) }
                        )
                    }
                }
            }
        }

        // Modals and Sheets
        if (uiState.isUmbrellaShopOpen) {
            UmbrellaShopDialog(
                onDismiss = { viewModel.closeUmbrellaShop() }
            )
        }

        if (isPaywallOpen) {
            PaywallDialog(
                isPremiumPurchased = uiState.isPremiumPurchased,
                isPending = uiState.isPremiumPurchasePending,
                offer = uiState.premiumOffer,
                onRequestPurchase = { viewModel.requestPremiumPurchase() },
                onConfirmPurchase = { viewModel.confirmPremiumPurchase() },
                onCancelPending = { viewModel.cancelPremiumPurchase() },
                onResetPremium = { viewModel.resetPremium() },
                onDismiss = { isPaywallOpen = false }
            )
        }

        if (isAdPromoOpen) {
            AdPromoDialog(
                creative = uiState.adCreative,
                imageUrl = uiState.adImageUrl,
                onAction = { action ->
                    // Лист рекламы закрывается, CTA ведёт в конкретную функцию приложения.
                    isAdPromoOpen = false
                    when (action) {
                        AdAction.PAYWALL -> isPaywallOpen = true
                        AdAction.LOOTBOX -> isLootboxOpen = true
                        AdAction.UMBRELLA_SHOP -> viewModel.openUmbrellaShop()
                        AdAction.FOOD -> viewModel.openFoodSheet()
                    }
                },
                onDismiss = { isAdPromoOpen = false }
            )
        }

        if (uiState.flightState.isSheetOpen) {
            FlightBookingDialog(
                flightState = uiState.flightState,
                onSelectOffer = { viewModel.selectFlightOffer(it) },
                onSelectSeat = { viewModel.selectSeat(it) },
                onBookFlight = { viewModel.bookFlight(it) },
                onDismiss = { viewModel.closeFlightSheet() },
                heroImageUrl = uiState.imageUrls[ImageCategory.TRAVEL_DUBAI]?.firstOrNull()
            )
        }

        if (isLootboxOpen) {
            LootboxDialog(
                lootboxState = uiState.lootboxState,
                availableThemes = uiState.availableThemes,
                currentTheme = uiState.currentTheme,
                onOpenLootbox = { viewModel.openLootbox() },
                onApplyTheme = { theme -> viewModel.setTheme(theme) },
                onDismiss = { isLootboxOpen = false }
            )
        }

        if (uiState.taxiState.isSheetOpen) {
            val taxiCity = uiState.selectedCity
                ?: uiState.cities.getOrNull(uiState.selectedCityIndex)
            TaxiBottomSheet(
                taxiState = uiState.taxiState,
                trafficScore = uiState.weather?.trafficScore,
                trafficAdvice = uiState.weather?.trafficAdvice?.takeIf { it.isNotBlank() }
                    ?: "Нет данных о пробках",
                city = taxiCity,
                onSelectTariff = { viewModel.selectTaxiTariff(it) },
                onSelectDestination = { viewModel.selectTaxiDestination(it) },
                onCancelTaxi = { viewModel.cancelTaxi() },
                onDismiss = { viewModel.closeTaxiSheet() }
            )
        }

        if (uiState.foodState.isSheetOpen) {
            FoodBottomSheet(
                foodState = uiState.foodState,
                photoUrls = uiState.imageUrls,
                onSelectRestaurant = { viewModel.selectRestaurant(it) },
                onAddToCart = { viewModel.addToCart(it) },
                onRemoveFromCart = { viewModel.removeFromCart(it) },
                onCancelOrder = { viewModel.cancelFoodOrder() },
                onDismiss = { viewModel.closeFoodSheet() }
            )
        }

        if (uiState.isGrassModalOpen) {
            GrassLockDialog(
                onGrassUploaded = { viewModel.onGrassUploaded() },
                onDismiss = { viewModel.setGrassModal(false) }
            )
        }

        selectedDayDetail?.let { day ->
            DailyDetailDialog(
                item = day,
                cityName = uiState.weather?.cityName ?: uiState.selectedCity?.name ?: "Город",
                windSpeedMps = uiState.weather?.windSpeed,
                onDismiss = { selectedDayDetail = null }
            )
        }
    }
}

@Composable
internal fun MainScreenContent(
    uiState: MainScreenUiState,
    listState: LazyListState,
    /** Высота нижнего бара из Scaffold: лента и вуаль не заходят под навигацию. */
    bottomContentPadding: Dp,
    cloudVeilDismissed: Boolean,
    onCloudVeilDismissed: () -> Unit,
    onRefresh: () -> Unit,
    onRetryWeather: () -> Unit,
    onSelectCityIndex: (Int) -> Unit,
    onSegmentSelected: (ConditionSegment) -> Unit,
    onRatingChanged: (Int) -> Unit,
    onCloudsCleared: () -> Unit,
    onRefreshAiFact: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenAdPromo: () -> Unit,
    onOpenFlight: () -> Unit,
    onOpenLootbox: () -> Unit,
    onOpenTaxi: () -> Unit,
    onOpenFood: () -> Unit,
    onOpenCityPicker: () -> Unit,
    onOpenUmbrellaShop: () -> Unit,
    onOpenGrassModal: () -> Unit,
    onDayClick: (DailyItem) -> Unit,
    onNextCity: () -> Unit,
    onPrevCity: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var showDebugFps by remember { mutableStateOf(false) }
    // После стирания облаков — короткий кулдаун: хвост того же жеста (и следующие быстрые
    // свайпы) не должен листать город (дефект run 8: 4 свайпа → Дубай).
    var veilCooldownUntil by remember { mutableLongStateOf(0L) }

    ProvideKochColors(uiState.currentTheme) {
        // Вуаль показываем только когда есть что скрывать (weather != null) и один раз за сессию.
        val veilVisible = uiState.isCloudVeilVisible &&
            !cloudVeilDismissed &&
            uiState.weather != null

        // Снимки для перехода между городами (M8 r15). Считаются здесь, в composable-скоупе:
        // content-лямбда `LazyColumn` не композируемая, `remember` внутри неё недоступен.
        // Ключ — сам город и его данные: смена индекса даёт новый снимок (слайд), а простое
        // обновление прогноза того же города оставляет `contentKey` прежним (без слайда).
        val weatherForSnapshot = uiState.weather
        val citySnapshot = remember(
            weatherForSnapshot,
            uiState.selectedCityIndex,
            uiState.cities.size,
            uiState.isSelectedCityCurrentLocation
        ) {
            weatherForSnapshot?.let { snapshotWeather ->
                CitySnapshot(
                    index = uiState.selectedCityIndex,
                    weather = snapshotWeather,
                    citiesCount = uiState.cities.size,
                    isCurrentLocation = uiState.isSelectedCityCurrentLocation
                )
            }
        }
        val hourlySnapshot = remember(
            weatherForSnapshot,
            uiState.selectedCityIndex,
            uiState.selectedSegment,
            uiState.userRating,
            uiState.rating,
            uiState.showRatingThankYou
        ) {
            weatherForSnapshot?.let { snapshotWeather ->
                HourlySnapshot(
                    index = uiState.selectedCityIndex,
                    weather = snapshotWeather,
                    selectedSegment = uiState.selectedSegment,
                    userRating = uiState.userRating,
                    averageRating = uiState.rating,
                    showRatingThankYou = uiState.showRatingThankYou
                )
            }
        }

        // Статус-бар: фон главной — всегда локальное фото темы под скримом, поэтому
        // яркость нельзя вывести из `bgTop` палитры (он не про фото). Белые иконки
        // читаются на затемнённом фото во всех темах; тёмные нужны только под
        // белой облачной вуалью.
        val view = androidx.compose.ui.platform.LocalView.current
        val useDarkStatusIcons = veilVisible
        LaunchedEffect(useDarkStatusIcons) {
            val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
            androidx.core.view.WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = useDarkStatusIcons
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(
                    uiState.cities.size,
                    uiState.selectedCityIndex,
                    uiState.isCloudLocked,
                    uiState.isCloudsCleared,
                    cloudVeilDismissed,
                    veilCooldownUntil
                ) {
                    // Пока облака не стёрты — жест принадлежит им: карусель городов НЕ листаем
                    // (дефект run 3: свайп стирания уводил на Дубай).
                    if (uiState.isCloudVeilVisible && !cloudVeilDismissed) {
                        return@pointerInput
                    }
                    // Хвост того же жеста: пелена ушла на 1–2 свайпе, а палец продолжает
                    // двигаться — эти drag'и тоже не должны листать город (дефект run 8).
                    val pagerBlocked: () -> Boolean = {
                        System.currentTimeMillis() < veilCooldownUntil
                    }
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            if (!pagerBlocked()) {
                                dragAccumulator += dragAmount
                            }
                        },
                        onDragEnd = {
                            if (!pagerBlocked()) {
                                if (dragAccumulator < -60f) {
                                    onNextCity()
                                } else if (dragAccumulator > 60f) {
                                    onPrevCity()
                                }
                            }
                            dragAccumulator = 0f
                        },
                        onDragCancel = {
                            dragAccumulator = 0f
                        }
                    )
                }
        ) {
            // Фон темы: локальный блюр-фон из ресурсов (data-coder: ThemeBackgrounds).
            // Никакой сети и Coil — фон есть всегда, в правильном разрешении.
            KochSkyBackground(
                modifier = Modifier.matchParentSize(),
                imageRes = uiState.currentTheme.backgroundRes
            )

            LazyColumn(
                // Обычный remember на уровне MainScreen: при возврате с «Карты»/«Вида»
                // главная открывается сверху, а не в сохранённой позиции у футера —
                // иначе кажется, что сервисные блоки «пропали» (дефект run 4b).
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    // Контент живёт ВЫШЕ бара: вьюпорт списка укорачивается на его высоту,
                    // поэтому карточки не рисуются под ним даже в середине скролла.
                    .padding(bottom = bottomContentPadding),
                contentPadding = PaddingValues(
                    top = 6.dp,
                    bottom = 8.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Offline/cache banner is driven by the weather state, not by a bare flag:
                // CACHED -> saved forecast with its timestamp, UNAVAILABLE -> no data, LIVE -> none.
                val offlineBannerText: String? = when (uiState.weatherState) {
                    WeatherDataState.CACHED -> buildString {
                        append("Сохранённый прогноз")
                        uiState.weatherLastUpdated?.let { timestamp ->
                            append(", ")
                            append(formatWeatherTime(timestamp))
                        }
                    }

                    WeatherDataState.UNAVAILABLE -> "Нет данных о погоде"
                    else -> null
                }
                if (offlineBannerText != null) {
                    val bannerColor = if (uiState.weatherState == WeatherDataState.UNAVAILABLE) {
                        Color(0xD9E53935)
                    } else {
                        Color(0xD9EF6C00)
                    }
                    item(key = "offline_banner") {
                        Surface(
                            color = bannerColor,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, Color(0x60FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable { onRefresh() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = offlineBannerText,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (uiState.isRefreshing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 1.5.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val weather = uiState.weather
                if (weather != null) {
                    // Section 0: шапка-капсула (refs/bottom_nav.png, 0:45). Облачная вуаль рисуется
                    // поверх, поэтому шапка остаётся в ленте. Анимация — только шапка и
                    // карточка условий: весь LazyColumn анимировать нельзя (jank).
                    item(key = "golden_hero_header") {
                        // M8: каждый слой рисуется из своего снимка — выходящий слой держит
                        // прежний город (СПб), входящий — новый (Москва).
                        citySnapshot?.let { snapshot ->
                            AnimatedContent(
                                targetState = snapshot,
                                transitionSpec = { cityHeaderTransition() },
                                contentKey = { it.index },
                                label = "city_header_transition"
                            ) { snap ->
                                GoldenCapsuleHeader(
                                    weather = snap.weather,
                                    citiesCount = snap.citiesCount,
                                    selectedCityIndex = snap.index,
                                    isCurrentLocation = snap.isCurrentLocation,
                                    onSelectCityIndex = onSelectCityIndex,
                                    onHeaderClick = onOpenCityPicker
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(KochSpace.m))
                    }

                    // Голубая тема — порядок clean_main/bottom_nav: погода сразу под шапкой.
                    // Остальные темы — «перегруженный» порядок theme_copper/f_063: промо сверху.
                    val cleanOrder = uiState.currentTheme == AppThemeMode.BLUE_SKY
                    val feedOrder = if (cleanOrder) CleanFeedOrder else PromoFeedOrder

                    feedOrder.forEach { key ->
                        if (key == "ad_banner" && !uiState.showAds) return@forEach
                        item(key = key) {
                            if (key == "hourly_card") {
                                // Тот же приём: снимок города+сегмента+оценки на слой.
                                hourlySnapshot?.let { snapshot ->
                                    AnimatedContent(
                                        targetState = snapshot,
                                        transitionSpec = { cityHourlyTransition() },
                                        contentKey = { it.index },
                                        label = "city_hourly_transition"
                                    ) { snap ->
                                        HourlyWeatherCard(
                                            weather = snap.weather,
                                            selectedSegment = snap.selectedSegment,
                                            userRating = snap.userRating,
                                            averageRating = snap.averageRating,
                                            showRatingThankYou = snap.showRatingThankYou,
                                            onSegmentSelected = onSegmentSelected,
                                            onRatingChanged = onRatingChanged
                                        )
                                    }
                                }
                            } else {
                                when (key) {
                                    "daily_card" -> TenDayForecastCard(
                                        dailyList = weather.daily,
                                        currentTemp = weather.currentTemp,
                                        onDayClick = onDayClick,
                                        onOpenUmbrellaShop = onOpenUmbrellaShop
                                    )

                                    "services_grid" -> ServicesGrid(
                                        onOpenFlight = onOpenFlight,
                                        onOpenLootbox = onOpenLootbox,
                                        onOpenTaxi = onOpenTaxi,
                                        onOpenFood = onOpenFood
                                    )

                                    "ai_fact_block" -> AiFactCard(
                                        aiFact = uiState.aiFact,
                                        // Тап (и строка «Другой факт») — новый факт;
                                        // модалка травы больше не живёт на этом блоке.
                                        onClick = onRefreshAiFact,
                                        onLongClick = onOpenGrassModal
                                    )

                                    "premium_banner" -> PremiumBanner(
                                        isPremiumPurchased = uiState.isPremiumPurchased,
                                        offer = uiState.premiumOffer,
                                        onOpenPaywall = onOpenPaywall
                                    )

                                    "ad_banner" -> AdBannerCard(
                                        creative = uiState.adCreative,
                                        imageUrl = uiState.adImageUrl,
                                        onClick = onOpenAdPromo
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(KochSpace.m))
                        }
                    }

                    // Attribution: the label and the failover/cache note come from the contract
                    // (MET Norway / Open-Meteo / cache), never hardcoded in the UI.
                    if (uiState.weatherAttributionRu.isNotBlank()) {
                        item(key = "data_source_footer") {
                            // Долгое нажатие по футеру — скрытый debug-жест для FPS-метра.
                            Box(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { showDebugFps = !showDebugFps }
                                    )
                                }
                            ) {
                                DataSourceFooter(
                                    text = uiState.weatherAttributionRu,
                                    note = uiState.weatherSourceNote
                                )
                            }
                        }
                    }
                } else if (uiState.isLoading) {
                    item(key = "loading_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                } else {
                    // Honest empty state: no synthetic numbers, explicit retry.
                    item(key = "weather_unavailable") {
                        WeatherUnavailableCard(
                            errorMessage = uiState.weatherErrorMessage,
                            isRefreshing = uiState.isRefreshing,
                            onRetry = onRetryWeather
                        )
                    }
                    if (uiState.weatherAttributionRu.isNotBlank()) {
                        item(key = "data_source_footer_empty") {
                            Box(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { showDebugFps = !showDebugFps }
                                    )
                                }
                            ) {
                                DataSourceFooter(
                                    text = uiState.weatherAttributionRu,
                                    note = uiState.weatherSourceNote
                                )
                            }
                        }
                    }
                }
            }

            // Debug-only FPS meter: выключен по умолчанию, включается долгим нажатием по
            // строке источника данных. В приёмочных скриншотах его быть не должно.
            if (BuildConfig.DEBUG && showDebugFps) {
                FpsOverlay(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, bottom = 96.dp)
                )
            }

            // Облачная вуаль (refs/cloud_swipe.png, 2:40) — ровно один раз за сессию.
            if (veilVisible) {
                // Вуаль НЕ должна накрывать нижний бар: он теперь снаружи, в Scaffold,
                // а bottomContentPadding — его точная высота (включая navigationBars).
                // Иначе в состоянии «нет данных» табы перестают нажиматься (дефект run 4).
                CloudVeilOverlay(
                    onCleared = {
                        // 1200 мс на «хвост» жеста и быстрые добивающие свайпы.
                        veilCooldownUntil = System.currentTimeMillis() + 1200L
                        onCloudVeilDismissed()
                        onCloudsCleared()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomContentPadding)
                )
            }
        }
    }
}
