package com.koch.weather.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koch.weather.BuildConfig
import com.koch.weather.data.model.BottomTab
import com.koch.weather.data.model.ConditionSegment
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.HourlyItem
import com.koch.weather.data.model.ImageCategory
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.WeatherDataState
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherIconType
import com.koch.weather.ui.components.AdPromoDialog
import com.koch.weather.ui.components.CityListScreen
import com.koch.weather.ui.components.CityPickerDialog
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
import com.koch.weather.ui.components.TShirtIcon
import com.koch.weather.ui.components.TaxiBottomSheet
import com.koch.weather.ui.components.TemperatureRangeBar
import com.koch.weather.ui.components.ThemeGalleryScreen
import com.koch.weather.ui.components.ThemeRouletteDialog
import com.koch.weather.ui.components.UmbrellaShopDialog
import com.koch.weather.ui.components.WeatherIcon
import com.koch.weather.ui.components.WeatherMapScreen
import com.koch.weather.ui.components.RemotePhoto
import com.koch.weather.ui.theme.getThemePalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isCityPickerOpen by remember { mutableStateOf(false) }
    var isThemeRouletteOpen by remember { mutableStateOf(false) }
    var isPaywallOpen by remember { mutableStateOf(false) }
    var isAdPromoOpen by remember { mutableStateOf(false) }
    var isLootboxOpen by remember { mutableStateOf(false) }
    var selectedDayDetail by remember { mutableStateOf<DailyItem?>(null) }

    // «Список» must show fresh cached values every time the tab is opened.
    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab == BottomTab.LIST) {
            viewModel.refreshSavedCities()
        }
    }

    MainScreenContent(
        uiState = uiState,
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
        onToggleHourlyExpanded = { viewModel.toggleHourlyExpanded() },
        onCloudsCleared = { viewModel.onCloudsCleared() },
        onRefreshAiFact = { viewModel.refreshAiFact() },
        onOpenPaywall = { isPaywallOpen = true },
        onOpenAdPromo = { isAdPromoOpen = true },
        onOpenFlight = { viewModel.openFlightSheet() },
        onOpenLootbox = { isLootboxOpen = true },
        onOpenTaxi = { viewModel.openTaxiSheet() },
        onOpenFood = { viewModel.openFoodSheet() },
        onOpenCityPicker = { isCityPickerOpen = true },
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

    // Modals and Sheets
    if (isCityPickerOpen) {
        CityPickerDialog(
            cities = uiState.cities,
            selectedCity = uiState.selectedCity,
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            onSearchQueryChange = { viewModel.searchCities(it) },
            onSelectCity = { city ->
                viewModel.addCity(city)
                isCityPickerOpen = false
            },
            onDismiss = {
                viewModel.clearSearch()
                isCityPickerOpen = false
            }
        )
    }

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
            cityName = uiState.weather?.cityName ?: uiState.selectedCity?.name ?: "Город",
            currentTemp = uiState.weather?.currentTemp,
            iphoneImageUrl = uiState.imageUrls[ImageCategory.IPHONE]?.firstOrNull(),
            onSelectTheme = { theme ->
                viewModel.setTheme(theme)
            },
            onDismiss = {
                // Opened from the «Вид» screen: stay there after closing the roulette.
                isThemeRouletteOpen = false
            }
        )
    }

    if (isPaywallOpen) {
        PaywallDialog(
            isPremiumPurchased = uiState.isPremiumPurchased,
            onPurchase = {
                viewModel.purchasePremium()
                isPaywallOpen = false
            },
            onReset = {
                viewModel.resetPremium()
            },
            onDismiss = { isPaywallOpen = false }
        )
    }

    if (isAdPromoOpen) {
        AdPromoDialog(
            onOpenPaywall = {
                isAdPromoOpen = false
                isPaywallOpen = true
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
            onDismiss = { viewModel.closeFlightSheet() }
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
            foodImageUrls = uiState.imageUrls[ImageCategory.FOOD].orEmpty(),
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

@Composable
internal fun MainScreenContent(
    uiState: MainScreenUiState,
    onRefresh: () -> Unit,
    onRetryWeather: () -> Unit,
    onSelectCityIndex: (Int) -> Unit,
    onSegmentSelected: (ConditionSegment) -> Unit,
    onTabSelected: (BottomTab) -> Unit,
    onRatingChanged: (Int) -> Unit,
    onToggleHourlyExpanded: () -> Unit,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = themeColors.gradientBrush)
            .pointerInput(uiState.cities.size, uiState.selectedCityIndex) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        if (dragAccumulator < -60f) {
                            onNextCity()
                        } else if (dragAccumulator > 60f) {
                            onPrevCity()
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = {
                        dragAccumulator = 0f
                    }
                )
            }
    ) {
        // High performance LazyColumn with stable keys.
        // navigationBarsPadding + 96dp bottom contentPadding keep the last cards and the
        // segments row fully above the floating bottom bar (bug from device audit_01/audit_02).
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp),
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
                // Section 1: Golden Capsule Header. The cloud veil is rendered on top of the
                // whole screen (see below), so the header stays in the feed underneath it.
                item(key = "golden_hero_header") {
                    GoldenCapsuleHeader(
                        weather = weather,
                        citiesCount = uiState.cities.size,
                        selectedCityIndex = uiState.selectedCityIndex,
                        onSelectCityIndex = onSelectCityIndex,
                        onHeaderClick = onOpenCityPicker
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Section 2: AI Fact Block (refs/premium_ads.png)
                item(key = "ai_fact_block") {
                    AiFactCard(
                        aiFact = uiState.aiFact,
                        onClick = onOpenGrassModal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Section 3: Premium Subscription Banner (refs/premium_ads.png)
                item(key = "premium_banner") {
                    PremiumBanner(
                        isPremiumPurchased = uiState.isPremiumPurchased,
                        onOpenPaywall = onOpenPaywall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Section 4: Satirical Ad Banner (refs/premium_ads.png)
                if (uiState.showAds) {
                    item(key = "ad_banner") {
                        AdBannerCard(
                            imageUrl = uiState.imageUrls[ImageCategory.AD_LAPTOP]?.firstOrNull(),
                            onClick = onOpenAdPromo
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Section 5: 2x2 Services Grid (refs/cards_travel.png, refs/cards_taxi_food.png)
                item(key = "services_grid") {
                    ServicesGrid(
                        travelImageUrl = uiState.imageUrls[ImageCategory.TRAVEL_DUBAI]?.firstOrNull(),
                        onOpenFlight = onOpenFlight,
                        onOpenLootbox = onOpenLootbox,
                        onOpenTaxi = onOpenTaxi,
                        onOpenFood = onOpenFood,
                        cardBackground = themeColors.cardBackground,
                        cardBorder = themeColors.cardBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Section 6: Card 1 (5 Segments + Condition/Hourly + Rate Stars)
                item(key = "hourly_card") {
                    HourlyWeatherCard(
                        weather = weather,
                        selectedSegment = uiState.selectedSegment,
                        isHourlyExpanded = uiState.isHourlyExpanded,
                        userRating = uiState.userRating,
                        showRatingThankYou = uiState.showRatingThankYou,
                        onSegmentSelected = onSegmentSelected,
                        onToggleHourlyExpanded = onToggleHourlyExpanded,
                        onRatingChanged = onRatingChanged,
                        cardBackground = themeColors.cardBackground,
                        cardBorder = themeColors.cardBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Section 7: Card 2 (10-Day Forecast with Umbrella Shop Banner)
                item(key = "daily_card") {
                    TenDayForecastCard(
                        dailyList = weather.daily,
                        currentTemp = weather.currentTemp,
                        onDayClick = onDayClick,
                        onOpenUmbrellaShop = onOpenUmbrellaShop,
                        cardBackground = themeColors.cardBackground,
                        cardBorder = themeColors.cardBorder
                    )
                }

                // Attribution: the label and the failover/cache note come from the contract
                // (MET Norway / Open-Meteo / cache), never hardcoded in the UI.
                if (uiState.weatherAttributionRu.isNotBlank()) {
                    item(key = "data_source_footer") {
                        DataSourceFooter(
                            text = uiState.weatherAttributionRu,
                            note = uiState.weatherSourceNote
                        )
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
                        DataSourceFooter(
                            text = uiState.weatherAttributionRu,
                            note = uiState.weatherSourceNote
                        )
                    }
                }
            }
        }

        // Full-Size Bottom Navigation Bar (5 tabs: Главная, Карта, Вид, Список, Поиск)
        FullSizeBottomBar(
            selectedTab = uiState.selectedTab,
            onTabSelected = onTabSelected,
            barBackground = Color(0xF0151E28),
            barBorder = Color(0x60FFFFFF),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 14.dp, end = 14.dp)
        )

        // Debug-only FPS meter. Anchored just above the floating bar (not over the header,
        // the offline banner or the cloud hint) and hidden until the first real measurement.
        if (BuildConfig.DEBUG) {
            FpsOverlay(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 14.dp, bottom = 92.dp)
            )
        }

        // Full-screen cloud veil: dense clouds cover the whole screen until the user wipes
        // them away with a finger (refs/cloud_swipe.png).
        if (uiState.isCloudLocked && !uiState.isCloudsCleared) {
            CloudVeilOverlay(onCleared = onCloudsCleared)
        }
    }
}

/**
 * Честный плейсхолдер, когда прогноза нет вообще (ни сети, ни кэша):
 * «Нет данных о погоде» + причина (если пришла из контракта) + «Повторить».
 */
@Composable
private fun WeatherUnavailableCard(
    errorMessage: String?,
    isRefreshing: Boolean,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0x33000000),
        border = BorderStroke(0.8.dp, Color(0x30FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Нет данных о погоде",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Не удалось загрузить прогноз. Проверьте подключение к сети и повторите попытку.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                enabled = !isRefreshing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
                    contentColor = Color.White
                )
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Обновляем...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Повторить", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 1. Золотая плашка-капсула + скошенная температура (21°) + точки пагинации (refs/rate_stars.png, refs/bottom_nav.png)
 */
@Composable
private fun GoldenCapsuleHeader(
    weather: WeatherDomainData,
    citiesCount: Int,
    selectedCityIndex: Int,
    onSelectCityIndex: (Int) -> Unit,
    onHeaderClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Container for golden capsule + slanted temperature
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
        ) {
            // Golden capsule pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHeaderClick
                    ),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFFE8B649),
                border = BorderStroke(1.dp, Color(0x88FFE082))
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE5A737), Color(0xFFF3C757), Color(0xFFE5A737))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Condition + min/max are laid out on their own line under the city, so the
                    // big slanted temperature (top-right, never moved) cannot overlap them.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ТЕКУЩЕЕ МЕСТО",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        val cityFontSize = when {
                            weather.cityName.length > 12 -> 16.5.sp
                            weather.cityName.length > 8 -> 20.sp
                            else -> 26.sp
                        }
                        Text(
                            text = weather.cityName,
                            color = Color.White,
                            fontSize = cityFontSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val conditionFontSize = when {
                                weather.conditionDescription.length > 22 -> 10.sp
                                weather.conditionDescription.length > 16 -> 10.5.sp
                                else -> 11.5.sp
                            }
                            Text(
                                text = weather.conditionDescription,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = conditionFontSize,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Макс.: ${weather.maxTemp}°, мин.: ${weather.minTemp}°",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Slanted temperature (e.g. 21°) overlapping top right of capsule
            Text(
                text = "${weather.currentTemp}°",
                color = Color.White,
                fontSize = 58.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-12).dp, y = (-24).dp)
                    .rotate(-15f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pagination indicators: navigation arrow + dots under the golden capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            // Location arrow
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Текущая геопозиция",
                tint = Color.White,
                modifier = Modifier
                    .size(13.dp)
                    .rotate(-45f)
                    .clip(CircleShape)
                    .clickable { onSelectCityIndex(0) }
            )

            // Interactive dots
            for (i in 0 until citiesCount.coerceAtLeast(1)) {
                val isSelected = i == selectedCityIndex
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isSelected) 16.dp else 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                        .clickable { onSelectCityIndex(i) }
                )
            }
        }
    }
}

/**
 * AI Fact Card (refs/premium_ads.png)
 */
@Composable
private fun AiFactCard(
    aiFact: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x35000000),
        border = BorderStroke(0.8.dp, Color(0x30FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "А знали ли вы...",
                    color = Color(0xFFFFD54F),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = aiFact.trim(),
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Premium Banner (refs/premium_ads.png)
 */
@Composable
private fun PremiumBanner(
    isPremiumPurchased: Boolean,
    onOpenPaywall: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x40000000),
        border = BorderStroke(0.8.dp, Color(0x35FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPaywall)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Больше советов и эксклюзивных функций",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isPremiumPurchased) Color(0xFF2E7D32) else Color(0xFF1976D2),
                border = BorderStroke(0.8.dp, if (isPremiumPurchased) Color(0xFF81C784) else Color(0xFF64B5F6))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPremiumPurchased) {
                        Text(
                            text = "★ ПРЕМИУМ",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Подписка Премиум",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "5.99 BYN",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "29.99",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Green Ad Banner (refs/premium_ads.png)
 */
@Composable
private fun AdBannerCard(
    imageUrl: String?,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1B5E20).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0x40000000)
                ) {
                    Text(
                        text = "РЕКЛАМА",
                        color = Color(0xFFA5D6A7),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Дизайнеры больше не нужны!",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Научим делать сайты с помощью нейросетей за 15 минут",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.5.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            RemotePhoto(
                url = imageUrl,
                contentDescription = "Реклама: ноутбук",
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                placeholderIcon = Icons.Default.LaptopMac,
                placeholderTint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 2x2 Services Grid (refs/cards_travel.png, refs/cards_taxi_food.png)
 */
@Composable
private fun ServicesGrid(
    travelImageUrl: String?,
    onOpenFlight: () -> Unit,
    onOpenLootbox: () -> Unit,
    onOpenTaxi: () -> Unit,
    onOpenFood: () -> Unit,
    cardBackground: Color,
    cardBorder: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Flights & Lootboxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Flights to Dubai
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = BorderStroke(0.8.dp, cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(102.dp)
                    .clickable(onClick = onOpenFlight)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // R17: real Dubai photo from Wikimedia Commons with dark scrim for the
                    // white RU labels; placeholder/error come from RemotePhoto.
                    RemotePhoto(
                        url = travelImageUrl,
                        contentDescription = "Дубай",
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(18.dp),
                        placeholderIcon = Icons.Default.Flight,
                        placeholderTint = Color.White.copy(alpha = 0.7f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x99000000), Color(0x77000000))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Flight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Путешествия",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                            Text("›", color = Color.White, fontSize = 14.sp)
                        }

                        Column {
                            Text(
                                text = "Билеты в Дубай",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                style = CardTextShadow
                            )
                            Text(
                                text = "Вы недавно смотрели...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Card 2: Lootboxes
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = BorderStroke(0.8.dp, cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(102.dp)
                    .clickable(onClick = onOpenLootbox)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Лутбоксы",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                style = CardTextShadow
                            )
                        }
                        Text("›", color = Color.White, fontSize = 14.sp)
                    }

                    Column {
                        Text(
                            text = "Не повезло с погодой?",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            style = CardTextShadow
                        )
                        Text(
                            text = "Повезет тут!",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            style = CardTextShadow
                        )
                    }
                }
            }
        }

        // Row 2: Taxi & Food
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Taxi
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = BorderStroke(0.8.dp, cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(102.dp)
                    .clickable(onClick = onOpenTaxi)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalTaxi,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Такси",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFB300),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Куда едем? ->",
                                color = Color(0xFF1E232A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Card 4: Food Delivery
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = BorderStroke(0.8.dp, cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(102.dp)
                    .clickable(onClick = onOpenFood)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fastfood,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Еда",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFF7043),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Что желаете? ->",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Карточка «Погодные условия» — 5 сегментов в ряд + контент + блок «Оцените погоду»
 */
@Composable
private fun HourlyWeatherCard(
    weather: WeatherDomainData,
    selectedSegment: ConditionSegment,
    isHourlyExpanded: Boolean,
    userRating: Int?,
    showRatingThankYou: Boolean,
    onSegmentSelected: (ConditionSegment) -> Unit,
    onToggleHourlyExpanded: () -> Unit,
    onRatingChanged: (Int) -> Unit,
    cardBackground: Color,
    cardBorder: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardBackground,
        border = BorderStroke(0.8.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // 5-segment pill container (refs/segments.png, refs/rate_stars.png)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x38000000),
                border = BorderStroke(0.6.dp, Color(0x25FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegmentTabItem(
                        title = "Погод. усл.",
                        icon = Icons.Default.Cloud,
                        isSelected = selectedSegment == ConditionSegment.WEATHER,
                        onClick = { onSegmentSelected(ConditionSegment.WEATHER) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentTabItem(
                        title = "Осадки",
                        icon = Icons.Default.WaterDrop,
                        isSelected = selectedSegment == ConditionSegment.RAIN,
                        onClick = { onSegmentSelected(ConditionSegment.RAIN) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentTabItem(
                        title = "Ветер",
                        icon = Icons.Default.Air,
                        isSelected = selectedSegment == ConditionSegment.WIND,
                        onClick = { onSegmentSelected(ConditionSegment.WIND) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentTabItem(
                        title = "Одежда",
                        icon = TShirtIcon,
                        isSelected = selectedSegment == ConditionSegment.CLOTHES,
                        onClick = { onSegmentSelected(ConditionSegment.CLOTHES) },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentTabItem(
                        title = "Пробки",
                        icon = Icons.Default.DirectionsCar,
                        isSelected = selectedSegment == ConditionSegment.TRAFFIC,
                        onClick = { onSegmentSelected(ConditionSegment.TRAFFIC) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle + Expand / Collapse Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedSegment) {
                        ConditionSegment.WEATHER -> "Почасовой прогноз"
                        ConditionSegment.RAIN -> "Вероятность осадков (%)"
                        ConditionSegment.WIND -> "Скорость ветра (м/с)"
                        ConditionSegment.CLOTHES -> "Рекомендации по одежде"
                        ConditionSegment.TRAFFIC -> "Дорожная ситуация"
                    },
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Toggle Button (refs/rate_stars.png "Развернуть v")
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x25FFFFFF),
                    modifier = Modifier.clickable(onClick = onToggleHourlyExpanded)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHourlyExpanded) "Свернуть" else "Развернуть",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (isHourlyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Collapsible Content
            AnimatedVisibility(
                visible = isHourlyExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when (selectedSegment) {
                        ConditionSegment.WEATHER -> {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(weather.hourly, key = { it.hourLabel }) { item ->
                                    HourlyItemColumn(
                                        item = item,
                                        selectedSegment = ConditionSegment.WEATHER
                                    )
                                }
                            }
                        }

                        ConditionSegment.RAIN -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Текущая вероятность осадков:",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp
                                    )
                                    val firstHourly = weather.hourly.firstOrNull()
                                    Text(
                                        text = if (firstHourly != null && firstHourly.popKnown) {
                                            "${firstHourly.pop}%"
                                        } else {
                                            "—"
                                        },
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(weather.hourly, key = { it.hourLabel }) { item ->
                                        HourlyItemColumn(
                                            item = item,
                                            selectedSegment = ConditionSegment.RAIN
                                        )
                                    }
                                }
                            }
                        }

                        ConditionSegment.WIND -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Текущая скорость ветра:",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = "${weather.windSpeed} м/с",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = when {
                                            weather.windSpeed < 5 -> "Слабый ветерок"
                                            weather.windSpeed < 10 -> "Умеренный ветер"
                                            weather.windSpeed < 15 -> "Сильный порывистый"
                                            else -> "Штормовое предупреждение"
                                        },
                                        color = Color(0xFF81D4FA),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(weather.hourly, key = { it.hourLabel }) { item ->
                                        HourlyItemColumn(
                                            item = item,
                                            selectedSegment = ConditionSegment.WIND
                                        )
                                    }
                                }
                            }
                        }

                        ConditionSegment.CLOTHES -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x28FFFFFF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = TShirtIcon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Совет на сегодня",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = weather.clothingAdvice,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        ConditionSegment.TRAFFIC -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x28FFFFFF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Traffic,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Дорожная обстановка",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val trafficScore = weather.trafficScore
                                            if (trafficScore != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFFFA000)
                                                ) {
                                                    Text(
                                                        text = "$trafficScore баллов",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = weather.trafficAdvice,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color(0x18FFFFFF), thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // 3. Блок «Оцените погоду» (refs/rate_stars.png)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Оцените погоду",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showRatingThankYou) {
                                "Спасибо за отзыв! (${userRating ?: 3} ★)"
                            } else if (userRating != null) {
                                "Ваша оценка: ${userRating} ★"
                            } else {
                                "В среднем сегодня: 3.5 ★"
                            },
                            color = if (showRatingThankYou) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f),
                            fontSize = 11.5.sp,
                            fontWeight = if (showRatingThankYou) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // 5 Interactive stars capsule (3 active by default)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x38000000),
                    border = BorderStroke(0.6.dp, Color(0x25FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeRating = userRating ?: 3
                        for (i in 1..5) {
                            val isFilled = i <= activeRating
                            Icon(
                                imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Оценка $i",
                                tint = if (isFilled) Color(0xFFFFD54F) else Color(0x55FFFFFF),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onRatingChanged(i) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Кнопка сегмента в карточке условий (иконка + текст, белый фон при выборе)
 */
@Composable
private fun SegmentTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color(0xFF1E88E5) else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun HourlyItemColumn(
    item: HourlyItem,
    selectedSegment: ConditionSegment
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(44.dp)
    ) {
        Text(
            text = item.hourLabel,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))

        when (selectedSegment) {
            ConditionSegment.WEATHER -> {
                WeatherIcon(iconType = item.iconType, size = 26.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.temp}°",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            ConditionSegment.RAIN -> {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (item.popKnown) "${item.pop}%" else "—",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            ConditionSegment.WIND -> {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = Color(0xFF81D4FA),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.wind.roundToInt()}",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            else -> {}
        }
    }
}

/**
 * 4. Карточка 10-дневного прогноза (refs/rate_stars.png)
 */
@Composable
private fun TenDayForecastCard(
    dailyList: List<DailyItem>,
    currentTemp: Int,
    onDayClick: (DailyItem) -> Unit,
    onOpenUmbrellaShop: () -> Unit,
    cardBackground: Color,
    cardBorder: Color
) {
    val overallMin = remember(dailyList) { dailyList.minOfOrNull { it.minTemp } ?: 10 }
    val overallMax = remember(dailyList) { dailyList.maxOfOrNull { it.maxTemp } ?: 25 }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardBackground,
        border = BorderStroke(0.8.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header: 10-дневный прогноз
            Text(
                text = "10-дневный прогноз",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Day rows
            dailyList.take(10).forEachIndexed { index, item ->
                DailyForecastRow(
                    item = item,
                    overallMin = overallMin,
                    overallMax = overallMax,
                    currentTemp = currentTemp,
                    onClick = { onDayClick(item) }
                )
                if (index < dailyList.size - 1) {
                    HorizontalDivider(
                        color = Color(0x18FFFFFF),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0x25FFFFFF), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Umbrella Shop Row (refs/rate_stars.png)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenUmbrellaShop)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BeachAccess,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Зонтики от 29.99 BYN",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Смотреть",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "›",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    item: DailyItem,
    overallMin: Int,
    overallMax: Int,
    currentTemp: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day label
        Text(
            text = item.dayLabel,
            color = Color.White,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(68.dp)
        )

        // Weather icon
        Box(
            modifier = Modifier.width(36.dp),
            contentAlignment = Alignment.Center
        ) {
            WeatherIcon(iconType = item.iconType, size = 24.dp)
        }

        // Min temperature
        Text(
            text = "${item.minTemp}°",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp)
        )

        // Gradient range bar
        TemperatureRangeBar(
            minTemp = item.minTemp,
            maxTemp = item.maxTemp,
            overallMin = overallMin,
            overallMax = overallMax,
            currentTemp = currentTemp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )

        // Max temperature
        Text(
            text = "${item.maxTemp}°",
            color = Color.White,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier.width(32.dp)
        )
    }
}

/**
 * 5-Tab High-Contrast Bottom Navigation Bar (refs/bottom_nav.png, refs/theme_copper.png)
 * Tabs: Главная, Карта, Вид, Список, Поиск
 */
@Composable
private fun FullSizeBottomBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    barBackground: Color,
    barBorder: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = barBackground,
        border = BorderStroke(1.2.dp, barBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTabItem(
                label = "Главная",
                icon = Icons.Default.Home,
                isSelected = selectedTab == BottomTab.HOME,
                onClick = { onTabSelected(BottomTab.HOME) }
            )
            BottomTabItem(
                label = "Карта",
                icon = Icons.Outlined.Map,
                isSelected = selectedTab == BottomTab.MAP,
                onClick = { onTabSelected(BottomTab.MAP) }
            )
            BottomTabItem(
                label = "Вид",
                icon = Icons.Default.Palette,
                isSelected = selectedTab == BottomTab.THEMES,
                onClick = { onTabSelected(BottomTab.THEMES) }
            )
            BottomTabItem(
                label = "Список",
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                isSelected = selectedTab == BottomTab.LIST,
                onClick = { onTabSelected(BottomTab.LIST) }
            )
            BottomTabItem(
                label = "Поиск",
                icon = Icons.Default.Search,
                isSelected = selectedTab == BottomTab.SEARCH,
                onClick = { onTabSelected(BottomTab.SEARCH) }
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = if (isSelected) 42.dp else 28.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (isSelected) Color(0xFF1E88E5) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
        )
    }
}
