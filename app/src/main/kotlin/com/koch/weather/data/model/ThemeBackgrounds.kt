package com.koch.weather.data.model

import androidx.annotation.DrawableRes
import com.koch.weather.R

/**
 * Локальный фон темы: всегда доступен, без сети и Coil.
 *
 * Файлы лежат в `app/src/main/res/drawable-nodpi` (1080x1920 — ровно экран
 * телефона; фото с размытием 4 px и лёгким зерном, без затемнения). Читаемость
 * белого текста даёт скрим в `KochBackground`.
 *
 * У темы по умолчанию («Голубое небо») фон и превью — кадр с собакой:
 * `theme_default.jpg` (1080×1920, без блюра) и `theme_default_preview.jpg`
 * (540×960). Остальные фото-темы используют свои `theme_<name>.jpg`.
 *
 * «Кровавый закат» намеренно без обоев (TZ): [backgroundRes] = null, и
 * `KochSkyBackground` рисует только градиент палитры темы (ветка `imageRes == null`),
 * тот же градиент, что уже используется на других экранах.
 *
 * Раньше фоном служили сетевые превью Wikimedia шириной 500 px, растянутые на
 * 1080 px, — отсюда «фон в очень низком разрешении». Сетевые фото остаются в
 * [ImageCatalog] для рекламы и витрин, но фон темы берётся только из этого
 * маппинга.
 * Атрибуция источников и лицензий — `ATTRIBUTIONS.md` в корне репозитория.
 * Тип `Int?` нужен именно для тем без фона («Кровавый закат»): UI обязан
 * нарисовать градиент темы.
 */
val AppThemeMode.backgroundRes: Int?
    @DrawableRes get() = when (this) {
        AppThemeMode.BLUE_SKY -> R.drawable.theme_default
        AppThemeMode.COPPER_SUNSET -> R.drawable.theme_copper_sunset
        AppThemeMode.MARS -> R.drawable.theme_mars
        AppThemeMode.CYBERPUNK -> R.drawable.theme_cyberpunk
        AppThemeMode.BLOODY_SUNSET -> null
        AppThemeMode.AURORA_BOREALIS -> R.drawable.theme_aurora_borealis
    }

/**
 * Фон ГЛАВНОЙ. «Голубое небо» отдаёт `theme_default.jpg` — тот же кадр без блюра,
 * что и [backgroundRes]; у остальных тем значения совпадают, а у «Кровавого заката»
 * оба null (градиент). Скрим и `ContentScale.Crop` рисует `KochSkyBackground`.
 */
val AppThemeMode.defaultBackgroundRes: Int?
    @DrawableRes get() = backgroundRes

/** Резкое превью темы (540×960) для карточек галереи и мокапов. */
val AppThemeMode.previewRes: Int?
    @DrawableRes get() = when (this) {
        AppThemeMode.BLUE_SKY -> R.drawable.theme_default_preview
        AppThemeMode.COPPER_SUNSET -> R.drawable.theme_copper_sunset_preview
        AppThemeMode.MARS -> R.drawable.theme_mars_preview
        AppThemeMode.CYBERPUNK -> R.drawable.theme_cyberpunk_preview
        AppThemeMode.BLOODY_SUNSET -> R.drawable.theme_bloody_sunset_preview
        AppThemeMode.AURORA_BOREALIS -> R.drawable.theme_aurora_borealis_preview
    }
