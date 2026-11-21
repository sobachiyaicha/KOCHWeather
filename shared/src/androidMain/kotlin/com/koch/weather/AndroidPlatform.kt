package com.koch.weather

import android.os.Build

/**
 * Android-actual для ожидания из commonMain Platform.kt (iOS-шелл, коммит 47be7b2).
 * Заметка: App() из iOS-шелла в Android-приложении не используется
 * (MainActivity рисует MainNavigation), actual добавлен для целостности KMP-модуля.
 */
actual fun platformSummary(): String = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
