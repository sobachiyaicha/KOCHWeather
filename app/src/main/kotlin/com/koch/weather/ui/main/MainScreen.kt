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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koch.weather.data.model.ConditionSegment
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.HourlyItem
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.data.model.WeatherIconType
import com.koch.weather.ui.components.CityPickerDialog
import com.koch.weather.ui.components.DailyDetailDialog
import com.koch.weather.ui.components.FpsOverlay
import com.koch.weather.ui.components.TemperatureRangeBar
import com.koch.weather.ui.components.WeatherIcon
import com.koch.weather.ui.theme.getThemePalette
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isCityPickerOpen by remember { mutableStateOf(false) }
    var selectedDayDetail by remember { mutableStateOf<DailyItem?>(null) }

    MainScreenContent(
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onSelectCityIndex = { viewModel.selectCityByIndex(it) },
        onSegmentSelected = { viewModel.setSegment(it) },
        onOpenCityPicker = { isCityPickerOpen = true },
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
    onOpenCityPicker: () -> Unit,
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
        // High performance LazyColumn with stable keys and contentPadding bottom 88dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
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

            // Hero Weather Header
            val weather = uiState.weather
            if (weather != null) {
                item(key = "hero_header") {
                    WeatherHeroHeader(
                        weather = weather,
                        onHeaderClick = onOpenCityPicker
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Card 1: Hourly Weather Conditions (Glassmorphism)
                item(key = "hourly_card") {
                    HourlyWeatherCard(
                        weather = weather,
                        selectedSegment = uiState.selectedSegment,
                        onSegmentSelected = onSegmentSelected,
                        cardBackground = themeColors.cardBackground,
                        cardBorder = themeColors.cardBorder
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Card 2: 10-Day Forecast (Glassmorphism)
                item(key = "daily_card") {
                    TenDayForecastCard(
                        dailyList = weather.daily,
                        currentTemp = weather.currentTemp,
                        onDayClick = onDayClick,
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

        // Floating Bottom Navigation Bar
        FloatingBottomBar(
            citiesCount = uiState.cities.size,
            selectedCityIndex = uiState.selectedCityIndex,
            onSelectIndex = onSelectCityIndex,
            onOpenMap = onOpenCityPicker,
            onOpenList = onOpenCityPicker,
            barBackground = themeColors.bottomBarBackground,
            barBorder = themeColors.cardBorder,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
        )

        // Floating FPS Overlay in top right corner (isolated Choreographer callback)
        FpsOverlay(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 12.dp)
        )
    }
}

@Composable
private fun WeatherHeroHeader(
    weather: WeatherDomainData,
    onHeaderClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onHeaderClick
            )
    ) {
        // "ТЕКУЩЕЕ МЕСТО" uppercase with slight letter spacing
        Text(
            text = "ТЕКУЩЕЕ МЕСТО",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // City name large
        Text(
            text = weather.cityName,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        // Giant current temperature (e.g. "21°") without extra space before degree
        Text(
            text = "${weather.currentTemp}°",
            color = Color.White,
            fontSize = 82.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = (-2).sp
        )

        // Weather condition description (e.g. "Облачно")
        Text(
            text = weather.conditionDescription,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Range exactly formatted: "Макс.: 22°, мин.: 13°"
        Text(
            text = "Макс.: ${weather.maxTemp}°, мин.: ${weather.minTemp}°",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun HourlyWeatherCard(
    weather: WeatherDomainData,
    selectedSegment: ConditionSegment,
    onSegmentSelected: (ConditionSegment) -> Unit,
    cardBackground: Color,
    cardBorder: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBackground,
        border = BorderStroke(0.8.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header Row: Title & Subtitle on left, Segmented Controls on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (selectedSegment) {
                            ConditionSegment.WEATHER -> "Погодные условия"
                            ConditionSegment.RAIN -> "Осадки"
                            ConditionSegment.WIND -> "Ветер"
                            else -> "Погодные условия"
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (selectedSegment) {
                            ConditionSegment.WEATHER -> "Температура (°C)"
                            ConditionSegment.RAIN -> "Вероятность осадков (%)"
                            ConditionSegment.WIND -> "Скорость ветра (м/с)"
                            else -> "Температура (°C)"
                        },
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Segmented control capsule with white active button & dark icon
                SegmentedControlPill(
                    selectedSegment = selectedSegment,
                    onSegmentSelected = onSegmentSelected
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedSegment) {
                ConditionSegment.WEATHER -> {
                    // Hourly weather row (6 columns: Сейчас, 15, 16, 17, 18, 19)
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
                                text = "Текущая вероятность:",
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Hourly rain probability row with white text
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Hourly wind speed row
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

                else -> {
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
            }
        }
    }
}

@Composable
private fun SegmentedControlPill(
    selectedSegment: ConditionSegment,
    onSegmentSelected: (ConditionSegment) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x38000000),
        border = BorderStroke(0.6.dp, Color(0x25FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentButton(
                icon = Icons.Default.Cloud,
                contentDesc = "Погода",
                isSelected = selectedSegment == ConditionSegment.WEATHER,
                onClick = { onSegmentSelected(ConditionSegment.WEATHER) }
            )
            SegmentButton(
                icon = Icons.Default.WaterDrop,
                contentDesc = "Осадки",
                isSelected = selectedSegment == ConditionSegment.RAIN,
                onClick = { onSegmentSelected(ConditionSegment.RAIN) }
            )
            SegmentButton(
                icon = Icons.Default.Air,
                contentDesc = "Ветер",
                isSelected = selectedSegment == ConditionSegment.WIND,
                onClick = { onSegmentSelected(ConditionSegment.WIND) }
            )
        }
    }
}

@Composable
private fun SegmentButton(
    icon: ImageVector,
    contentDesc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (isSelected) Color(0xFF333333) else Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(17.dp)
        )
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
        // Label "Сейчас", "15", "16" strictly 1 line, no wrapping!
        Text(
            text = item.hourLabel,
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        WeatherIcon(
            iconType = if (selectedSegment == ConditionSegment.RAIN) {
                WeatherIconType.RAIN
            } else {
                item.iconType
            },
            size = 26.dp
        )

        Spacer(modifier = Modifier.height(6.dp))

        val valueText = when (selectedSegment) {
            ConditionSegment.WEATHER -> "${item.temp}°"
            ConditionSegment.RAIN -> "${item.pop}%"
            ConditionSegment.WIND -> "${item.wind.roundToInt()} м/с"
            else -> "${item.temp}°"
        }

        // High contrast readable white text
        Text(
            text = valueText,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TenDayForecastCard(
    dailyList: List<DailyItem>,
    currentTemp: Int,
    onDayClick: (DailyItem) -> Unit,
    cardBackground: Color,
    cardBorder: Color
) {
    if (dailyList.isEmpty()) return

    val overallMin = dailyList.minOf { it.minTemp }
    val overallMax = dailyList.maxOf { it.maxTemp }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBackground,
        border = BorderStroke(0.8.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            dailyList.forEachIndexed { index, item ->
                DailyForecastRow(
                    item = item,
                    currentTemp = if (index == 0) currentTemp else null,
                    overallMin = overallMin,
                    overallMax = overallMax,
                    onClick = { onDayClick(item) }
                )

                if (index < dailyList.lastIndex) {
                    HorizontalDivider(
                        color = Color(0x1AFFFFFF),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
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
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day name (e.g. "Сегодня", "Пн", "Вт"...)
        Text(
            text = item.dayLabel,
            color = Color.White,
            fontSize = 15.sp,
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
                    size = 24.dp
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
            fontSize = 15.sp,
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
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
private fun FloatingBottomBar(
    citiesCount: Int,
    selectedCityIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onOpenMap: () -> Unit,
    onOpenList: () -> Unit,
    barBackground: Color,
    barBorder: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Map / City list button
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = barBackground,
            border = BorderStroke(0.8.dp, barBorder)
        ) {
            IconButton(onClick = onOpenMap) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = "Список городов",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Center: Pill with interactive location arrow + clickable page dots
        Surface(
            modifier = Modifier.height(46.dp),
            shape = RoundedCornerShape(23.dp),
            color = barBackground,
            border = BorderStroke(0.8.dp, barBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Location arrow icon (clickable to switch to city 0)
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Текущая геопозиция",
                    tint = Color.White,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(-45f)
                        .clip(CircleShape)
                        .clickable { onSelectIndex(0) }
                )

                // Page indicator dots - fully interactive with pill capsule for active
                for (i in 0 until citiesCount.coerceAtLeast(1)) {
                    val isSelected = i == selectedCityIndex
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isSelected) 14.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.40f)
                            )
                            .clickable { onSelectIndex(i) }
                    )
                }
            }
        }

        // Right: List / Search Button
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = barBackground,
            border = BorderStroke(0.8.dp, barBorder)
        ) {
            IconButton(onClick = onOpenList) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Поиск и список городов",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
