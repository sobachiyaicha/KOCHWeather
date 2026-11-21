package com.koch.weather.ui.components

import androidx.compose.runtime.Composable

/**
 * iOS-actual: системного back-жеста нет — no-op. Port plan §13: не полагаться
 * на BackHandler; закрытие диалогов — только явные крестик/CTA (все есть).
 */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op: onBack недостижим системным жестом на iOS.
}
