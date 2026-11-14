package com.koch.weather.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.ConditionSegment
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.HourlyItem
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.ui.components.TShirtIcon
import com.koch.weather.ui.components.TemperatureRangeBar
import com.koch.weather.ui.components.WeatherIcon
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochPanel
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Карточка «Погодные условия» (refs/clean_main.png, refs/segments.png):
 * 5 сегментов, почасовой ряд **развёрнут всегда**, снизу — «Оцените погоду» с «Развернуть».
 */
@Composable
internal fun HourlyWeatherCard(
    weather: WeatherDomainData,
    selectedSegment: ConditionSegment,
    userRating: Int?,
    showRatingThankYou: Boolean,
    onSegmentSelected: (ConditionSegment) -> Unit,
    onRatingChanged: (Int) -> Unit
) {
    val colors = LocalKochColors.current
    var ratingExpanded by remember { mutableStateOf(false) }

    KochCard(
        shape = KochShape.cardLg,
        padding = PaddingValues(horizontal = KochSpace.l, vertical = KochSpace.l)
    ) {
        Text(
            text = "Погодные условия",
            style = KochType.titleSection,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(KochSpace.s))

        // Пилюля-переключатель: 5 сегментов, активный — светлая заливка, не белая плашка.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(KochShape.pill)
                .background(colors.textPrimary.copy(alpha = 0.08f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentTabItem("Погод. усл.", Icons.Default.Cloud, selectedSegment == ConditionSegment.WEATHER, { onSegmentSelected(ConditionSegment.WEATHER) }, Modifier.weight(1f))
            SegmentTabItem("Осадки", Icons.Default.WaterDrop, selectedSegment == ConditionSegment.RAIN, { onSegmentSelected(ConditionSegment.RAIN) }, Modifier.weight(1f))
            SegmentTabItem("Ветер", Icons.Default.Air, selectedSegment == ConditionSegment.WIND, { onSegmentSelected(ConditionSegment.WIND) }, Modifier.weight(1f))
            SegmentTabItem("Одежда", TShirtIcon, selectedSegment == ConditionSegment.CLOTHES, { onSegmentSelected(ConditionSegment.CLOTHES) }, Modifier.weight(1f))
            SegmentTabItem("Пробки", Icons.Default.DirectionsCar, selectedSegment == ConditionSegment.TRAFFIC, { onSegmentSelected(ConditionSegment.TRAFFIC) }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(KochSpace.m))
        Text(
            text = when (selectedSegment) {
                ConditionSegment.WEATHER -> "Температура (°C)"
                ConditionSegment.RAIN -> "Вероятность осадков (%)"
                ConditionSegment.WIND -> "Скорость ветра (м/с)"
                ConditionSegment.CLOTHES -> "Рекомендации по одежде"
                ConditionSegment.TRAFFIC -> "Дорожная ситуация"
            },
            style = KochType.caption,
            // 88 % белого + затемнённое фото-небо под стеклом: контраст ≥4.5:1.
            // 45 % (как было) на светлом облаке не читались.
            color = colors.textPrimary.copy(alpha = 0.88f)
        )
        Spacer(Modifier.height(KochSpace.s))

        when (selectedSegment) {
            ConditionSegment.WEATHER -> HourlyRow(weather.hourly, ConditionSegment.WEATHER)
            ConditionSegment.RAIN -> HourlyRow(weather.hourly, ConditionSegment.RAIN)
            ConditionSegment.WIND -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = KochSpace.s),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = weather.windSpeed?.let { "$it м/с" } ?: "Нет данных",
                        style = KochType.numberRow,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.width(KochSpace.s))
                    val windSpeed = weather.windSpeed
                    Text(
                        text = when {
                            windSpeed == null -> "Нет данных о ветре"
                            windSpeed < 5 -> "Слабый ветерок"
                            windSpeed < 10 -> "Умеренный ветер"
                            windSpeed < 15 -> "Сильный порывистый"
                            else -> "Штормовое предупреждение"
                        },
                        style = KochType.caption,
                        color = colors.textSecondary
                    )
                }
                HourlyRow(weather.hourly, ConditionSegment.WIND)
            }
            ConditionSegment.CLOTHES -> KochPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = TShirtIcon,
                        contentDescription = null,
                        tint = colors.textPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(KochSpace.m))
                    Column {
                        Text("Совет на сегодня", style = KochType.bodyStrong, color = colors.textPrimary)
                        Text(
                            weather.clothingAdvice,
                            style = KochType.sub,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            ConditionSegment.TRAFFIC -> KochPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Traffic,
                        contentDescription = null,
                        tint = colors.accentPromo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(KochSpace.m))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Дорожная обстановка", style = KochType.bodyStrong, color = colors.textPrimary)
                            val trafficScore = weather.trafficScore
                            if (trafficScore != null) {
                                Spacer(Modifier.width(KochSpace.s))
                                Text(
                                    text = "$trafficScore/10",
                                    style = KochType.caption,
                                    color = colors.accentPromo
                                )
                            }
                        }
                        Text(
                            weather.trafficAdvice,
                            style = KochType.sub,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(KochSpace.m))
        HorizontalDivider(color = colors.textPrimary.copy(alpha = 0.08f), thickness = 0.8.dp)
        Spacer(Modifier.height(KochSpace.m))

        // Блок оценки: средняя оценка читается всегда (R3), звёзды — под ней.
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Оцените погоду",
                style = KochType.titleSection,
                color = colors.textPrimary
            )
            Text(
                text = when {
                    showRatingThankYou -> "Спасибо за отзыв! (${userRating ?: 3} ★)"
                    userRating != null -> "Ваша оценка: $userRating ★ · в среднем сегодня: 3.5 ★"
                    else -> "В среднем сегодня: 3.5 ★"
                },
                style = KochType.caption,
                color = if (showRatingThankYou) colors.accentPromo else colors.textTertiary,
                maxLines = 2
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KochSpace.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(KochShape.pill)
                    .background(colors.textPrimary.copy(alpha = 0.10f))
                    .padding(horizontal = KochSpace.s, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeRating = userRating ?: 3
                for (i in 1..5) {
                    val isFilled = i <= activeRating
                    Icon(
                        imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Оценка $i",
                        tint = if (isFilled) colors.accentStar else colors.textPrimary.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onRatingChanged(i) }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(KochShape.pill)
                    .clickable { ratingExpanded = !ratingExpanded }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (ratingExpanded) "Свернуть" else "Развернуть",
                    style = KochType.caption,
                    color = colors.textTertiary
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = if (ratingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (ratingExpanded) {
            Text(
                text = if (userRating != null) {
                    "Ваша оценка сохранена: $userRating ★"
                } else {
                    "Нажмите на звезду, чтобы оценить погоду"
                },
                style = KochType.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SegmentTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(KochShape.pill)
            .background(if (isSelected) colors.textPrimary.copy(alpha = 0.20f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) colors.accentPromo else colors.textPrimary.copy(alpha = 0.88f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = title,
                style = KochType.tab,
                color = if (isSelected) colors.textPrimary else colors.textPrimary.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun HourlyRow(items: List<HourlyItem>, segment: ConditionSegment) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KochSpace.m)
    ) {
        items(items, key = { it.hourLabel }) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Минимальная ширина: числа не должны сжиматься до обрезки («1» вместо «15»).
                modifier = Modifier.widthIn(min = 36.dp)
            ) {
                Text(
                    text = item.hourLabel,
                    style = KochType.caption,
                    color = LocalKochColors.current.textPrimary.copy(alpha = 0.8f),
                    maxLines = 1
                )
                Spacer(Modifier.height(KochSpace.xs))
                WeatherIcon(iconType = item.iconType, size = 22.dp)
                Spacer(Modifier.height(KochSpace.xs))
                Text(
                    text = when (segment) {
                        ConditionSegment.RAIN -> if (item.popKnown) "${item.pop}%" else "—"
                        ConditionSegment.WIND -> item.wind?.let { "${it.toInt()}" } ?: "—"
                        else -> "${item.temp}°"
                    },
                    style = KochType.bodyStrong,
                    color = LocalKochColors.current.textPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Карточка 10 дней (refs/rate_stars.png): строки без заголовка, зонтики — сразу после «Пн».
 */
@Composable
internal fun TenDayForecastCard(
    dailyList: List<DailyItem>,
    currentTemp: Int,
    onDayClick: (DailyItem) -> Unit,
    onOpenUmbrellaShop: () -> Unit
) {
    val colors = LocalKochColors.current
    val overallMin = remember(dailyList) { dailyList.minOfOrNull { it.minTemp } ?: 10 }
    val overallMax = remember(dailyList) { dailyList.maxOfOrNull { it.maxTemp } ?: 25 }
    // Зонтики — сразу после «Пн» (refs/rate_stars.png). Ищем по подписи дня, а не по индексу:
    // список может начинаться и с «Сегодня», и с «Пн».
    val umbrellaIndex = remember(dailyList) {
        dailyList.indexOfFirst { it.dayLabel.startsWith("Пн") }.takeIf { it >= 0 } ?: 1
    }

    KochCard(
        shape = KochShape.cardLg,
        padding = PaddingValues(horizontal = KochSpace.m, vertical = KochSpace.s)
    ) {
        dailyList.take(10).forEachIndexed { index, item ->
            DailyForecastRow(
                item = item,
                overallMin = overallMin,
                overallMax = overallMax,
                currentTemp = currentTemp,
                onClick = { onDayClick(item) }
            )
            if (index == umbrellaIndex) {
                Spacer(Modifier.height(KochSpace.xs))
                UmbrellaRow(onClick = onOpenUmbrellaShop)
                Spacer(Modifier.height(KochSpace.xs))
            } else if (index < dailyList.lastIndex) {
                HorizontalDivider(
                    color = colors.textPrimary.copy(alpha = 0.08f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun UmbrellaRow(onClick: () -> Unit) {
    val colors = LocalKochColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KochRadius.input))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = KochSpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.BeachAccess,
            contentDescription = null,
            tint = colors.accentPromo,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(KochSpace.s))
        Text(
            text = "Зонтики от",
            style = KochType.caption,
            color = colors.textPrimary.copy(alpha = 0.85f)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "29.99 BYN",
            style = KochType.bodyStrong,
            color = colors.accentPromo
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Смотреть ›",
            style = KochType.caption,
            color = colors.textPrimary.copy(alpha = 0.85f),
            maxLines = 1
        )
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
    val colors = LocalKochColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KochRadius.input))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = KochSpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.dayLabel,
            style = KochType.bodyStrong,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.width(64.dp)
        )
        Box(
            modifier = Modifier.width(34.dp),
            contentAlignment = Alignment.Center
        ) {
            WeatherIcon(iconType = item.iconType, size = 22.dp)
        }
        Text(
            text = "${item.minTemp}°",
            style = KochType.caption,
            color = colors.textPrimary.copy(alpha = 0.6f),
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(30.dp)
        )
        TemperatureRangeBar(
            minTemp = item.minTemp,
            maxTemp = item.maxTemp,
            overallMin = overallMin,
            overallMax = overallMax,
            currentTemp = if (item.dayLabel == "Сегодня") currentTemp else null,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = KochSpace.s),
            barHeight = 4.dp
        )
        Text(
            text = "${item.maxTemp}°",
            style = KochType.bodyStrong,
            color = colors.textPrimary,
            textAlign = TextAlign.Start,
            maxLines = 1,
            modifier = Modifier.width(32.dp)
        )
    }
}
