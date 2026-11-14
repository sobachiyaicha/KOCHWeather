package com.koch.weather.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.WeatherDomainData
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.topHighlightBrush
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Шапка-капсула + скошенная температура + точки городов
 * (refs/bottom_nav.png, 0:45; refs/rate_stars.png, 1:00).
 *
 * ВАЖНО: геометрия температуры заморожена — `rotate(-15f)` и offset не менять.
 */
@Composable
internal fun GoldenCapsuleHeader(
    weather: WeatherDomainData,
    citiesCount: Int,
    selectedCityIndex: Int,
    onSelectCityIndex: (Int) -> Unit,
    onHeaderClick: () -> Unit
) {
    val colors = LocalKochColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    // В референсе капсула занимает ~74 % ширины экрана, а не всю строку.
                    .fillMaxWidth(0.84f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHeaderClick
                    ),
                shape = RoundedCornerShape(KochRadius.sheet),
                color = colors.capsuleFill,
                border = BorderStroke(
                    KochBorderWidth.hairline,
                    colors.textPrimary.copy(alpha = 0.28f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .background(topHighlightBrush())
                        .padding(horizontal = KochSpace.l, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ТЕКУЩЕЕ МЕСТО",
                            style = KochType.microCaps,
                            color = colors.textPrimary.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = weather.cityName,
                            style = KochType.displayCity,
                            color = colors.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = weather.conditionDescription,
                                style = KochType.sub,
                                color = colors.textPrimary.copy(alpha = 0.95f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Макс.: ${weather.maxTemp}°, мин.: ${weather.minTemp}°",
                                style = KochType.caption,
                                color = colors.textPrimary.copy(alpha = 0.85f),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // ЗАМОРОЖЕНО: rotate(-15°) и offset — требование пользователя.
            Text(
                text = "${weather.currentTemp}°",
                style = KochType.displayTemp,
                color = colors.textPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-12).dp, y = (-24).dp)
                    .rotate(-15f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Текущая геопозиция",
                tint = colors.textPrimary,
                modifier = Modifier
                    .size(13.dp)
                    .rotate(-45f)
                    .clip(CircleShape)
                    .clickable { onSelectCityIndex(0) }
            )

            for (i in 0 until citiesCount.coerceAtLeast(1)) {
                val isSelected = i == selectedCityIndex
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isSelected) 16.dp else 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isSelected) colors.textPrimary
                            else colors.textPrimary.copy(alpha = 0.45f)
                        )
                        .clickable { onSelectCityIndex(i) }
                )
            }
        }
    }
}

/**
 * Честное пустое состояние: «нет данных», причина и кнопка «Повторить».
 * Никаких выдуманных температур (R21).
 */
@Composable
internal fun WeatherUnavailableCard(
    errorMessage: String?,
    isRefreshing: Boolean,
    onRetry: () -> Unit
) {
    val colors = LocalKochColors.current
    KochCard(
        material = if (colors.isDark) KochMaterial.Solid else KochMaterial.Glass,
        shape = RoundedCornerShape(KochRadius.card),
        padding = PaddingValues(horizontal = KochSpace.xl, vertical = KochSpace.x2l)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(KochSpace.m))
            Text(
                text = "Нет данных о погоде",
                style = KochType.titleSection,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(KochSpace.xs))
            Text(
                text = "Не удалось загрузить прогноз. Проверьте подключение к сети и повторите попытку.",
                style = KochType.sub,
                color = colors.textTertiary,
                textAlign = TextAlign.Center
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(KochSpace.s))
                Text(
                    text = errorMessage,
                    style = KochType.caption,
                    color = colors.textQuaternary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(KochSpace.l))
            KochPrimaryButton(
                text = if (isRefreshing) "Обновляем..." else "Повторить",
                onClick = onRetry,
                enabled = !isRefreshing,
                loading = isRefreshing
            )
        }
    }
}
