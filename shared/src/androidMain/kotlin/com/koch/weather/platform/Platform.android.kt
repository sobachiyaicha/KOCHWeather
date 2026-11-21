package com.koch.weather.platform

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.koch.weather.BuildConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Clock
import okio.Path
import okio.Path.Companion.toPath
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Холдер android-контекста для expect-функций (DataStore, вибрация, сеть).
 *
 * androidApp при старте вызывает [install] (Application.onCreate) — это основной путь.
 * Если install не вызван, используется фолбэк `ActivityThread.currentApplication()`
 * (hidden-API light greylist, работает с Application.onCreate и позже).
 */
object AndroidPlatform {
    @Volatile
    private var installed: Context? = null

    @Volatile
    private var callbacksRegistered: Boolean = false

    /** Активность в resumed-состоянии — для setStatusBarDarkIcons (см. MainScreen: window). */
    @Volatile
    internal var currentActivity: Activity? = null

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) { currentActivity = activity }
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity === activity) currentActivity = null
        }
        override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
    }

    /** Вызвать из Application.onCreate androidApp: `AndroidPlatform.install(this)`. */
    fun install(context: Context) {
        val app = context.applicationContext
        installed = app
        registerCallbacks(app)
    }

    private fun registerCallbacks(app: Context?) {
        if (callbacksRegistered) return
        val application = app as? Application ?: return
        callbacksRegistered = true
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    private fun reflectApplication(): Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        activityThread.getDeclaredMethod("currentApplication").invoke(null) as? Context
    }.getOrNull()

    val appContext: Context?
        get() = installed ?: reflectApplication()?.also { install(it) }

    internal fun requireAppContext(): Context =
        appContext ?: throw IllegalStateException(
            "AndroidPlatform.appContext is null: call AndroidPlatform.install(context) in Application.onCreate"
        )
}

// ---------------------------------------------------------------------------
// DataStore (cache-porter: PreferenceDataStoreFactory.createWithPath)
// ---------------------------------------------------------------------------

/**
 * Тот же файл, что раньше создавал `preferencesDataStore(name = "weather_cache")`
 * (`filesDir/datastore/weather_cache.preferences_pb`) — пользовательские данные
 * Android переживают обновление без миграции. Родительский каталог создаётся
 * при необходимости (DataStore сам его не делает при createWithPath).
 */
actual fun dataStoreFile(): Path {
    val context = AndroidPlatform.requireAppContext()
    val file = File(context.filesDir, "datastore/weather_cache.preferences_pb")
    file.parentFile?.mkdirs()
    return file.absolutePath.toPath()
}

// ---------------------------------------------------------------------------
// Медиа
// ---------------------------------------------------------------------------

/**
 * Видео лежат в androidMain res/raw (ad_lion.mp4, ad_slippers.mp4, umbrella_face.mp4) —
 * формат URI 1:1 с прежним AdPromoDialog (`android.resource://pkg/raw/<name>`).
 * После переноса в composeResources (resources-porter) media-engineer заменяет
 * реализацию на `Res.getUri("files/$key")` — контракт (ключ, формат возврата) не меняется.
 */
actual fun mediaUrl(key: String): String {
    val context = AndroidPlatform.requireAppContext()
    val rawName = key.substringBeforeLast('.')
    return "android.resource://${context.packageName}/raw/$rawName"
}

// ---------------------------------------------------------------------------
// Сборка и ключи
// ---------------------------------------------------------------------------

/**
 * FLAG_DEBUGGABLE у приложения: true в debug-сборке, false в release — как
 * BuildConfig.DEBUG в прежнем :app, но без per-variant buildConfigField в KMP.
 */
actual fun isDebugBuild(): Boolean {
    val context = AndroidPlatform.appContext ?: return false
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

/** Тот же источник значения, что читал прежний TrafficRepository (local.properties). */
actual fun trafficApiKey(): String = BuildConfig.TRAFFIC_API_KEY

// ---------------------------------------------------------------------------
// Вибрация (LootboxDialog.vibrateOnDrop, 1:1)
// ---------------------------------------------------------------------------

/**
 * API ≥ 31 — `VibratorManager.defaultVibrator`, API ≥ 26 —
 * `VibrationEffect.createWaveform(0,40,60,40, -1)`, ниже — `vibrate(120)`.
 * Разрешение `android.permission.VIBRATE` уже объявлено в манифесте androidApp.
 */
@Suppress("DEPRECATION")
actual fun vibrateDrop() {
    val context = AndroidPlatform.appContext ?: return
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrator == null || !vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
    } else {
        vibrator.vibrate(120)
    }
}

// ---------------------------------------------------------------------------
// Deep links (DeepLinkLauncher.openFirstAvailable, 1:1)
// ---------------------------------------------------------------------------

/**
 * ACTION_VIEW + FLAG_ACTIVITY_NEW_TASK, как `DeepLinkPlan.openFirstAvailable`.
 * Android 11+ требует <queries> в манифесте — они уже объявлены в androidApp.
 * false/onFail — ни приложение, ни браузер не обработали ссылку.
 */
actual fun openUrl(url: String, onFail: (() -> Unit)?): Boolean {
    val context = AndroidPlatform.appContext
    if (context == null) {
        onFail?.invoke()
        return false
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        onFail?.invoke()
        false
    } catch (_: SecurityException) {
        onFail?.invoke()
        false
    } catch (_: Exception) {
        onFail?.invoke()
        false
    }
}

// ---------------------------------------------------------------------------
// Время
// ---------------------------------------------------------------------------

