package com.koch.weather.ui.design

import com.koch.weather.data.model.AppThemeMode

/**
 * Общий порт androidMain `data/model/ThemeBackgrounds.kt`: R.drawable-идентификаторы
 * заменены строковыми ключами файлов (1:1 с маппингом исходника; «Кровавый закат»
 * по-прежнему без обоев — UI обязан нарисовать градиент темы).
 *
 * Android-декод ключей — шов [decodeLocalDrawable] (res/drawable-nodpi 1:1 с
 * исходником); в фазе 3 ключи лягут на `Res.drawable.*` (resources-porter).
 *
 * Локальный фон темы: всегда доступен, без сети и Coil. У темы по умолчанию
 * («Голубое небо») фон и превью — кадр с собакой: `theme_default` (1080×1920,
 * без блюра) и `theme_default_preview` (540×960). Остальные фото-темы используют
 * свои `theme_<name>`.
 */
val AppThemeMode.backgroundKey: String?
    get() = when (this) {
        AppThemeMode.BLUE_SKY -> "theme_default"
        AppThemeMode.COPPER_SUNSET -> "theme_copper_sunset"
        AppThemeMode.MARS -> "theme_mars"
        AppThemeMode.CYBERPUNK -> "theme_cyberpunk"
        AppThemeMode.BLOODY_SUNSET -> null
        AppThemeMode.AURORA_BOREALIS -> "theme_aurora_borealis"
    }

/** Фон ГЛАВНОЙ: значения совпадают с [backgroundKey], у «Кровавого заката» — null (градиент). */
val AppThemeMode.defaultBackgroundKey: String?
    get() = backgroundKey

/** Резкое превью темы (540×960) для карточек галереи и мокапов. */
val AppThemeMode.previewKey: String?
    get() = when (this) {
        AppThemeMode.BLUE_SKY -> "theme_default_preview"
        AppThemeMode.COPPER_SUNSET -> "theme_copper_sunset_preview"
        AppThemeMode.MARS -> "theme_mars_preview"
        AppThemeMode.CYBERPUNK -> "theme_cyberpunk_preview"
        AppThemeMode.BLOODY_SUNSET -> "theme_bloody_sunset_preview"
        AppThemeMode.AURORA_BOREALIS -> "theme_aurora_borealis_preview"
    }
