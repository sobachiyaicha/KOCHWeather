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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.ConditionSegment
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.DefaultUmbrellas
import com.koch.weather.data.model.HourlyItem
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.ui.components.TShirtIcon
import com.koch.weather.ui.components.TemperatureRangeBar
import com.koch.weather.ui.components.WeatherIcon
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochEmptyState
import com.koch.weather.ui.design.KochPanel
import com.koch.weather.ui.design.formatMmRu
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import kotlin.math.roundToInt

/**
 * Карточка «Погодные условия» (refs/clean_main.png, refs/segments.png):
 * 5 сегментов, почасовой ряд **развёрнут всегда**, снизу — «Оцените погоду».
 *
 * Строки «Развернуть/Свернуть» больше нет (item 13): она открывала блок с одной и той же
 * подсказкой и выглядела как кнопка-обманка. Оценка, средняя и звёзды видны сразу.
 *
 * Вкладка «Осадки» (item 10) показывает фактические мм/ч, если источник их отдаёт;
 * только после этого — проценты; если нет ни того, ни другого — честное пустое состояние.
 */
@Composable
internal fun HourlyWeatherCard(
    weather: WeatherDomainData,
    selectedSegment: ConditionSegment,
    userRating: Int?,
    /** Средняя оценка погоды за день (3.5 по умолчанию) — та же, что в тексте и звёздах. */
    averageRating: Float,
    showRatingThankYou: Boolean,
    onSegmentSelected: (ConditionSegment) -> Unit,
    onRatingChanged: (Int) -> Unit
) {
    val colors = LocalKochColors.current
    val rainMode = remember(weather.hourly) { RainMode.of(weather.hourly) }

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
                // Метрика в шапке — по фактическим данным, а не всегда «(%)».
                ConditionSegment.RAIN -> when (rainMode) {
                    RainMode.MILLIMETERS -> "Осадки, мм/ч"
                    RainMode.PERCENT -> "Вероятность осадков (%)"
                    RainMode.NONE -> "Осадки"
                }
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
            ConditionSegment.RAIN -> when (rainMode) {
                RainMode.MILLIMETERS -> PrecipitationChart(weather.hourly)
                RainMode.PERCENT -> HourlyRow(weather.hourly, ConditionSegment.RAIN)
                RainMode.NONE -> KochEmptyState(
                    icon = Icons.Default.WaterDrop,
                    title = "Нет данных об осадках",
                    subtitle = "Источник не отдаёт осадки для этого города"
                )
            }
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
        // Число в тексте и заливка звёзд берутся из ОДНОЙ округлённой величины:
        // было «3.5 ★» и три звезды — текст врал о картинке.
        val averageHalfStars = remember(averageRating) { halfStarsOf(averageRating) }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Оцените погоду",
                style = KochType.titleSection,
                color = colors.textPrimary
            )
            Text(
                text = when {
                    showRatingThankYou -> "Спасибо за отзыв! (${userRating ?: 3} ★)"
                    userRating != null ->
                        "Ваша оценка: $userRating ★ · в среднем сегодня: ${formatRating(averageHalfStars)} ★"
                    else -> "В среднем сегодня: ${formatRating(averageHalfStars)} ★"
                },
                style = KochType.caption,
                // Было 3.56:1 (textTertiary) — подняли до ~5:1.
                color = if (showRatingThankYou) colors.accentPromo else colors.textPrimary.copy(alpha = 0.9f),
                maxLines = 2
            )
        }

        // Блок оценки (item 13): ни «Развернуть», ни скрытого блока. Средняя оценка,
        // «Ваша оценка: N ★» и звёзды — всё видно сразу, кнопки-обманки нет.
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
                // Пока пользователь не оценил — показываем среднее (по умолчанию 3.5),
                // включая половинку; после оценки — ровно его звёзды.
                val halfStars = userRating?.let { it * 2 } ?: averageHalfStars
                for (i in 1..5) {
                    val full = halfStars >= i * 2
                    val half = !full && halfStars == i * 2 - 1
                    Icon(
                        imageVector = when {
                            full -> Icons.Default.Star
                            half -> Icons.AutoMirrored.Filled.StarHalf
                            else -> Icons.Default.StarBorder
                        },
                        contentDescription = "Оценка $i",
                        tint = if (full || half) colors.accentStar else colors.textPrimary.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onRatingChanged(i) }
                    )
                }
            }
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

/**
 * Что реально может показать вкладка «Осадки»: мм/ч (источник отдаёт количество),
 * только вероятность (%), либо ничего — тогда честное пустое состояние.
 */
private enum class RainMode {
    MILLIMETERS,
    PERCENT,
    NONE;