actual fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

// ---------------------------------------------------------------------------
// Декод изображений (ThemeVisuals/KochBackground → BitmapFactory)
// ---------------------------------------------------------------------------

actual fun decodeImage(bytes: ByteArray): ImageBitmap? = runCatching {
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()

// ---------------------------------------------------------------------------
// Форматирование дат (WeatherMapper/Formatters, ru)
// ---------------------------------------------------------------------------

/** «HH:mm» — русская локаль, детерминированная (не зависит от локали телефона). */
actual fun formatTimeHm(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.forLanguageTag("ru")).format(Date(epochMillis))

/**
 * «Сегодня» для локально сегодняшнего дня; иначе короткое имя дня недели
 * (`SimpleDateFormat("EE", ru)`) с заглавной первой буквой — «Вс», «Пн», …
 * Точно как WeatherMapper.buildDailyList: даты дня и «сейчас» сравниваются
 * по формату "yyyy-MM-dd" в таймзоне устройства.
 */
actual fun formatDayLabelRu(epochMillis: Long): String {
    val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = dayKey.format(Date(nowMillis()))
    if (dayKey.format(Date(epochMillis)) == today) return "Сегодня"
    val dayName = SimpleDateFormat("EE", Locale.forLanguageTag("ru"))
    return dayName.format(Date(epochMillis)).replaceFirstChar { it.uppercase() }
}

// ---------------------------------------------------------------------------
// Foreground (ProcessLifecycleOwner, как MainScreenViewModel/AdPromoDialog)
// ---------------------------------------------------------------------------

actual fun appForegroundFlow(): Flow<Boolean> = callbackFlow {
    val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            trySend(true)
        }

        override fun onStop(owner: LifecycleOwner) {
            trySend(false)
        }
    }
    val lifecycle = ProcessLifecycleOwner.get().lifecycle
    // Начальное значение — как в MainScreenViewModel.init: уже стартовавший процесс.
    trySend(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
    lifecycle.addObserver(observer)
    awaitClose { lifecycle.removeObserver(observer) }
}.distinctUntilChanged()

// ---------------------------------------------------------------------------
// Сеть (NetworkMonitor.kt, 1:1)
// ---------------------------------------------------------------------------

/**
 * Порт `data/network/NetworkMonitor.kt`: ConnectivityManager.NetworkCallback
 * с NET_CAPABILITY_INTERNET, начальное значение — текущее состояние,
 * ошибка регистрации → true (как в исходнике).
 */
actual fun networkMonitorFlow(): Flow<Boolean> = callbackFlow {
    val connectivityManager =
        AndroidPlatform.appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("WeatherApp", "NetworkCallback onAvailable")
            trySend(true)
        }

        override fun onLost(network: Network) {
            Log.d("WeatherApp", "NetworkCallback onLost")
            trySend(false)
        }

        override fun onUnavailable() {
            Log.d("WeatherApp", "NetworkCallback onUnavailable")
            trySend(false)
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    val initial = isCurrentlyConnected(connectivityManager)
    Log.d("WeatherApp", "NetworkMonitor initial isConnected: $initial")
    trySend(initial)

    try {
        connectivityManager?.registerNetworkCallback(request, callback)
    } catch (e: Exception) {
        Log.e("WeatherApp", "Error registering network callback", e)
        trySend(true)
    }

    awaitClose {
        try {
            connectivityManager?.unregisterNetworkCallback(callback)
        } catch (_: Exception) {}
    }
}.distinctUntilChanged()

private fun isCurrentlyConnected(connectivityManager: ConnectivityManager?): Boolean {
    val cm = connectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    Log.d("WeatherApp", "isCurrentlyConnected: hasInternet=$hasInternet")
    return hasInternet
}

// ---------------------------------------------------------------------------
// Статус-бар (MainScreen: isAppearanceLightStatusBars)
// ---------------------------------------------------------------------------

/**
 * Тёмные иконки статус-бара = светлый контент на iOS. Активность берётся из
 * [AndroidPlatform.currentActivity] (tracked через ActivityLifecycleCallbacks);
 * без активного окна — no-op (на iOS поведение описано в Platform.ios.kt).
 */
actual fun setStatusBarDarkIcons(dark: Boolean) {
    val activity = AndroidPlatform.currentActivity ?: return
    val window = activity.window ?: return
    WindowCompat.getInsetsController(window, window.decorView)
        .isAppearanceLightStatusBars = dark
}

// ---------------------------------------------------------------------------
// HTTP и User-Agent
// ---------------------------------------------------------------------------

/**
 * OkHttp-движок с таймаутами OpenMeteoClient (connect 6 с / read+write 10 с /
 * call 12 с, retryOnConnectionFailure) — 1:1. На уровне клиента HttpTimeout
 * в AppContainer задаёт те же лимиты; здесь они действуют как предохранитель.
 */
actual fun httpClientEngine(): HttpClientEngine = OkHttp.create {
    // OkHttpConfig.config в ktor 3.6.1 internal — публичный путь preconfigured.
    // Таймауты 1:1 с OpenMeteoClient (connect 6 / read+write 10 / call 12 с).
    preconfigured = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

/**
 * Формат — прежний WEATHER_HTTP_USER_AGENT; версия поднята до versionName 1.1
 * (KOCHWeather/1.0 (android; +https://github.com/koch/weather) → 1.1).
 */
actual fun appUserAgent(): String = "KOCHWeather/1.1 (android; +https://github.com/koch/weather)"
