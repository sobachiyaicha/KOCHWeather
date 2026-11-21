package com.koch.weather

import androidx.compose.runtime.Composable
import com.koch.weather.ui.main.MainScreen

/**
 * Корень навигации. Фаза 2 (KMP): переехал из androidApp в commonMain —
 * один и тот же состав и для MainActivity, и для iOS MainViewController.
 * Вкладки и диалоги — состояние MainScreenViewModel (BottomTab в uiState),
 * отдельного навигационного стека нет (1:1 с исходником).
 */
@Composable
fun MainNavigation() {
    MainScreen()
}