    companion object {
        fun of(hours: List<HourlyItem>): RainMode = when {
            hours.any { it.precipitationMm != null } -> MILLIMETERS
            hours.any { it.popKnown } -> PERCENT
            else -> NONE
        }
    }
}

/**
 * Столбики осадков, мм/ч (item 10 фазы 5). Высота столбика = mm / шкала, слева — ось
 * «мм» с подписями 0 и максимум, под столбиками — подписи часов. Сумма за период
 * выводится как «Σ 2,5 мм».
 *
 * Ось и подписи (правка по FAIL visual-critic, M4 / кадр r14_10):
 *  - подпись часа стоит ровно под своим слотом (`index * slot`), а не на равномерной
 *    сетке: столбики идут с шагом 1 час, поэтому подпись «каждый третий» обязана
 *    отстоять на `3 * slot` и попадать в индекс своего бара, а не соседнего;
 *  - слева — шкала «мм»: подписи 0 и максимум плюс горизонтальные линии сетки на них,
 *    поэтому график объясняет высоту столбика, а не просто «рисует палки»;
 *  - подписи часов не налезают друг на друга: час с осадками важнее сеточного,
 *    спорная сеточная подпись пропускается (крайние подписи прижимаются к краю);
 *  - максимум < 1 мм — Canvas 92 dp: короткие «следы дождя» в полной высоте 104 dp
 *    выглядели пустой рамкой. Значимые осадки (≥ 1 мм) оставляют 104 dp.
 *
 * Если осадков нет во всех часах (`Σ 0 мм`), график не рисуется вовсе: пустой Canvas
 * на 104 dp с одной базовой линией выглядел как сломанный график и занимал ~300 dp.
 * Вместо него — компактная честная строка «Осадков не ожидается · за 24 часа».
 */
