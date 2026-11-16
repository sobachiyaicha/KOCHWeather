package com.koch.weather

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

actual fun platformSummary(): String = IosPlatform.summary()

/**
 * Хост Compose-UI для Swift:
 *
 * ```swift
 * ComposeView().ignoresSafeArea()
 * ```
 */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
