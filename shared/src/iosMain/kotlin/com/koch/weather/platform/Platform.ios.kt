@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package com.koch.weather.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Clock
import okio.Path
import okio.Path.Companion.toPath
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.darwin.dispatch_queue_create

// ---------------------------------------------------------------------------
// DataStore (cache-porter: PreferenceDataStoreFactory.createWithPath)
// ---------------------------------------------------------------------------

/**
 * Путь из port plan §3: NSApplicationSupportDirectory/koch.preferences_pb.
 * Application Support создаётся при необходимости (iOS его не гарантирует).
 */
actual fun dataStoreFile(): Path {
    val directories = NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory, NSUserDomainMask, true
    )
    val appSupport = directories.firstOrNull() as? String
        ?: throw IllegalStateException("NSApplicationSupportDirectory is not resolvable")
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = appSupport,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )
    return "$appSupport/koch.preferences_pb".toPath()
}

// ---------------------------------------------------------------------------
// Медиа (media-engineer, phase 4)
// ---------------------------------------------------------------------------

/**
 * `Payload.app/media/<key>` — AVPlayer играет file:// напрямую.
 *
 * Единственная копия роликов в репо — mp4-файлы в `shared/src/androidMain/res/raw`
 * (второй копии не держим); `scripts/ios-build.sh` копирует их в бандл приложения
 * в каталог `media/` на шаге сборки (и для --device, и для --simulator).
 * Ключ приходит с расширением: `ad_lion.mp4`, `ad_slippers.mp4`,
 * `umbrella_face.mp4` — расширение не отрезается.
 *
 * Переход на Compose Resources (`Res.getUri("files/$key")`) — Phase 3:
 * composeResources в iOS-бандл пока не подключены, поэтому путь ведёт в `media/`.
 * Контракт (ключ → играбельный URI) при этом не меняется.
 */
actual fun mediaUrl(key: String): String {
    val bundlePath = NSBundle.mainBundle.bundlePath
    return "file://$bundlePath/media/$key"
}

// ---------------------------------------------------------------------------
// Сборка и ключи
// ---------------------------------------------------------------------------

/**
 * Debug-бинарник Kotlin/Native: linkDebugFramework → true, linkRelease → false.
 * Этого достаточно для FPS-оверлея и debug-диагностики (port plan §4).
 */
actual fun isDebugBuild(): Boolean = kotlin.native.Platform.isDebugBinary

/** Port plan §4.4: ключ пробок на iOS пустой → пробки честно показывают «Нет данных». */
actual fun trafficApiKey(): String = ""

// ---------------------------------------------------------------------------
// Haptics (LootboxDialog vibrateOnDrop)
// ---------------------------------------------------------------------------

/**
 * Один тактильный выстрел на дроп лутбокса: средний impact — ближайший аналог
 * Android waveform 0/40/60/40 мс. Генератор создаётся на вызов (ленивый синглтон
 * держать не обязательно: impactOccurred валиден и для свежего генератора).
 */
actual fun vibrateDrop() {
    UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
        .impactOccurred()
}

// ---------------------------------------------------------------------------
// Deep links (DeepLinkLauncher.openFirstAvailable)
// ---------------------------------------------------------------------------

/**
 * UIApplication.open с предварительным canOpenURL: false → onFail → вызывающий
 * повторяет вызов с https-URL (DeepLinkPlan.web) — фолбэк app→https 1:1.
 * canOpenURL требует схем в LSApplicationQueriesSchemes (iosApp Info.plist:
 * yandextaxi; для карт/еды ios-shell-engineer добавит yandexmaps и eda.yandex).
 * Проверка и запуск идут с main-потока (вызов из Compose-событий).
 */
actual fun openUrl(url: String, onFail: (() -> Unit)?): Boolean {
    val target = NSURL.URLWithString(url)
    if (target == null) {
        onFail?.invoke()
        return false
    }
    val application = UIApplication.sharedApplication
    if (!application.canOpenURL(target)) {
        onFail?.invoke()
        return false
    }
    application.openURL(
        url = target,
        options = mapOf<Any?, Any>(),
        completionHandler = { success ->
            if (!success) onFail?.invoke()
        }
    )
    return true
}

// ---------------------------------------------------------------------------
// Время
// ---------------------------------------------------------------------------

actual fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

// ---------------------------------------------------------------------------
// Декод изображений (KochBackground/ThemeVisuals → Skia)
// ---------------------------------------------------------------------------

actual fun decodeImage(bytes: ByteArray): ImageBitmap? = runCatching {
    // CMP 1.12.2: createImageBitmap/toComposeImageBitmap — internal, поэтому
    // публичный путь: skia Image → Bitmap.makeFromImage → asComposeImageBitmap.
    Bitmap.makeFromImage(Image.makeFromEncoded(bytes)).asComposeImageBitmap()
}.getOrNull()

// ---------------------------------------------------------------------------
// Форматирование дат (WeatherMapper.buildDailyList, ru_RU)
// ---------------------------------------------------------------------------

/** Форматтеры дороги — держим ленивые синглтоны (аналог `remember` в MainScreen). */
private val timeFormatterRu by lazy {
    NSDateFormatter().apply {
        dateFormat = "HH:mm"
        locale = NSLocale(localeIdentifier = "ru_RU")
    }
}

