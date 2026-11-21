package com.koch.weather.platform

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import okio.Path

/**
 * Платформенные швы KOCHWeather (владелец — platform-engineer, phase 2).
 *
 * Контракт для data-porter / cache-porter / ui-porter / media-engineer:
 *  - [dataStoreFile]    → полный путь к файлу DataStore Preferences
 *                        (`PreferenceDataStoreFactory.createWithPath { dataStoreFile() }`);
 *  - [mediaUrl]         → проигрываемый URI медиа по ключу ресурса, напр. "ad_lion.mp4";
 *  - [isDebugBuild]     → true только в debug-сборке (FPS-оверлей и debug-диагностика);
 *  - [trafficApiKey]    → ключ пробок из local.properties; пустая строка — «Нет данных»;
 *  - [vibrateDrop]      → один тактильный выстрел на дроп лутбокса;
 *  - [openUrl]          → внешняя ссылка (deep link), false/onFail — ничего не открылось;
 *  - [nowMillis]        → текущее время epochMillis (замена System.currentTimeMillis());
 *  - [decodeImage]      → декод байтов (JPEG/PNG) в ImageBitmap, null при ошибке;
 *  - [formatTimeHm]     → «HH:mm» в русской локали (замена SimpleDateFormat("HH:mm"));
 *  - [formatDayLabelRu] → «Сегодня» либо короткий день недели («Вс», «Пн», …) —
 *                         замена логики WeatherMapper.buildDailyList;
 *  - [appForegroundFlow]→ true на ON_START, false на ON_STOP процесса;
 *  - [networkMonitorFlow] → true = сеть есть (замена NetworkMonitor.isOnline);
 *  - [setStatusBarDarkIcons] → тёмные иконки статус-бара (iOS: светлый контент);
 *  - [httpClientEngine] → Ktor-движок платформы (OkHttp на Android, Darwin на iOS).
 */
expect fun dataStoreFile(): Path

/** Проигрываемый URI медиа по ключу compose-ресурса, напр. "ad_lion.mp4" или "ad_lion.jpg". */
expect fun mediaUrl(key: String): String

expect fun isDebugBuild(): Boolean

/** TRAFFIC_API_KEY из local.properties; пустая строка → пробки «Нет данных» (port plan §4.4). */
expect fun trafficApiKey(): String

/**
 * Один выстрел тактильного отклика на дроп лутбокса.
 * Android: waveform 0/40/60/40 мс (≥API 26) или 120 мс; iOS: UIImpactFeedbackGenerator.
 * Вызывать можно многократно, безопасность по потокам — платформенная.
 */
expect fun vibrateDrop()

/**
 * Открыть URL (deep link приложения или https-страницу).
 * Возвращает true, если запуск принят платформой; при неудаче вызывает [onFail].
 * Фолбэк app→https делает вызывающий (DeepLinkPlan.candidates), вызывая функцию повторно.
 */
expect fun openUrl(url: String, onFail: (() -> Unit)?): Boolean

/** Текущее время epochMillis — замена System.currentTimeMillis(). */
expect fun nowMillis(): Long

/** Декод байтов изображения в ImageBitmap; null — декод не удался (остаётся градиент темы). */
expect fun decodeImage(bytes: ByteArray): ImageBitmap?

/** «HH:mm» в русской локали (замена SimpleDateFormat("HH:mm", ru)). */
expect fun formatTimeHm(epochMillis: Long): String

/**
 * Подпись дня: «Сегодня», если epochMillis — локально сегодняшний день, иначе короткое
 * имя дня недели с заглавной буквы («Вс», «Пн», «Вт», «Ср», «Чт», «Пт», «Сб»).
 */
expect fun formatDayLabelRu(epochMillis: Long): String

/** true — приложение в foreground (ON_START/ON_START уже прошёл); начальное значение — текущее состояние. */
expect fun appForegroundFlow(): Flow<Boolean>

/** true = сеть доступна; начальное значение — текущее состояние (замена NetworkMonitor.isOnline). */
expect fun networkMonitorFlow(): Flow<Boolean>

/** Тёмные иконки статус-бара под белой облачной вуалью (MainScreen veilVisible). */
expect fun setStatusBarDarkIcons(dark: Boolean)

/** Ktor-движок платформы: androidMain — OkHttp, iosMain — Darwin. */
expect fun httpClientEngine(): HttpClientEngine

/**
 * Единый User-Agent для HTTP-провайдеров (MET Norway/OSM отвечают 403 на дефолтные UA).
 * Формат 1:1 с WEATHER_HTTP_USER_AGENT, версия — versionName 1.1.
 */
expect fun appUserAgent(): String
