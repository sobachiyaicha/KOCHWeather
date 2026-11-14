package com.koch.weather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.koch.weather.data.model.AppThemeMode

/**
 * Семантическая палитра темы (см. `_history/reports/phase-4/ui-design-plan.md`, §4.2–4.3).
 *
 * Роли, а не оттенки: экран не знает, что такое `#1E232A`; он знает `surface`/`border`/`accent`.
 * Голубая тема — стекло поверх неба; медная и производные — плотные поверхности.
 *
 * `@Immutable`: объекты кешируются по теме (см. [kochColors]), поэтому одна и та же пара
 * тема→палитра всегда даёт один и тот же экземпляр, и CompositionLocal не инвалидирует дерево.
 */
@Immutable
data class KochColors(
    val isDark: Boolean,
    val bgTop: Color,
    val bgUpper: Color,
    val bgLower: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceGlass: Color,
    val surfaceGlassStrong: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textQuaternary: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentPromo: Color,
    val accentStar: Color,
    val accentSuccess: Color,
    val accentDanger: Color,
    val accentAdTop: Color,
    val accentAdBottom: Color,
    val accentAdLabel: Color,
    val navFill: Color,
    val navContent: Color,
    val navActive: Color,
    val sheetSurface: Color,
    val capsuleFill: Color
) {
    /** Фон приложения: мягкий вертикальный градиент из 4 стопов (измерено по refs/raw). */
    val gradient: Brush
        get() = Brush.verticalGradient(listOf(bgTop, bgUpper, bgLower, bgBottom))
}

/** Акцент действия один для всех тем: синий iOS (`#0B8DF1` в референсе). */
private val ActionBlue = Color(0xFF0A8AF0)
private val ActionBluePressed = Color(0xFF0070D8)
private val PromoAmber = Color(0xFFE9BF60)
private val StarGold = Color(0xFFF5C24C)
private val SuccessMint = Color(0xFF7BC98A)
private val DangerRed = Color(0xFFFF6B6B)
private val AdGreenTop = Color(0xFF4C8B2B)
private val AdGreenBottom = Color(0xFF2F631F)
private val AdGreenLabel = Color(0xFFC9F08A)

private val SkyBlue = KochColors(
    isDark = false,
    bgTop = Color(0xFF8EB3C7),
    bgUpper = Color(0xFF579BBB),
    bgLower = Color(0xFF6AAED0),
    bgBottom = Color(0xFF9ACCEC),
    surface = Color(0x1AFFFFFF),
    surfaceAlt = Color(0x14FFFFFF),
    surfaceGlass = Color(0x1AFFFFFF),
    surfaceGlassStrong = Color(0x29FFFFFF),
    border = Color(0x33FFFFFF),
    borderStrong = Color(0x59FFFFFF),
    textPrimary = Color.White,
    textSecondary = Color(0xC7FFFFFF),
    textTertiary = Color(0x8CFFFFFF),
    textQuaternary = Color(0x61FFFFFF),
    accent = ActionBlue,
    accentPressed = ActionBluePressed,
    accentPromo = PromoAmber,
    accentStar = StarGold,
    accentSuccess = SuccessMint,
    accentDanger = DangerRed,
    accentAdTop = AdGreenTop,
    accentAdBottom = AdGreenBottom,
    accentAdLabel = AdGreenLabel,
    navFill = Color(0xA6FFFFFF),
    navContent = Color(0xFF21445C),
    navActive = Color(0xFF0A6FD0),
    // Плотное синее стекло: белый текст держит контраст ≥ 4.5:1 (blur недоступен по перф-бюджету).
    sheetSurface = Color(0xF2436E86),
    capsuleFill = PromoAmber
)

private val CopperSunset = KochColors(
    isDark = true,
    bgTop = Color(0xFFD5967E),
    bgUpper = Color(0xFFB07258),
    bgLower = Color(0xFFA1624A),
    bgBottom = Color(0xFF8F5240),
    surface = Color(0xFF4B2A20),
    surfaceAlt = Color(0xFF553024),
    surfaceGlass = Color(0x1FFFFFFF),
    surfaceGlassStrong = Color(0x29FFFFFF),
    border = Color(0x1AFFFFFF),
    borderStrong = Color(0xFF6E4433),
    textPrimary = Color.White,
    textSecondary = Color(0xCCFFFFFF),
    textTertiary = Color(0x8CFFFFFF),
    textQuaternary = Color(0x61FFFFFF),
    accent = ActionBlue,
    accentPressed = ActionBluePressed,
    accentPromo = PromoAmber,
    accentStar = StarGold,
    accentSuccess = SuccessMint,
    accentDanger = DangerRed,
    accentAdTop = AdGreenTop,
    accentAdBottom = AdGreenBottom,
    accentAdLabel = AdGreenLabel,
    navFill = Color(0xF2F2E6DC),
    navContent = Color(0xFF4A2A1E),
    navActive = Color(0xFFC2552D),
    sheetSurface = Color(0xF24B2A20),
    capsuleFill = PromoAmber
)

