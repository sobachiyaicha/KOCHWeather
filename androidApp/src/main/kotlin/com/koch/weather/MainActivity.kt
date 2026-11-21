package com.koch.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.koch.weather.ui.theme.KOCHWeatherTheme

/**
 * Единственная activity.
 *
 * Приложение НЕ показывается поверх экрана блокировки и НЕ будит экран:
 * `setShowWhenLocked`/`setTurnScreenOn` и флаги `FLAG_SHOW_WHEN_LOCKED`/
 * `FLAG_TURN_SCREEN_ON` удалены намеренно (и в манифесте, и здесь).
 * Если появится соблазн вернуть — не надо.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      KOCHWeatherTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainNavigation()
        }
      }
    }
  }
}
