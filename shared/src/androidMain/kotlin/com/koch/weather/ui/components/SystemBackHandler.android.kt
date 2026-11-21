package com.koch.weather.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Android-actual: системный «назад» обрабатывается как в исходнике. */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
