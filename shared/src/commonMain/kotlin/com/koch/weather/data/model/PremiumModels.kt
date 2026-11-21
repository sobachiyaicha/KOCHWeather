package com.koch.weather.data.model

import androidx.compose.runtime.Immutable

/**
 * Единственный источник цены/периода премиума для всех экранов.
 * UI не должен хардкодить 179/899/«в месяц» в разных местах.
 *
 * Цены — в российских рублях (179 ₽ вместо прежних 5.99 в чужой валюте;
 * те же деньги по курсу ≈30 ₽, скидка 80 % сохранена).
 */
@Immutable
data class PremiumOffer(
    val tariffTitleRu: String = "Годовой VIP",
    val priceRub: Int = 179,
    val oldPriceRub: Int = 899,
    val discountPercent: Int = 80,
    val periodRu: String = "год",
    val billingNoteRu: String = "списание один раз"
) {
    val priceLabel: String get() = formatRub(priceRub)
    val oldPriceLabel: String get() = formatRub(oldPriceRub)
    val periodLabel: String get() = "за $periodRu"
    val discountLabel: String get() = "Скидка $discountPercent% только сегодня"

    /** «179 ₽», «34 500 ₽» — разряды через пробел, как в остальном приложении.
     *  Замена String.format("%,d", value).replace(',', ' ') — то же разбиение по 3 цифры. */
    private fun formatRub(value: Int): String {
        val digits = kotlin.math.abs(value).toString()
        val grouped = StringBuilder()
        for (i in digits.indices) {
            if (i > 0 && (digits.length - i) % 3 == 0) grouped.append(' ')
            grouped.append(digits[i])
        }
        val sign = if (value < 0) "-" else ""
        return sign + grouped.toString() + " ₽"
    }
}
