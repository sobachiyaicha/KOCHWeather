package com.koch.weather.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Нативная карта с маркером выбранного города — шов для WeatherMapScreen
 * (владелец — platform-engineer; карта и её управление — платформенная зона,
 * стекло/карточки экрана остаются в ui.components).
 *
 * Сигнатура — по фактическому вызову в androidMain WeatherMapScreen
 * (port lead предлагал temperatureText/conditionText: String, но вызов передаёт
 * `currentTemp: Int?` и `conditionRu: String?`: маркер обязан честно показать
 * «Нет данных», когда температуры нет — форматирование подписи живёт в actual,
 * 1:1 с прежним markerSnippet: «2° · Облачно» / «Нет данных» / «2°»).
 *
 * Контракт:
 *  - маркер в точке (latitude, longitude), title = cityName;
 *  - карта центрируется на координатах при их смене (и при первом показе);
 *  - Android: osmdroid MAPNIK, зум 12.0, пин-иконка с акцентным цветом темы,
 *    кнопка «Центр карты» (MyLocation) — как в исходнике;
 *  - iOS: MKMapView, стандартный зум (span ~0.04°), подпись
 *    «© OpenStreetMap contributors» в углу; кнопка недавнего центра — Phase 5
 *    (map-engineer), там же визуальный тюнинг.
 */
@Composable
expect fun PlatformMap(
    cityName: String,
    latitude: Double,
    longitude: Double,
    currentTemp: Int?,
    conditionRu: String? = null,
    modifier: Modifier = Modifier
)
