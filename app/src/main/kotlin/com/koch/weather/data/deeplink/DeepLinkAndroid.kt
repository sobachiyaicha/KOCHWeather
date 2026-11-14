package com.koch.weather.data.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Готовый Android Uri для ui-coder: Intent(ACTION_VIEW, target.toAndroidUri()). */
fun DeepLinkTarget.toAndroidUri(): Uri = Uri.parse(uri)

fun DeepLinkTarget.toViewIntent(): Intent {
    val intent = Intent(Intent.ACTION_VIEW, toAndroidUri())
    if (this is DeepLinkTarget.App) {
        intent.setPackage(packageName)
    }
    return intent
}

/**
 * Возвращает первый Intent, который реально может быть обработан на устройстве:
 * приложение, если установлено, иначе веб-фолбэк. null — не нашлось ни одного.
 */
fun DeepLinkPlan.resolveLaunchIntent(context: Context): Intent? =
    candidates()
        .map { it.toViewIntent() }
        .firstOrNull { intent ->
            context.packageManager.resolveActivity(intent, 0) != null
        }
