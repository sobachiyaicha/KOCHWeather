package com.koch.weather.data.model

import com.koch.weather.R

/**
 * Android-резолв медиа-ключа рекламного креатива в ресурс R.
 *
 * Каталог [AdCatalog] переехал в commonMain: прежнее поле `mediaRes: Int` заменено
 * строковым [AdCreative.mediaKey] (port plan §4.3 — «Int меняют тип на String-ключ»).
 * Пока ресурсы не переехали в composeResources (Phase 3), только этот файл
 * androidMain знает про R.drawable/R.raw — тот же набор пар «ключ → ресурс»,
 * что был в прежнем AdModels.kt, 1:1.
 */
fun AdCreative.androidMediaRes(): Int = when (type) {
    AdMediaType.PHOTO -> when (mediaKey) {
        "ad_photo1" -> R.drawable.ad_photo1
        "ad_photo2" -> R.drawable.ad_photo2
        "ad_slippers" -> R.drawable.ad_slippers
        "ad_son" -> R.drawable.ad_son
        else -> 0
    }

    AdMediaType.VIDEO -> when (mediaKey) {
        "ad_lion" -> R.raw.ad_lion
        "ad_slippers" -> R.raw.ad_slippers
        else -> 0
    }
}
