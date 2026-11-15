package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.SavedCityWeather
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.formatMaxMinRu
import com.koch.weather.ui.design.KochEmptyState
import com.koch.weather.ui.design.KochIconButton
import com.koch.weather.ui.design.KochListRow
import com.koch.weather.ui.design.KochPageHeader
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * R13: полноэкранный «Список» сохранённых городов (без референса — язык видео).
 * Одна группа строк с разделителями вместо четырёх карточек; температура тонкая крупная.
 *
 * Навигация — нижний бар, поэтому собственной стрелки «назад» у экрана нет.
 * Добавление города — «+» в шапке и строка в конце списка.
 */
@Composable
fun CityListScreen(
    cities: List<SavedCityWeather>,
    selectedCityName: String?,
    isLoading: Boolean,
    background: Brush,
    attribution: String?,
    attributionNote: String? = null,
    onRefresh: () -> Unit,
    onSelectCity: (SavedCityWeather) -> Unit,
    onAddCity: () -> Unit
) {
    val colors = LocalKochColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            KochPageHeader(
                title = "Список городов",
                subtitle = "Сохранённые города и погода",
                onBack = null,
                actions = {
                    KochIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Добавить город",
                        onClick = onAddCity,
                        tint = colors.accent,
                        containerAlpha = 0f
                    )
                    KochIconButton(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Обновить список",
                        onClick = onRefresh,
                        tint = colors.textPrimary.copy(alpha = 0.9f),
                        containerAlpha = 0f
                    )
                }
            )
            Spacer(Modifier.height(KochSpace.s))

            when {
                isLoading && cities.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.textPrimary)
                    }
                }

                cities.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        KochEmptyState(
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
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = KochSpace.xl,
                            end = KochSpace.xl,
                            bottom = KochSpace.x2l
                        )
                    ) {
                        item(key = "cities_group") {
                            KochCard(padding = PaddingValues(vertical = KochSpace.xs)) {
                                cities.forEachIndexed { index, item ->
                                    KochListRow(
                                        title = item.city.name,
                                        subtitle = buildString {
                                            val line = item.city.locationLine()
                                            append(line)
                                            if (!item.conditionOrNoDataRu.isNullOrBlank()) {
                                                append(" · ")
                                                append(item.conditionOrNoDataRu)
                                            }
                                        },
                                        selected = item.city.name.equals(
                                            selectedCityName,
                                            ignoreCase = true
                                        ),
                                        showDivider = index != cities.lastIndex,
                                        minHeight = 72.dp,
                                        // M2a: условие («Сильный дождь») — часть смысла строки,
                                        // третья строка важнее обрезки. У остальных строк в
                                        // приложении подзаголовок остаётся двухстрочным.
                                        subtitleMaxLines = 3,
                                        onClick = { onSelectCity(item) },
                                        trailing = {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = item.tempC?.let { "$it°" } ?: "Нет данных",
                                                    style = KochType.numberPrice,
                                                    color = if (item.tempC != null) {
                                                        colors.textPrimary
                                                    } else {
                                                        colors.textTertiary
                                                    },
                                                    maxLines = 1
                                                )
                                                if (item.maxTempC != null && item.minTempC != null) {
                                                    Text(
                                                        text = formatMaxMinRu(item.maxTempC, item.minTempC),
                                                        style = KochType.caption,
                                                        color = colors.textTertiary,
                                                        textAlign = TextAlign.End,
                                                        maxLines = 1
                                                    )
                                                }
                                                if (item.hasData && item.isOffline) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.CloudOff,
                                                            contentDescription = null,
                                                            tint = colors.accentPromo,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                        Text(
                                                            text = "сохранённый прогноз",
                                                            style = KochType.caption,
                                                            color = colors.accentPromo,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        item(key = "add_city") {
                            Spacer(Modifier.height(KochSpace.m))
                            KochCard(padding = PaddingValues(vertical = KochSpace.xs)) {
                                KochListRow(
                                    title = "Добавить город",
                                    subtitle = "Поиск по названию",
                                    onClick = onAddCity,
                                    leading = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }

                        attribution?.takeIf { it.isNotBlank() }?.let { text ->
                            item(key = "attribution") {
                                DataSourceFooter(text, note = attributionNote)
                            }
                        }
                    }
                }
            }
        }
    }
}
