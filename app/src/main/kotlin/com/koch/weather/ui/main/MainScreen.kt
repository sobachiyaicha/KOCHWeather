package com.koch.weather.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koch.weather.BuildConfig
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.BottomTab
import com.koch.weather.data.model.ConditionSegment
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.ImageCategory
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.WeatherDataState
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
import com.koch.weather.ui.components.ThemeRouletteDialog
import com.koch.weather.ui.components.UmbrellaShopDialog
import com.koch.weather.ui.components.WeatherMapScreen
import com.koch.weather.ui.design.KochNavBar
import com.koch.weather.ui.design.KochSkyBackground
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.LocalKochColors
import com.koch.weather.ui.theme.ProvideKochColors
import com.koch.weather.ui.theme.getThemePalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Soft shadow that keeps white RU labels readable on the light glassmorphism cards (R5). */
private val CardTextShadow = TextStyle(
    shadow = Shadow(
        color = Color(0x99000000),
        offset = Offset(1f, 1f),
        blurRadius = 3f
    )
)

private fun formatWeatherTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

/** Порядок блоков ленты: стабильные списки, не пересоздаются на каждую рекомпозицию. */
private val CleanFeedOrder: List<String> = listOf(
    "services_grid", "hourly_card", "daily_card", "ai_fact_block", "premium_banner", "ad_banner"
)
private val PromoFeedOrder: List<String> = listOf(
    "ai_fact_block", "premium_banner", "ad_banner", "services_grid", "hourly_card", "daily_card"
)

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isThemeRouletteOpen by remember { mutableStateOf(false) }
    var isPaywallOpen by remember { mutableStateOf(false) }
    var isAdPromoOpen by remember { mutableStateOf(false) }
    var isLootboxOpen by remember { mutableStateOf(false) }
    var selectedDayDetail by remember { mutableStateOf<DailyItem?>(null) }
    // Облачная вуаль — один раз за сессию. Латч живёт здесь, чтобы не сбрасываться при
    // уходе с главной на другую вкладку (там MainScreenContent выходит из композиции).
    var cloudVeilDismissed by rememberSaveable { mutableStateOf(false) }

    // «Список» must show fresh cached values every time the tab is opened.
    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab == BottomTab.LIST) {
            viewModel.refreshSavedCities()
        }
    }

    // Провайдер темы поднят на уровень MainScreen: листы и полноэкранные страницы открываются
    // из этого composable, а не из MainScreenContent, и обязаны видеть ту же палитру.
    ProvideKochColors(uiState.currentTheme) {
        // Рендерим только активный экран: на карте главная больше не остаётся скомпонованной
        // под MapView (дефект perf-qa) — экономит композицию и GPU-слой.
        if (uiState.selectedTab == BottomTab.HOME) {
            MainScreenContent(
                uiState = uiState,
                cloudVeilDismissed = cloudVeilDismissed,
                onCloudVeilDismissed = { cloudVeilDismissed = true },
                onRefresh = { viewModel.refresh() },
        onRetryWeather = { viewModel.retryWeather() },
        onSelectCityIndex = { viewModel.selectCityByIndex(it) },
        onSegmentSelected = { viewModel.setSegment(it) },
        onTabSelected = { tab ->
            // R13/R14/R15/R12 are full-screen destinations driven by selectedTab;
            // the old per-tab dialogs are gone.
            viewModel.setTab(tab)
        },
        onRatingChanged = { stars -> viewModel.rateWeather(stars) },
        onCloudsCleared = { viewModel.onCloudsCleared() },
        onRefreshAiFact = { viewModel.refreshAiFact() },
        onOpenPaywall = { isPaywallOpen = true },
        onOpenAdPromo = { isAdPromoOpen = true },
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
        },
        modifier = modifier
            )
        }

    // Modals and Sheets
    if (uiState.isUmbrellaShopOpen) {
        UmbrellaShopDialog(
            onDismiss = { viewModel.closeUmbrellaShop() }
        )
    }

    // Full-screen destinations: Карта (R12), Список (R13), Поиск (R14), Вид (R15).
    val themeColors = getThemePalette(uiState.currentTheme)
    when (uiState.selectedTab) {
        BottomTab.MAP -> {
            val mapCity = uiState.selectedCity
                ?: uiState.cities.getOrNull(uiState.selectedCityIndex)
                ?: uiState.cities.firstOrNull()
            if (mapCity != null) {
                WeatherMapScreen(
                    cityName = uiState.weather?.cityName ?: mapCity.name,
                    latitude = mapCity.latitude,
                    longitude = mapCity.longitude,
                    currentTemp = uiState.weather?.currentTemp,
                    conditionRu = uiState.weather?.conditionDescription,
                    onClose = { viewModel.setTab(BottomTab.HOME) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF101820)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет данных о городе для карты",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { viewModel.setTab(BottomTab.HOME) }
                    )
                }
            }
        }

        BottomTab.LIST -> {
            CityListScreen(
                cities = uiState.savedCities,
                selectedCityName = uiState.selectedCity?.name,
                isLoading = uiState.isLoadingSavedCities,
                background = themeColors.gradientBrush,
                attribution = uiState.weatherAttributionRu,
                attributionNote = uiState.weatherSourceNote,
                onRefresh = { viewModel.refreshSavedCities() },
                onSelectCity = { savedCity -> viewModel.addCity(savedCity.city) },
                onBack = { viewModel.setTab(BottomTab.HOME) }
            )
        }

        BottomTab.SEARCH -> {
            CitySearchScreen(
                query = uiState.searchQuery,
                results = uiState.searchResults,
                isSearching = uiState.isSearching,
                errorMessage = uiState.searchError,
                background = themeColors.gradientBrush,
                onQueryChange = { viewModel.searchCities(it) },
                onSelectCity = { city -> viewModel.addCity(city) },
                onBack = {
                    viewModel.clearSearch()
                    viewModel.setTab(BottomTab.HOME)
                }
            )
        }

        BottomTab.THEMES -> {
            ThemeGalleryScreen(
                themes = uiState.themeCatalog,
                currentTheme = uiState.currentTheme,
                background = themeColors.gradientBrush,
                attribution = uiState.weatherAttributionRu,
                attributionNote = uiState.weatherSourceNote,
                onSelectTheme = { theme -> viewModel.setTheme(theme) },
                onOpenRoulette = { isThemeRouletteOpen = true },
                onBack = { viewModel.setTab(BottomTab.HOME) }
            )
        }

        BottomTab.HOME -> Unit
    }

    if (isThemeRouletteOpen) {
        ThemeRouletteDialog(
            currentTheme = uiState.currentTheme,
            availableThemes = uiState.availableThemes,
            iphoneImageUrl = uiState.imageUrls[ImageCategory.IPHONE_FRONT]?.firstOrNull(),
            onSelectTheme = { theme -> viewModel.setTheme(theme) },
            onDismiss = {
                // Opened from the «Вид» screen: stay there after closing the roulette.
                isThemeRouletteOpen = false
            }
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
            onOpenPaywall = {
                isAdPromoOpen = false
                isPaywallOpen = true
            },
            onDismiss = { isAdPromoOpen = false },
            imageUrl = uiState.imageUrls[ImageCategory.AD_LAPTOP]?.firstOrNull(),
            offer = uiState.premiumOffer
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
            onOpenLootbox = { viewModel.openLootbox() },
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
            onDismiss = { selectedDayDetail = null }
        )
    }
    }
}

