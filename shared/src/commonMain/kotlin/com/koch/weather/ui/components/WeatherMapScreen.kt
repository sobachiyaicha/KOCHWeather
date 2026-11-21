package com.koch.weather.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import com.koch.weather.platform.PlatformMap

/**
 * R12: полноэкранная карта OSM. Хром — плавающее стекло поверх карты.
 *
 * Phase 2 (platform-engineer): тело карты (osmdroid MapView в AndroidView, маркер,
 * центрирование и кнопка «Центр карты») перенесено в `PlatformMap` — нативный шов
 * `com.koch.weather.platform`. Здесь остаётся только хром экрана: при переносе в
 * commonMain ui-porter копирует этот хром 1:1 и вызывает `PlatformMap`;
 * этот androidMain-файл после переноса удаляется (или становится actual —
 * решает ui-porter, файл описан в отчёте platform-engineer).
 *
 * Навигация — нижний бар, поэтому собственной кнопки «назад» у экрана нет:
 * шапка только показывает город и источник, [modifier] приходит из `MainScreen`
 * с отступом под бар (`bottomContentPadding`), чтобы карта не уходила под него.
 */
@Composable
fun WeatherMapScreen(
    cityName: String,
    latitude: Double,
    longitude: Double,
    currentTemp: Int?,
    conditionRu: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
    ) {
        // Нативная карта + маркер + кнопка «Центр карты» — platform-шов.
        PlatformMap(
            cityName = cityName,
            latitude = latitude,
            longitude = longitude,
            currentTemp = currentTemp,
            conditionRu = conditionRu,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0x73000000), Color(0x00000000)))
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = KochSpace.l, vertical = KochSpace.s),
            shape = KochShape.pill,
            color = colors.sheetSurface,
            border = BorderStroke(KochBorderWidth.hairline, colors.borderStrong),
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KochSpace.l, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cityName,
                        style = KochType.bodyStrong,
                        color = colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = buildString {
                            append("Карта · OpenStreetMap")
                            append(" · ")
                            append(currentTemp?.let { "$it°" } ?: "Нет данных")
                        },
                        style = KochType.caption,
                        color = colors.textTertiary,
                        maxLines = 1
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = KochSpace.l, vertical = KochSpace.l),
            shape = RoundedCornerShape(KochRadius.card),
            color = colors.sheetSurface,
            border = BorderStroke(KochBorderWidth.hairline, colors.borderStrong),
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KochSpace.l, vertical = KochSpace.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cityName,
                        style = KochType.bodyStrong,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Маркер — выбранный город",
                        style = KochType.caption,
                        color = colors.textTertiary,
                        maxLines = 1
                    )
                    Text(
                        text = "© OpenStreetMap contributors",
                        style = KochType.caption,
                        color = colors.textQuaternary,
                        maxLines = 1
                    )
                }
                if (currentTemp != null) {
                    Spacer(Modifier.width(KochSpace.s))
                    Text(
                        text = "$currentTemp°",
                        style = KochType.displayCity,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
