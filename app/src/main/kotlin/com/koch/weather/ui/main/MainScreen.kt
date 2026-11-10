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
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koch.weather.data.model.BottomTab
import com.koch.weather.data.model.ConditionSegment
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.HourlyItem
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherIconType
import com.koch.weather.ui.components.CityPickerDialog
import com.koch.weather.ui.components.DailyDetailDialog
import com.koch.weather.ui.components.FpsOverlay
import com.koch.weather.ui.components.TShirtIcon
import com.koch.weather.ui.components.TemperatureRangeBar
import com.koch.weather.ui.components.UmbrellaShopDialog
import com.koch.weather.ui.components.WeatherIcon
import com.koch.weather.ui.components.WeatherMapDialog
import com.koch.weather.ui.theme.getThemePalette
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isCityPickerOpen by remember { mutableStateOf(false) }
    var isUmbrellaShopOpen by remember { mutableStateOf(false) }
    var isWeatherMapOpen by remember { mutableStateOf(false) }
    var selectedDayDetail by remember { mutableStateOf<DailyItem?>(null) }

    MainScreenContent(
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onSelectCityIndex = { viewModel.selectCityByIndex(it) },
        onSegmentSelected = { viewModel.setSegment(it) },
        onTabSelected = { tab ->
            viewModel.setTab(tab)
            when (tab) {
                BottomTab.HOME -> {}
                BottomTab.MAP -> isWeatherMapOpen = true
                BottomTab.LIST -> isCityPickerOpen = true
                BottomTab.SEARCH -> isCityPickerOpen = true
            }
        },
        onRatingChanged = { stars -> viewModel.rateWeather(stars) },
        onOpenCityPicker = { isCityPickerOpen = true },
        onOpenUmbrellaShop = { viewModel.openUmbrellaShop() },
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

    if (isWeatherMapOpen) {
        WeatherMapDialog(
            cityName = uiState.weather?.cityName ?: uiState.selectedCity?.name ?: "Город",
            currentTemp = uiState.weather?.currentTemp ?: 21,
            onDismiss = {
                isWeatherMapOpen = false
                viewModel.setTab(BottomTab.HOME)
            }
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
    onSelectCityIndex: (Int) -> Unit,
    onSegmentSelected: (ConditionSegment) -> Unit,
    onTabSelected: (BottomTab) -> Unit,
    onRatingChanged: (Int) -> Unit,
    onOpenCityPicker: () -> Unit,
    onOpenUmbrellaShop: () -> Unit,
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
        // High performance LazyColumn with stable keys
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Offline warning banner
            if (uiState.isOffline) {
                item(key = "offline_banner") {
                    Surface(
                        color = Color(0xD9E53935),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.8.dp, Color(0x60FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
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
                                text = "Нет подключения к сети. Показан сохраненный прогноз.",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            val weather = uiState.weather
            if (weather != null) {
                // Section 1: Golden Capsule Header + Slanted Temperature + Dots
                item(key = "golden_hero_header") {
                    GoldenCapsuleHeader(
                        weather = weather,
                        citiesCount = uiState.cities.size,
                        selectedCityIndex = uiState.selectedCityIndex,
                        onSelectCityIndex = onSelectCityIndex,
                        onHeaderClick = onOpenCityPicker
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Section 2: Card 1 (5 Segments + Condition/Hourly + Rate Stars)
                item(key = "hourly_card") {
                    HourlyWeatherCard(
                        weather = weather,
                        selectedSegment = uiState.selectedSegment,
                        userRating = uiState.userRating,
                        showRatingThankYou = uiState.showRatingThankYou,
                        onSegmentSelected = onSegmentSelected,
                        onRatingChanged = onRatingChanged,
                        cardBackground = themeColors.cardBackground,
                        cardBorder = themeColors.cardBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Section 3: Card 2 (10-Day Forecast with Umbrella Shop Banner)
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
            }
        }

        // Full-Size Bottom Navigation Bar (4 tabs: Главная, Карта, Список, Поиск)
        FullSizeBottomBar(
            selectedTab = uiState.selectedTab,
            onTabSelected = onTabSelected,
            barBackground = themeColors.bottomBarBackground,
            barBorder = themeColors.cardBorder,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
        )

        // Floating FPS Overlay in top left corner
        FpsOverlay(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp)
        )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: uppercase label + large city name
                        Column(modifier = Modifier.weight(1.25f)) {
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
                        }

                        // Right: condition + min/max range
                        Column(
                            modifier = Modifier.weight(1.05f),
                            horizontalAlignment = Alignment.End
                        ) {
                            val conditionFontSize = when {
                                weather.conditionDescription.length > 18 -> 8.5.sp
                                weather.conditionDescription.length > 14 -> 9.5.sp
                                else -> 11.sp
                            }
                            Text(
                                text = weather.conditionDescription,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = conditionFontSize,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(1.dp))
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
 * 2. Карточка «Погодные условия» — 5 сегментов в ряд + контент + блок «Оцените погоду»
 */
@Composable
private fun HourlyWeatherCard(
    weather: WeatherDomainData,
    selectedSegment: ConditionSegment,
    userRating: Int?,
    showRatingThankYou: Boolean,
    onSegmentSelected: (ConditionSegment) -> Unit,
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

            // Subtitle
            Text(
                text = when (selectedSegment) {
                    ConditionSegment.WEATHER -> "Температура (°C)"
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

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic content depending on selected segment
            when (selectedSegment) {
                ConditionSegment.WEATHER -> {
                    // Standard 6-column hourly weather row
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
                    // High-contrast Precipitation Block
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
                            val currentPop = weather.hourly.firstOrNull()?.pop ?: 0
                            Text(
                                text = "$currentPop%",
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
                    // High-contrast Wind Block
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
                                    text = "Скорость ветра",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${weather.windSpeed} м/с",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Порывы до",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                val gust = (weather.windSpeed * 1.4).roundToInt()
                                Text(
                                    text = "$gust м/с",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
                    // Clothing advice card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x28FFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🧥", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Совет по гардеробу на сегодня",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = weather.clothingAdvice,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                ConditionSegment.TRAFFIC -> {
                    // Traffic situation card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x28FFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🚦", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Дорожная обстановка",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFA000)
                                    ) {
                                        Text(
                                            text = "${weather.trafficScore} баллов",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
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

            Spacer(modifier = Modifier.height(6.dp))
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

                // 5 Interactive stars capsule
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
                tint = if (isSelected) Color(0xFF222222) else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF222222) else Color.White.copy(alpha = 0.8f),
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
        modifier = Modifier.width(52.dp)
    ) {
        Text(
            text = item.hourLabel,
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        WeatherIcon(
            iconType = if (selectedSegment == ConditionSegment.RAIN) {
                WeatherIconType.RAIN
            } else {
                item.iconType
            },
            size = 24.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        val valueText = when (selectedSegment) {
            ConditionSegment.WEATHER -> "${item.temp}°"
            ConditionSegment.RAIN -> "${item.pop}%"
            ConditionSegment.WIND -> "${item.wind.roundToInt()} м/с"
            else -> "${item.temp}°"
        }

        Text(
            text = valueText,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 4. Карточка 10-дневного прогноза со вставкой «Зонтики» между Пн и Вт (refs/rate_stars.png)
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
    if (dailyList.isEmpty()) return

    val overallMin = dailyList.minOf { it.minTemp }
    val overallMax = dailyList.maxOf { it.maxTemp }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardBackground,
        border = BorderStroke(0.8.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            dailyList.forEachIndexed { index, item ->
                DailyForecastRow(
                    item = item,
                    currentTemp = if (index == 0) currentTemp else null,
                    overallMin = overallMin,
                    overallMax = overallMax,
                    onClick = { onDayClick(item) }
                )

                // Insert Umbrella Offer between Monday (index 1) and Tuesday (index 2)
                if (index == 1) {
                    HorizontalDivider(
                        color = Color(0x1AFFFFFF),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Umbrella banner row
                    UmbrellaOfferRow(onClick = onOpenUmbrellaShop)
                }

                if (index < dailyList.lastIndex) {
                    HorizontalDivider(
                        color = Color(0x1AFFFFFF),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Полоса «🌂 Зонтики от 29.99 BYN / Смотреть ›» (refs/rate_stars.png)
 */
@Composable
private fun UmbrellaOfferRow(
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x22FFFFFF),
        border = BorderStroke(0.6.dp, Color(0x35FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌂", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Зонтики от ",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "29.99 BYN",
                    color = Color(0xFF00E676),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Смотреть ›",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DailyForecastRow(
    item: DailyItem,
    currentTemp: Int?,
    overallMin: Int,
    overallMax: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day name (e.g. "Сегодня", "Пн", "Вт"...)
        Text(
            text = item.dayLabel,
            color = Color.White,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(68.dp)
        )

        // Weather Icon with optional POP % underneath
        Box(
            modifier = Modifier.width(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WeatherIcon(
                    iconType = item.iconType,
                    size = 22.dp
                )
                if (item.pop >= 15) {
                    Text(
                        text = "${item.pop}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Min temperature
        Text(
            text = "${item.minTemp}°",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp)
        )

        // Temperature range gradient bar with current temp dot for "Сегодня"
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
 * 5. Полноразмерный нижний таб-бар с 4 вкладками: Главная, Карта, Список, Поиск (refs/bottom_nav.png, refs/rate_stars.png)
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
        border = BorderStroke(0.8.dp, barBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon capsule
        Box(
            modifier = Modifier
                .size(width = if (isSelected) 44.dp else 28.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) Color(0x551976D2) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
        )
    }
}
