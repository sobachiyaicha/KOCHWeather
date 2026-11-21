package com.koch.weather

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.koch.weather.ui.theme.KOCHWeatherTheme
import platform.UIKit.UIViewController

actual fun platformSummary(): String = IosPlatform.summary()

/**
 * Хост Compose-UI для Swift (port plan §9):
 *
 * ```swift
 * ComposeView().ignoresSafeArea(.all)
 * ```
 *
 * Состав 1:1 с androidApp MainActivity: KOCHWeatherTheme + Surface(фон темы) +
 * MainNavigation() → MainScreen. Safe area: `.ignoresSafeArea(.all)` в Swift-хосте,
 * внутри экрана — `statusBarsPadding()` (MainScreen/диалоги).
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    KOCHWeatherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainNavigation()
        }
    }
}
