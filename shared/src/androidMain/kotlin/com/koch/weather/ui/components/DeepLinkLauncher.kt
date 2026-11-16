package com.koch.weather.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.koch.weather.data.deeplink.DeepLinkPlan
import com.koch.weather.data.deeplink.DeepLinks
import com.koch.weather.data.deeplink.resolveLaunchIntent
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.TaxiDestination

/**
 * R16: запуск внешнего приложения/веб-фолбэка.
 *
 * Проверяем resolveActivity (уже внутри [resolveLaunchIntent]), затем стартуем ACTION_VIEW
 * с FLAG_ACTIVITY_NEW_TASK. Возвращаем false, если ни приложение, ни браузер не обработали
 * ссылку — вызывающий UI обязан показать понятное сообщение, а не «заказ создан».
 */
fun DeepLinkPlan.openFirstAvailable(context: Context): Boolean {
    val intent = resolveLaunchIntent(context) ?: return false
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}

/**
 * Полный маршрут Яндекс Go: координаты подачи берутся из выбранного города,
 * координаты назначения — из [TaxiDestination.coordinateNear] вокруг этого же
 * города. UI получает готовый app-Uri и веб-фолбэк.
 */
fun taxiDeepLinkPlan(city: CityLocation, destination: TaxiDestination): DeepLinkPlan =
    DeepLinks.yandexGoRouteFor(city, destination)
