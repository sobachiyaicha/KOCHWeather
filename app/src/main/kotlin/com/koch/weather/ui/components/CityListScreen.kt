package com.koch.weather.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koch.weather.data.model.SavedCityWeather

/**
 * R13: полноэкранный «Список» сохранённых городов.
 *
 * Показывает только реальные данные из [SavedCityWeather]; если прогноза нет — «Нет данных»,
 * никаких синтетических температур.
 */
@Composable
fun CityListScreen(
    cities: List<SavedCityWeather>,
    selectedCityName: String?,
    isLoading: Boolean,
    background: Brush,
    attribution: String?,
    onRefresh: () -> Unit,
    onSelectCity: (SavedCityWeather) -> Unit,
    onBack: () -> Unit
) {
    FullScreenScaffold(
        title = "Список городов",
        subtitle = "Сохранённые города и погода",
        background = background,
        onBack = onBack,
        headerActions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить список",
                    tint = Color.White
                )
            }
        }
    ) {
        when {
            isLoading && cities.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            cities.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    FullScreenEmptyState(
                        icon = Icons.Default.LocationCity,
                        title = "Нет сохранённых городов",
                        subtitle = "Добавьте город через поиск — он появится в этом списке."
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        top = 12.dp,
                        end = 14.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cities, key = { it.city.name }) { item ->
                        CityListRow(
                            item = item,
                            isSelected = item.city.name.equals(selectedCityName, ignoreCase = true),
                            onClick = { onSelectCity(item) }
                        )
                    }

                    attribution?.takeIf { it.isNotBlank() }?.let { text ->
                        item(key = "attribution") {
                            DataSourceFooter(text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityListRow(
    item: SavedCityWeather,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) Color(0x332196F3) else Color(0x26FFFFFF),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) Color(0xFF64B5F6) else Color(0x30FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.city.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E88E5)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Выбран",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.city.country,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.5.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.conditionOrNoDataRu,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.5.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (item.tempC != null) {
                    Text(
                        text = "${item.tempC}°",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Нет данных",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (item.maxTempC != null && item.minTempC != null) {
                    Text(
                        text = "Макс.: ${item.maxTempC}°, мин.: ${item.minTempC}°",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.End
                    )
                }

                if (item.hasData && item.isOffline) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "сохранённый прогноз",
                            color = Color(0xFFFFD54F),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
