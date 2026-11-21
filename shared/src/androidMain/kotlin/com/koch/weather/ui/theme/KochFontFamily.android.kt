package com.koch.weather.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.koch.weather.R

/**
 * Android-actual шрифтового шва (см. commonMain `Tokens.kt`): Inter (SIL OFL 1.1,
 * лицензия — `assets/fonts/Inter-OFL.txt`), 4 статических начертания.
 * Полное покрытие кириллицы (проверено по cmap: А, Ё, №, °, ★, →, ›, −, —).
 * 1:1 с исходником `res/font/inter_*`; переедет в composeResources в фазе 3
 * (resources-porter), тогда этот файл исчезнет.
 */
actual val KochFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_bold, FontWeight.ExtraBold)
)