@Composable
private fun PrecipitationChart(hours: List<HourlyItem>) {
    val colors = LocalKochColors.current
    val visible = remember(hours) { hours.take(24) }
    val amounts = remember(visible) { visible.map { it.precipitationMm } }
    val maxMm = remember(amounts) { amounts.mapNotNull { it }.maxOrNull() ?: 0.0 }
    val totalMm = remember(amounts) { amounts.mapNotNull { it }.sum() }

    if (maxMm <= 0.0) {
        NoRainExpected()
        return
    }

    // Верх шкалы — реальный максимум, но не меньше 0,1 мм: иначе подпись максимума
    // округлилась бы до «0,0», а столбик на 0,04 мм прилип бы к оси (max = 0.1 — край).
    val axisMax = maxOf(maxMm, 0.1)
    // След дождя (< 1 мм) — компактная высота; значимые осадки — полная.
    val chartHeight = if (axisMax < 1.0) 92.dp else 104.dp

    val measurer = rememberTextMeasurer()
    val barColorTop = colors.accent
    val barColorBottom = colors.accent.copy(alpha = 0.45f)
    val labelColor = colors.textPrimary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Σ ${formatMmRu(totalMm)} мм",
                style = KochType.numberRow,
                color = colors.textPrimary
            )
            Spacer(Modifier.width(KochSpace.s))
            Text(
                text = "за 24 часа",
                style = KochType.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(Modifier.height(KochSpace.s))

        // Раньше здесь был `Canvas`: его лямбда — это draw-фаза, поэтому на КАЖДОМ кадре
        // скролла заново шли `measurer.measure` для 3 подписей оси + ~10 подписей часов
        // (кеш TextMeasurer — 8 записей, то есть почти всегда промах), `Brush.verticalGradient`
        // на каждый столбик и аллокация `placed`. Теперь текстовые лэйауты, геометрия
        // и кисти считаются один раз на размер в `drawWithCache`, а в кадре — только
        // draw-вызовы: математика и внешний вид не менялись.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .drawWithCache {
                    val axisStyle = KochType.tab.copy(color = labelColor.copy(alpha = 0.72f))
                    val tickStyle = KochType.tab.copy(color = labelColor.copy(alpha = 0.82f))
                    val maxLabel = measurer.measure(text = formatMmRu(axisMax), style = axisStyle)
                    val zeroLabel = measurer.measure(text = "0", style = axisStyle)
                    val unitLabel = measurer.measure(text = "мм", style = axisStyle)

                    val gap = 4.dp.toPx()
                    val unitGap = 8.dp.toPx()
                    // Жёлоб оси: «мм» и подпись максимума стоят на верхней линии шкалы левее
                    // столбиков, «0» — на нулевой. Ширина = самая широкая подпись + отступы,
                    // поэтому подписи оси не налезают ни на столбики, ни друг на друга.
                    val plotLeft = unitLabel.size.width + unitGap + maxLabel.size.width + gap
                    val plotWidth = (size.width - plotLeft).coerceAtLeast(1f)
                    val topLine = 14.dp.toPx()
                    val baseline = size.height - 18.dp.toPx()  // полоса под подписи часов
                    val lineWidth = 1.dp.toPx()

                    // Один–два часа не растягиваем на всю ширину: иначе единственный столбик
                    // занимает половину карточки, а подпись уезжает от него. Мало часов — группа
                    // центрируется с максимальным шагом 56 dp. Шаг подписей — тот же `slot`.
                    val slot = if (visible.size >= 6) {
                        plotWidth / visible.size
                    } else {
                        56.dp.toPx()
                    }
                    val startX = plotLeft + ((plotWidth - slot * visible.size) / 2f).coerceAtLeast(0f)
                    val barWidth = (slot * 0.55f).coerceAtLeast(1.5.dp.toPx())
                    val usableHeight = (baseline - topLine).coerceAtLeast(1f)

                    val bars = ArrayList<RainBar>(amounts.size)
                    amounts.forEachIndexed { index, mm ->
                        if (mm == null || mm <= 0.0) return@forEachIndexed
                        val ratio = (mm / axisMax).toFloat().coerceIn(0f, 1f)
                        // Минимум 2 dp: «след дождя» 0,1 мм не должен исчезать в ноль пикселей.
                        val barHeight = (ratio * usableHeight).coerceAtLeast(2.dp.toPx())
                        val left = startX + index * slot + (slot - barWidth) / 2f
                        bars += RainBar(
                            topLeft = Offset(left, baseline - barHeight),
                            size = Size(barWidth, barHeight),
                            brush = Brush.verticalGradient(
                                colors = listOf(barColorTop, barColorBottom),
                                startY = baseline - barHeight,
                                endY = baseline
                            )
                        )
                    }

                    // Подписи часов — ровно под своим слотом: «22» всегда под баром 22:00,
                    // а не на один слот правее (дефект кадра r14_10).
                    val ticks = ArrayList<RainTick>(visible.size)
                    if (visible.isNotEmpty()) {
                        val placed = ArrayList<Pair<Float, Float>>(visible.size)
                        val minGap = 2.dp.toPx()

                        fun placeTick(index: Int) {
                            val layout = measurer.measure(text = visible[index].hourLabel, style = tickStyle)
                            val centered = startX + index * slot + (slot - layout.size.width) / 2f
                            // Крайние подписи прижимаем к кромке, чтобы не рисовать за её пределами.
                            val maxX = (size.width - layout.size.width).coerceAtLeast(0f)
                            val x = centered.coerceIn(0f, maxX)
                            val right = x + layout.size.width
                            if (placed.any { right > it.first - minGap && x < it.second + minGap }) return
                            placed += x to right
                            ticks += RainTick(layout, Offset(x, baseline + 4.dp.toPx()))
                        }

                        // Сначала часы с осадками (у них приоритет), затем сетка каждый третий час.
                        visible.indices.forEach { index ->
                            if ((amounts.getOrNull(index) ?: 0.0) > 0.0) placeTick(index)
                        }
                        for (index in visible.indices step 3) {
                            if ((amounts.getOrNull(index) ?: 0.0) > 0.0) continue
                            placeTick(index)
                        }
                    }

                    val topGridColor = labelColor.copy(alpha = 0.18f)
                    val baselineGridColor = labelColor.copy(alpha = 0.12f)

                    onDrawBehind {
                        drawLine(
                            color = topGridColor,
                            start = Offset(plotLeft, topLine),
                            end = Offset(size.width, topLine),
                            strokeWidth = lineWidth
                        )
                        drawText(
                            textLayoutResult = unitLabel,
                            topLeft = Offset(0f, topLine - unitLabel.size.height / 2f)
                        )
                        drawText(
                            textLayoutResult = maxLabel,
                            topLeft = Offset(
                                x = plotLeft - gap - maxLabel.size.width,
                                y = topLine - maxLabel.size.height / 2f
                            )
                        )
                        drawLine(
                            color = baselineGridColor,
                            start = Offset(plotLeft, baseline),
                            end = Offset(size.width, baseline),
                            strokeWidth = lineWidth
                        )
                        drawText(
                            textLayoutResult = zeroLabel,
                            topLeft = Offset(
                                x = plotLeft - gap - zeroLabel.size.width,
                                y = baseline - zeroLabel.size.height / 2f
                            )
                        )

                        bars.forEach { bar ->
                            drawRoundRect(
                                brush = bar.brush,
                                topLeft = bar.topLeft,
                                size = bar.size,
                                cornerRadius = CornerRadius(bar.size.width / 2f, bar.size.width / 2f)
                            )
                        }
                        ticks.forEach { tick ->
                            drawText(
                                textLayoutResult = tick.layout,
                                topLeft = tick.topLeft
                            )
                        }
                    }
                }
        )
    }
}

