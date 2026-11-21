package com.koch.weather.ui.components

import com.koch.weather.data.deeplink.DeepLinkPlan
import com.koch.weather.data.deeplink.DeepLinks
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.TaxiDestination
import com.koch.weather.platform.openUrl

/**
 * R16: запуск внешнего приложения/веб-фолбэка.
 *
 * Фаза 2 (KMP): ACTION_VIEW/resolveActivity заменены платформенным швом
 * [openUrl] (Android-actual ловит ActivityNotFoundException/SecurityException,
 * как прежний startActivity; iOS — canOpenURL + openURL). Обходим [candidates]
 * по порядку: сначала приложение, затем веб-фолбэк — та же стратегия, что у
 * resolveLaunchIntent. Возвращаем false, если ни приложение, ни браузер не
 * обработали ссылку — вызывающий UI обязан показать понятное сообщение,
 * а не «заказ создан».
 */
fun DeepLinkPlan.openFirstAvailable(): Boolean {
    for (target in candidates()) {
        var failed = false
        val opened = openUrl(target.uri) { failed = true }
        if (opened && !failed) return true
    }
    return false
}

/**
 * Полный маршрут Яндекс Go: координаты подачи берутся из выбранного города,
 * координаты назначения — из [TaxiDestination.coordinateNear] вокруг этого же
 * города. UI получает готовый app-Uri и веб-фолбэк.
 */
fun taxiDeepLinkPlan(city: CityLocation, destination: TaxiDestination): DeepLinkPlan =
    DeepLinks.yandexGoRouteFor(city, destination)
