package com.koch.weather.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koch.weather.R

/**
 * Inter (SIL OFL 1.1, лицензия — `assets/fonts/Inter-OFL.txt`), 4 статических начертания.
 * Полное покрытие кириллицы (проверено по cmap: А, Ё, №, °, ★, →, ›, −, —).
 */
val KochFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_bold, FontWeight.ExtraBold)
)

/**
 * Дизайн-токены KOCHWeather (этап 1 редизайна, `_history/reports/phase-4/ui-design-plan.md`).
 *
 * Правило: экраны и компоненты не задают собственные кегли/радиусы/отступы/цвета.
 * Всё берётся отсюда или из [KochColors] (см. Theme.kt).
 *
 * Шкала кеглей сведена к 8 размерам: 10 / 12 / 14 / 16 / 18 / 22 / 28 / 60.
 * Веса: 400 / 500 / 600 / 700 (+ 800 только у температуры-героя, как в референсе).
 */
object KochType {

    /**
     * Семейство шрифта: Inter (OFL). Подключено статическими файлами `res/font/inter_*`,
     * поэтому одинаково работает на minSdk 24 без вариативных настроек.
     */
    private val Family: FontFamily = KochFontFamily

    /** 60 sp — температура-герой в шапке (`rotate`/`offset` не трогаем). */
    val displayTemp = TextStyle(
        fontFamily = Family,
        fontSize = 60.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-2.2).sp
    )

    /** 22 sp — город на капсуле и заголовок листа. */
    val displayCity = TextStyle(
        fontFamily = Family,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )

    /** 28 sp — заголовок полноэкранной страницы. */
    val titlePage = TextStyle(
        fontFamily = Family,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.7).sp
    )

    /** 22 sp — заголовок листа («Поздравляем!», «Пора на улицу!»). */
    val titleSheet = TextStyle(
        fontFamily = Family,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )

    /** 18 sp — заголовок карточки/листа («Такси «Сухой салон»»). */
    val titleCard = TextStyle(
        fontFamily = Family,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    )

    /** 16 sp — заголовок секции внутри листа. */
    val titleSection = TextStyle(
        fontFamily = Family,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp
    )

    /** 14 sp / 400 — тело. */
    val body = TextStyle(
        fontFamily = Family,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    /** 14 sp / 600 — название в списке/строке. */
    val bodyStrong = TextStyle(
        fontFamily = Family,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    )

    /** 12 sp / 400 — подпись, описание (55–70 %). */
    val sub = TextStyle(
        fontFamily = Family,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp
    )

    /** 12 sp / 500 — мета («3.2 км», «~25 мин»). */
    val caption = TextStyle(
        fontFamily = Family,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp
    )

    /** 10 sp / 600 — капс-лейблы («ТЕКУЩЕЕ МЕСТО», «РЕКЛАМА»). */
    val microCaps = TextStyle(
        fontFamily = Family,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.1.sp
    )

    /** 18 sp / 600 — числа в строках (температура города, температура дня). */
    val numberRow = TextStyle(
        fontFamily = Family,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    )

    /** 18 sp / 700 — цены. */
    val numberPrice = TextStyle(
        fontFamily = Family,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    )

    /** 16 sp / 600 — подпись CTA. */
    val button = TextStyle(
        fontFamily = Family,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp
    )

    /** 12 sp / 600 — чипы. */
    val chip = TextStyle(
        fontFamily = Family,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    )

    /** 10 sp / 500 — подписи таб-бара. */
    val tab = TextStyle(
        fontFamily = Family,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )
}

/** Отступы: 4 / 8 / 12 / 16 / 20 / 24 / 32. */
object KochSpace {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val x2l = 24.dp
    val x3l = 32.dp
}

/** Радиусы: 8 / 16 / 20 / 24 / 28 / 32 + пилюля. */
object KochRadius {
    val badge = 8.dp
    val input = 16.dp
    val panel = 20.dp
    val card = 24.dp
    val cardLg = 28.dp
    val sheet = 32.dp
}

/** Готовые формы. */
object KochShape {
    val badge = RoundedCornerShape(KochRadius.badge)
    val input = RoundedCornerShape(KochRadius.input)
    val panel = RoundedCornerShape(KochRadius.panel)
    val card = RoundedCornerShape(KochRadius.card)
    val cardLg = RoundedCornerShape(KochRadius.cardLg)
    val pill = RoundedCornerShape(percent = 50)
    val sheetTop = RoundedCornerShape(
        topStart = KochRadius.sheet,
        topEnd = KochRadius.sheet
    )
    val sheetInset = RoundedCornerShape(KochRadius.sheet)
}

/** Высоты и размеры элементов. */
object KochSize {
    val cta = 52.dp
    val buttonSecondary = 44.dp
    val input = 48.dp
    val row = 64.dp
    val rowTall = 72.dp
    val rowFood = 96.dp
    val serviceCard = 104.dp
    val chip = 32.dp
    val chipTall = 44.dp
    val nav = 64.dp
    val iconButton = 44.dp
    val divider = 0.8.dp
    val dividerSoft = 0.5.dp
    val thumb = 72.dp
    val hero = 112.dp
    val grabberWidth = 36.dp
    val grabberHeight = 4.dp
}

/** Толщины рамок. */
object KochBorderWidth {
    val hairline = 0.8.dp
    val focus = 1.5.dp
    val dashed = 1.5.dp
}

/** Затемнение под листом (45 %). */
val KochScrim: Color
    get() = Color(0x73000000)

/** Нижний скрим фото-героя: прозрачный → 60 % чёрного (refs/premium_ads.png, 1:57). */
val KochHeroScrim: Brush
    get() = Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))