/** Готовый столбик осадков: позиция, размер и кисть посчитаны вне draw-фазы. */
private class RainBar(
    val topLeft: Offset,
    val size: Size,
    val brush: Brush
)

/** Готовая подпись часа: текстовый лэйаут измерен один раз. */
private class RainTick(
    val layout: TextLayoutResult,
    val topLeft: Offset
)

/** Компактное «дождя нет»: 56–64 dp вместо пустого графика на 104 dp + ряд часов. */
@Composable
private fun NoRainExpected() {
    val colors = LocalKochColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(KochRadius.panel))
            .background(colors.textPrimary.copy(alpha = 0.06f))
            .padding(horizontal = KochSpace.m, vertical = KochSpace.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(KochSpace.m))
        Column {
            Text(
                text = "Осадков не ожидается",
                style = KochType.bodyStrong,
                color = colors.textPrimary
            )
            Text(
                text = "за 24 часа",
                style = KochType.caption,
                color = colors.textSecondary
            )
        }
    }
}

/**
 * Оценка в «полузвёздах» 0..10: 3.5 → 7 (3 полных + половинка). Звёзды и число
 * рядом выводятся из одного значения, поэтому «3.5 ★» и три звезды больше не
 * расходятся (дефект с кадра r14).
 */
private fun halfStarsOf(rating: Float): Int =
    (rating.coerceIn(0f, 5f) * 2f).roundToInt()

/** Подпись оценки: половинка — с одним знаком («3.5»), целое — без хвоста («4»). */
private fun formatRating(halfStars: Int): String =
    if (halfStars % 2 == 0) {
        (halfStars / 2).toString()
    } else {
        com.koch.weather.data.api.formatFixed(halfStars / 2f.toDouble(), 1)
    }

@Composable
private fun HourlyRow(items: List<HourlyItem>, segment: ConditionSegment) {
    // Палитра читается один раз на карточку, а не внутри каждой ячейки ряда.
    val colors = LocalKochColors.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KochSpace.m)
    ) {
        // `contentType` = сегмент: у «Погод. усл.», «Осадков» и «Ветра» одинаковая
        // структура ячейки, поэтому подслоты переиспользуются при смене сегмента.
        items(items, key = { it.hourLabel }, contentType = { segment }) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Минимальная ширина: числа не должны сжиматься до обрезки («1» вместо «15»).
                modifier = Modifier.widthIn(min = 36.dp)
            ) {
                Text(
                    text = item.hourLabel,
                    style = KochType.caption,
                    color = colors.textPrimary.copy(alpha = 0.8f),
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
                    color = colors.textPrimary,
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
    // Строки готовим один раз: без пересчёта строк/индексов на каждом кадре скролла.
    val rows = remember(dailyList) { dailyList.take(10) }

    KochCard(
        shape = KochShape.cardLg,
        padding = PaddingValues(horizontal = KochSpace.m, vertical = KochSpace.s)
    ) {
        rows.forEachIndexed { index, item ->
            DailyForecastRow(
                item = item,
                overallMin = overallMin,
                overallMax = overallMax,
                currentTemp = currentTemp,
                // Разделитель рисуется в самой строке (на один composable меньше на кадр).
                showDivider = index != umbrellaIndex && index < rows.lastIndex,
                onClick = { onDayClick(item) }
            )
            if (index == umbrellaIndex) {
                Spacer(Modifier.height(KochSpace.xs))
                UmbrellaRow(onClick = onOpenUmbrellaShop)
                Spacer(Modifier.height(KochSpace.xs))
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
            text = DefaultUmbrellas.minPriceLabel,
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
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Разделитель — линией в drawWithCache: не плодим composable-элемент на каждую
            // строку и не пересчитываем dp/цвет на каждом кадре (10 строк в карточке).
            .drawWithCache {
                val dividerColor = colors.textPrimary.copy(alpha = 0.08f)
                val dividerHeight = 0.5.dp.toPx()
                onDrawBehind {
                    if (showDivider) {
                        drawRect(
                            color = dividerColor,
                            topLeft = Offset(0f, size.height - dividerHeight),
                            size = Size(size.width, dividerHeight)
                        )
                    }
                }
            }
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
