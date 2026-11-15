package com.koch.weather

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.koch.weather.ui.components.AdVideoEngine
import com.koch.weather.ui.theme.KOCHWeatherTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      )
    }

    enableEdgeToEdge()
    setContent {
      KOCHWeatherTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  override fun onDestroy() {
    // Выход из приложения — единственный момент, когда рекламный плеер и его
    // постоянная выходная поверхность действительно освобождаются. Во время
    // показа рекламы (и между показами) release() не вызывается вовсе, поэтому
    // многосекундный teardown кодека на realme/OPPO не попадает в горячий путь.
    // Внутри shutdown() сначала clearVideoSurface(), только потом release().
    // Смена конфигурации сюда не относится: isFinishing == false, плеер не трогаем.
    if (isFinishing) {
      AdVideoEngine.shutdown()
    }
    super.onDestroy()
  }
}