/** Ключ дня «yyyy-MM-dd» — сравнение «сегодня» без таймзонных сюрпризов. */
private val dayKeyFormatter by lazy {
    NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd"
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
    }
}

/** Короткий день недели в ru_RU: «вс», «пн», … (1:1 с SimpleDateFormat("EE", ru)). */
private val dayNameFormatterRu by lazy {
    NSDateFormatter().apply {
        dateFormat = "EE"
        locale = NSLocale(localeIdentifier = "ru_RU")
    }
}

private fun nsDate(epochMillis: Long): NSDate =
    // initWithTimeIntervalSince1970 не отмаплен Kotlin/Native — считаем от
    // reference date (2001-01-01 00:00:00 UTC = 978307200 с Unix).
    NSDate(timeIntervalSinceReferenceDate = epochMillis / 1000.0 - 978_307_200.0)

actual fun formatTimeHm(epochMillis: Long): String =
    timeFormatterRu.stringFromDate(nsDate(epochMillis))

/**
 * «Сегодня» для локально сегодняшнего дня, иначе короткое имя дня недели
 * с заглавной первой буквы («Вс», «Пн», …) — как WeatherMapper.buildDailyList.
 */
actual fun formatDayLabelRu(epochMillis: Long): String {
    val date = nsDate(epochMillis)
    if (dayKeyFormatter.stringFromDate(date) == dayKeyFormatter.stringFromDate(nsDate(nowMillis()))) {
        return "Сегодня"
    }
    return dayNameFormatterRu.stringFromDate(date).replaceFirstChar { it.uppercase() }
}

// ---------------------------------------------------------------------------
// Foreground (NSNotificationCenter ↔ ProcessLifecycleOwner)
// ---------------------------------------------------------------------------

/**
 * true на UIApplicationDidBecomeActiveNotification, false на
 * UIApplicationWillResignActiveNotification (аналог ON_START/ON_STOP).
 * Первое значение приходит из актуального applicationState — как в
 * MainScreenViewModel.init (уже стартовавший процесс).
 */
actual fun appForegroundFlow(): Flow<Boolean> = callbackFlow {
    val center = NSNotificationCenter.defaultCenter
    val mainQueue = NSOperationQueue.mainQueue
    val foregroundObserver = center.addObserverForName(
        UIApplicationDidBecomeActiveNotification, null, mainQueue
    ) { _ ->
        trySend(true)
    }
    val backgroundObserver = center.addObserverForName(
        UIApplicationWillResignActiveNotification, null, mainQueue
    ) { _ ->
        trySend(false)
    }
    trySend(
        UIApplication.sharedApplication.applicationState ==
            UIApplicationState.UIApplicationStateActive
    )
    awaitClose {
        center.removeObserver(foregroundObserver)
        center.removeObserver(backgroundObserver)
    }
}.distinctUntilChanged()

// ---------------------------------------------------------------------------
// Сеть (NWPathMonitor ↔ ConnectivityManager.NetworkMonitor)
// ---------------------------------------------------------------------------

/**
 * Порт data/network/NetworkMonitor.kt на NWPathMonitor (C API Network.framework):
 * satisfied — сеть есть (аналог NET_CAPABILITY_INTERNET). Первое значение
 * приходит из pathUpdateHandler сразу после старта монитора; C API не даёт
 * синхронного снапшота, поэтому отдельного «начального» trySend нет.
 */
actual fun networkMonitorFlow(): Flow<Boolean> = callbackFlow {
    val monitor = nw_path_monitor_create()
    val queue = dispatch_queue_create("koch.weather.network.monitor", null)
    nw_path_monitor_set_queue(monitor, queue)
    nw_path_monitor_set_update_handler(monitor) { path ->
        val satisfied = path != null &&
            nw_path_get_status(path) == nw_path_status_satisfied
        trySend(satisfied)
    }
    nw_path_monitor_start(monitor)
    awaitClose {
        nw_path_monitor_cancel(monitor)
    }
}.distinctUntilChanged()

// ---------------------------------------------------------------------------
// Статус-бар (MainScreen veilVisible)
// ---------------------------------------------------------------------------

/**
 * Минимально компилируемый no-op (port plan §5): iOS-стиль статус-бара
 * задаётся глобально у хост-VC. Если понадобится динамическая смена —
 * ios-shell-engineer отдаёт preferredStatusBarStyle через конфиг
 * ComposeUIViewController; UI-логика (передавать veilVisible) уже в шве.
 */
actual fun setStatusBarDarkIcons(dark: Boolean) {
    // no-op на iOS: контент статус-бара управляется Info.plist/хост-VC.
}

// ---------------------------------------------------------------------------
// HTTP и User-Agent
// ---------------------------------------------------------------------------

/** Darwin-движок (NSURLSession); таймауты задаёт HttpTimeout в AppContainer. */
actual fun httpClientEngine(): HttpClientEngine = Darwin.create()

/** Формат 1:1 с Android; платформенная метка — ios. */
actual fun appUserAgent(): String = "KOCHWeather/1.1 (ios; +https://github.com/koch/weather)"
