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
 * Раньше фоном служили сетевые превью Wikimedia шириной 500 px, растянутые на
 * 1080 px, — отсюда «фон в очень низком разрешении». Сетевые фото остаются в
 * [ImageCatalog] для рекламы и витрин, но фон темы берётся только из этого
 * маппинга.
 * Атрибуция источников и лицензий — `ATTRIBUTIONS.md` в корне репозитория.
 * Для всех 7 тем ресурс ненулевой; тип `Int?` оставлен на случай тем без фона,
 * которые могут появиться позже (тогда UI обязан рисовать градиент темы).
 */
val AppThemeMode.backgroundRes: Int?
    @DrawableRes get() = when (this) {
        AppThemeMode.BLUE_SKY -> R.drawable.theme_blue_sky
        AppThemeMode.COPPER_SUNSET -> R.drawable.theme_copper_sunset
        AppThemeMode.FOREST -> R.drawable.theme_forest
        AppThemeMode.MARS -> R.drawable.theme_mars
        AppThemeMode.CYBERPUNK -> R.drawable.theme_cyberpunk
        AppThemeMode.BLOODY_SUNSET -> R.drawable.theme_bloody_sunset
        AppThemeMode.AURORA_BOREALIS -> R.drawable.theme_aurora_borealis
    }

/** Резкое превью темы (540×960) для карточек и мокапов; фон главной размыт отдельно. */
val AppThemeMode.previewRes: Int?
    @DrawableRes get() = when (this) {
        AppThemeMode.BLUE_SKY -> R.drawable.theme_blue_sky_preview
        AppThemeMode.COPPER_SUNSET -> R.drawable.theme_copper_sunset_preview
        AppThemeMode.FOREST -> R.drawable.theme_forest_preview
        AppThemeMode.MARS -> R.drawable.theme_mars_preview
        AppThemeMode.CYBERPUNK -> R.drawable.theme_cyberpunk_preview
        AppThemeMode.BLOODY_SUNSET -> R.drawable.theme_bloody_sunset_preview
        AppThemeMode.AURORA_BOREALIS -> R.drawable.theme_aurora_borealis_preview
    }