/** Производные темы: та же формула, что и у медной, но свой градиент. */
private fun derived(
    top: Color,
    upper: Color,
    lower: Color,
    bottom: Color
): KochColors {
    val surface = lerp(bottom, Color.Black, 0.55f)
    return KochColors(
        isDark = true,
        bgTop = top,
        bgUpper = upper,
        bgLower = lower,
        bgBottom = bottom,
        surface = surface,
        surfaceAlt = lerp(surface, Color.White, 0.06f),
        surfaceGlass = Color(0x1FFFFFFF),
        surfaceGlassStrong = Color(0x29FFFFFF),
        border = Color(0x1AFFFFFF),
        borderStrong = lerp(surface, Color.White, 0.18f),
        textPrimary = Color.White,
        textSecondary = Color(0xCCFFFFFF),
        textTertiary = Color(0x8CFFFFFF),
        textQuaternary = Color(0x61FFFFFF),
        accent = ActionBlue,
        accentPressed = ActionBluePressed,
        accentPromo = PromoAmber,
        accentStar = StarGold,
        accentSuccess = SuccessMint,
        accentDanger = DangerRed,
        accentAdTop = AdGreenTop,
        accentAdBottom = AdGreenBottom,
        accentAdLabel = AdGreenLabel,
        navFill = lerp(top, Color.White, 0.85f).copy(alpha = 0.92f),
        navContent = lerp(bottom, Color.Black, 0.85f),
        navActive = lerp(bottom, Color.White, 0.15f),
        sheetSurface = surface.copy(alpha = 0.96f),
        capsuleFill = PromoAmber
    )
}

/**
 * Кеш палитр: одна тема — один экземпляр [KochColors].
 * Раньше `derived()` создавал новый объект на каждый вызов, из-за чего `LocalKochColors`
 * (staticCompositionLocalOf) менялся при каждом обновлении uiState и перекомпоновывал всё дерево
 * на 5 темах из 7 (дефект из perf-qa).
 */
private val KochColorsCache = java.util.concurrent.ConcurrentHashMap<AppThemeMode, KochColors>()

fun kochColors(mode: AppThemeMode): KochColors = KochColorsCache.getOrPut(mode) {
    when (mode) {
        AppThemeMode.BLUE_SKY -> SkyBlue
        AppThemeMode.COPPER_SUNSET -> CopperSunset
        AppThemeMode.FOREST -> derived(
            Color(0xFF1B3B2B), Color(0xFF2C5E43), Color(0xFF427A5B), Color(0xFF162D21)
        )
        AppThemeMode.MARS -> derived(
            Color(0xFF4A1A12), Color(0xFF7A2D20), Color(0xFFA84230), Color(0xFF33120C)
        )
        AppThemeMode.CYBERPUNK -> derived(
            Color(0xFF0D0221), Color(0xFF240046), Color(0xFF3C096C), Color(0xFF19053B)
        )
        AppThemeMode.BLOODY_SUNSET -> derived(
            Color(0xFF2B090D), Color(0xFF590D15), Color(0xFF8B121E), Color(0xFF23070A)
        )
        AppThemeMode.AURORA_BOREALIS -> derived(
            Color(0xFF05131E), Color(0xFF0C2B36), Color(0xFF134E4A), Color(0xFF082229)
        )
    }
}

/** Легаси-структура: экраны этапа 2 ещё принимают её из MainScreen. Кешируется по теме. */
@Immutable
data class CustomThemeColors(
    val gradientBrush: Brush,
    val cardBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPill: Color,
    val bottomBarBackground: Color,
    val koch: KochColors
)

private val LegacyPaletteCache = java.util.concurrent.ConcurrentHashMap<AppThemeMode, CustomThemeColors>()

fun getThemePalette(mode: AppThemeMode): CustomThemeColors = LegacyPaletteCache.getOrPut(mode) {
    val k = kochColors(mode)
    CustomThemeColors(
        gradientBrush = k.gradient,
        cardBackground = if (k.isDark) k.surface else k.surfaceGlass,
        cardBorder = k.border,
        textPrimary = k.textPrimary,
        textSecondary = k.textSecondary,
        accentPill = k.accentPromo,
        bottomBarBackground = k.navFill,
        koch = k
    )
}

/** Доступ к токенам палитры из любого composable под [ProvideKochColors]. */
val LocalKochColors = staticCompositionLocalOf { SkyBlue }

@Composable
fun ProvideKochColors(mode: AppThemeMode, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKochColors provides kochColors(mode), content = content)
}

/** Материальная типографика собирается из тех же 8 кеглей, чтобы M3-компоненты не «врали». */
@Composable
fun KOCHWeatherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ActionBlue,
            onPrimary = Color.White,
            background = SkyBlue.bgTop,
            surface = SkyBlue.sheetSurface,
            onSurface = Color.White
        ),
        typography = KochMaterialTypography,
        content = content
    )
}
