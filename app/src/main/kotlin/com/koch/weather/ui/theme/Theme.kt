package com.koch.weather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.koch.weather.data.model.AppThemeMode

data class CustomThemeColors(
    val gradientBrush: Brush,
    val cardBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPill: Color,
    val bottomBarBackground: Color
)

fun getThemePalette(mode: AppThemeMode): CustomThemeColors {
    return when (mode) {
        AppThemeMode.BLUE_SKY -> CustomThemeColors(
            gradientBrush = Brush.verticalGradient(
                listOf(SkyBlueTop, SkyBlueMiddle, SkyBlueBottom)
            ),
            cardBackground = Color(0x38FFFFFF),
            cardBorder = Color(0x44FFFFFF),
            textPrimary = Color.White,
            textSecondary = Color(0xD8FFFFFF),
            accentPill = Color(0xFFF9C449),
            bottomBarBackground = Color(0x66FFFFFF)
        )
        AppThemeMode.COPPER_SUNSET -> CustomThemeColors(
            gradientBrush = Brush.verticalGradient(
                listOf(CopperTop, CopperMiddle, CopperBottom)
            ),
            cardBackground = Color(0xFF38231C),
            cardBorder = Color(0xFF5A392F),
            textPrimary = Color.White,
            textSecondary = Color(0xD0E5D5),
            accentPill = Color(0xFFF9C449),
            bottomBarBackground = Color(0x992B1914)
        )
        AppThemeMode.FOREST -> CustomThemeColors(
            gradientBrush = Brush.verticalGradient(
                listOf(ForestTop, ForestMiddle, ForestBottom)
            ),
            cardBackground = ForestCard,
            cardBorder = Color(0xFF264C38),
            textPrimary = Color.White,
            textSecondary = Color(0xD0E0D8),
            accentPill = Color(0xFF81C784),
            bottomBarBackground = Color(0x9910241A)
        )
        AppThemeMode.MARS -> CustomThemeColors(
            gradientBrush = Brush.verticalGradient(
                listOf(MarsTop, MarsMiddle, MarsBottom)
            ),
            cardBackground = MarsCard,
            cardBorder = Color(0xFF552118),
            textPrimary = Color.White,
            textSecondary = Color(0xDCE0D8),
            accentPill = Color(0xFFFF8A65),
            bottomBarBackground = Color(0x992B0D08)
        )
    }
}

@Composable
fun KOCHWeatherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            background = SkyBlueTop,
            surface = SkyCardBackground
        ),
        content = content
    )
}