@Composable
internal fun MainScreenContent(
    uiState: MainScreenUiState,
    cloudVeilDismissed: Boolean,
    onCloudVeilDismissed: () -> Unit,
    onRefresh: () -> Unit,
    onRetryWeather: () -> Unit,
    onSelectCityIndex: (Int) -> Unit,
    onSegmentSelected: (ConditionSegment) -> Unit,
    onTabSelected: (BottomTab) -> Unit,
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
    val themeColors = getThemePalette(uiState.currentTheme)
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var showDebugFps by remember { mutableStateOf(false) }
    // После стирания облаков — короткий кулдаун: хвост того же жеста (и следующие быстрые
    // свайпы) не должен листать город (дефект run 8: 4 свайпа → Дубай).
    var veilCooldownUntil by remember { mutableLongStateOf(0L) }
    // Позиция ленты не переживает уход с главной: возвращаемся всегда к шапке.
    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }

    ProvideKochColors(uiState.currentTheme) {
        val koch = LocalKochColors.current

        // Вуаль показываем только когда есть что скрывать (weather != null) и один раз за сессию.
        val veilVisible = uiState.isCloudVeilVisible &&
            !cloudVeilDismissed &&
            uiState.weather != null

        // Статус-бар: тёмные иконки на светлом небе/закате и под облаками (там всегда светлый
        // фон), светлые — на тёмных темах. Иначе «белым по белому».
        val view = androidx.compose.ui.platform.LocalView.current
        val useDarkStatusIcons = koch.bgTop.luminance() > 0.35f || veilVisible
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
            // Фон темы: фото неба из state (data-coder) с фолбэком на градиент+облака.
            // Голубая — кучевые облака, медная — закат, «Северное сияние» — полярное небо.
            val skyPhotoUrl = when (uiState.currentTheme) {
                AppThemeMode.BLUE_SKY -> uiState.skyBlueImageUrl
                AppThemeMode.COPPER_SUNSET -> uiState.skySunsetImageUrl
                AppThemeMode.AURORA_BOREALIS -> uiState.skyNorthernImageUrl
                else -> null
            }
            KochSkyBackground(
                modifier = Modifier.matchParentSize(),
                imageUrl = skyPhotoUrl
            )

            // Нижний бар — часть Scaffold, а не оверлей: контент физически не может уехать
            // под него (блокер №1 из аудита r2_02).
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        KochNavBar(
                            selectedTab = uiState.selectedTab,
                            onTabSelected = onTabSelected
                        )
                    }
                }
            ) { innerPadding ->
                // Контент живёт ВЫШЕ бара: вьюпорт списка укорачивается на высоту нижнего бара,
                // поэтому карточки не рисуются под ним даже в середине скролла (дефект run 3).
                LazyColumn(
                    // Обычный remember (не rememberSaveable): при возврате с «Карты»/«Вида»
                    // главная открывается сверху, а не в сохранённой позиции у футера —
                    // иначе кажется, что сервисные блоки «пропали» (дефект run 4b).
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = innerPadding.calculateBottomPadding()),
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
                // поверх, поэтому шапка остаётся в ленте.
                item(key = "golden_hero_header") {
                    GoldenCapsuleHeader(
                        weather = weather,
                        citiesCount = uiState.cities.size,
                        selectedCityIndex = uiState.selectedCityIndex,
                        onSelectCityIndex = onSelectCityIndex,
                        onHeaderClick = onOpenCityPicker
                    )
                    Spacer(modifier = Modifier.height(KochSpace.m))
                }

                // Голубая тема — порядок clean_main/bottom_nav: погода сразу под шапкой.
                // Остальные темы — «перегруженный» порядок theme_copper/f_063: промо сверху.
                val cleanOrder = uiState.currentTheme == AppThemeMode.BLUE_SKY
                val feedOrder = if (cleanOrder) CleanFeedOrder else PromoFeedOrder

                feedOrder.forEach { key ->
                    if (key == "ad_banner" && !uiState.showAds) return@forEach
                    item(key = key) {
                        when (key) {
                            "hourly_card" -> HourlyWeatherCard(
                                weather = weather,
                                selectedSegment = uiState.selectedSegment,
                                userRating = uiState.userRating,
                                showRatingThankYou = uiState.showRatingThankYou,
                                onSegmentSelected = onSegmentSelected,
                                onRatingChanged = onRatingChanged
                            )
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
                                onClick = onOpenGrassModal
                            )
                            "premium_banner" -> PremiumBanner(
                                isPremiumPurchased = uiState.isPremiumPurchased,
                                offer = uiState.premiumOffer,
                                onOpenPaywall = onOpenPaywall
                            )
                            "ad_banner" -> AdBannerCard(
                                imageUrl = uiState.imageUrls[ImageCategory.AD_LAPTOP]?.firstOrNull(),
                                onClick = onOpenAdPromo
                            )
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
                // Вуаль НЕ должна накрывать нижний бар: считаем высоту бара + системный инсет
                // и оставляем эту полосу вне оверлея. Иначе в состоянии «нет данных» табы
                // перестают нажиматься (дефект run 4).
                val density = LocalDensity.current
                val barsHeight = with(density) {
                    (WindowInsets.navigationBars.getBottom(density) + 88.dp.roundToPx()).toDp()
                }
                CloudVeilOverlay(
                    onCleared = {
                        // 1200 мс на «хвост» жеста и быстрые добивающие свайпы.
                        veilCooldownUntil = System.currentTimeMillis() + 1200L
                        onCloudVeilDismissed()
                        onCloudsCleared()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = barsHeight)
                )
            }
        }
    }
}

