package com.koch.weather.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.ClothingAdvisor
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.WmoHelper
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.formatMaxMinRu
import com.koch.weather.ui.design.formatMmRu
import com.koch.weather.ui.design.KochPanel
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import kotlinx.datetime.toLocalDateTime

/**
 * Детали дня — обычный нижний лист в общем языке (раньше был отдельный синий диалог).
 *
 * Осадки (item 10 фазы 5): количество из `DailyItem.precipitationMm` («Осадки: 4,2 мм»),
 * вероятность — только если источник её реально отдаёт (`popKnown`). Раньше на этом месте
 * всегда стояло «Нет данных», даже когда мм были известны.
 *
 * Совет по одежде — из `ClothingAdvisor.adviceFor(temp, code, wind, city, dayOfYear)`:
 * детерминирован от города и дня, поэтому не «дёргается» при рекомпозиции и меняется
 * по дням, а не остаётся одной и той же строкой навсегда.
 */
@Composable
fun DailyDetailDialog(
    item: DailyItem,
    cityName: String,
    onDismiss: () -> Unit,
    windSpeedMps: Double? = null,
    dayOfYear: Int = currentDayOfYear()
) {
    val colors = LocalKochColors.current
    val clothingAdvice = remember(item, cityName, windSpeedMps, dayOfYear) {
        ClothingAdvisor.adviceFor(
            tempC = (item.maxTemp + item.minTemp) / 2,
            weatherCode = item.weatherCode,
            windSpeedMps = windSpeedMps,
            cityName = cityName,
            dayOfYear = dayOfYear
        )
    }

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.74f,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = KochSpace.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.dayLabel,
                        style = KochType.titleSheet,
                        color = colors.textPrimary
                    )
                    Text(
                        text = cityName,
                        style = KochType.sub,
                        color = colors.textTertiary,
                        maxLines = 1
                    )
                }
                KochCloseButton(onClick = onDismiss)
            }

            Spacer(Modifier.height(KochSpace.l))

            Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherIcon(iconType = item.iconType, size = 44.dp)
                Spacer(Modifier.width(KochSpace.l))
                Column {
                    Text(
                        text = WmoHelper.getDescriptionRu(item.weatherCode),
                        style = KochType.titleSection,
                        color = colors.textPrimary
                    )
                    Text(
                        text = formatMaxMinRu(item.maxTemp, item.minTemp),
                        style = KochType.sub,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(KochSpace.l))

            KochPanel {
                // Осадки: миллиметры за сутки, если источник их отдаёт.
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Осадки",
                        style = KochType.body,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.precipitationMm?.let { "${formatMmRu(it)} мм" } ?: "Нет данных",
                        style = KochType.numberRow,
                        color = colors.textPrimary
                    )
                }
                // Вероятность — только когда она известна (MET Norway её не отдаёт).
                if (item.popKnown) {
                    Spacer(Modifier.height(KochSpace.s))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Вероятность",
                            style = KochType.body,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${item.pop} %",
                            style = KochType.numberRow,
                            color = colors.textPrimary
                        )
                    }
                }
                Spacer(Modifier.height(KochSpace.m))
                HorizontalDivider(color = colors.textPrimary.copy(alpha = 0.08f), thickness = 0.5.dp)
                Spacer(Modifier.height(KochSpace.m))
                Text(
                    text = "Совет по одежде",
                    style = KochType.caption,
                    color = colors.textTertiary
                )
                Text(
                    text = clothingAdvice,
                    style = KochType.body,
                    color = colors.textPrimary
                )
            }
        },
        footer = {
            KochTextButton(
                text = "Понятно",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

/**
 * День года для детерминированного совета (фаза 2, KMP): замена
 * `java.util.Calendar.getInstance().get(Calendar.DAY_OF_YEAR)` —
 * kotlinx-datetime по локальной зоне устройства, число то же.
 */
private fun currentDayOfYear(): Int =
    kotlinx.datetime.Instant.fromEpochMilliseconds(
        com.koch.weather.platform.nowMillis()
    ).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.dayOfYear
