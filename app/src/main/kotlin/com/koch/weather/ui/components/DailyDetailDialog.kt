package com.koch.weather.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.DailyItem
import com.koch.weather.data.model.WmoHelper
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.KochPanel
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/** Детали дня — обычный нижний лист в общем языке (раньше был отдельный синий диалог). */
@Composable
fun DailyDetailDialog(
    item: DailyItem,
    cityName: String,
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    val clothingAdvice = WmoHelper.getClothingAdvice(
        (item.maxTemp + item.minTemp) / 2,
        item.weatherCode
    )

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.72f,
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
                        text = "Макс.: ${item.maxTemp}°, мин.: ${item.minTemp}°",
                        style = KochType.sub,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(KochSpace.l))

            KochPanel {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Вероятность осадков",
                        style = KochType.body,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (item.popKnown) "${item.pop}%" else "Нет данных",
                        style = KochType.numberRow,
                        color = colors.textPrimary
                    )
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
