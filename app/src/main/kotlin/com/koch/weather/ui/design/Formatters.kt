package com.koch.weather.ui.design

import java.util.Locale

/** "34 500" — разряды через пробел, без копеек. Для цен в российских рублях. */
fun formatRuAmount(value: Int): String =
    String.format(Locale.US, "%,d", value).replace(',', ' ')

/**
 * Единый формат диапазона температур для шапки, списка городов и деталей дня:
 * ровно один пробел после двоеточия — «Макс.: 4°, мин.: 2°».
 */
fun formatMaxMinRu(max: Int, min: Int): String = "Макс.: $max°, мин.: $min°"

/**
 * «2,5» — миллиметры осадков с запятой, один знак после неё. Интерфейс русский,
 * поэтому «Σ 2.5 мм» на графике осадков читалось как опечатка (кадр r14_10).
 * Формат детерминирован (Locale.US + замена точки), а не зависит от локали телефона.
 */
fun formatMmRu(value: Double): String =
    String.format(Locale.US, "%.1f", value).replace('.', ',')
