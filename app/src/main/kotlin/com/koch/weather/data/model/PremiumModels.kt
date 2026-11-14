package com.koch.weather.data.model

import androidx.compose.runtime.Immutable
import java.util.Locale

/**
 * Единственный источник цены/периода премиума для всех экранов.
 * UI не должен хардкодить 5.99/29.99/«в месяц» в разных местах.
 */
@Immutable
data class PremiumOffer(
    val tariffTitleRu: String = "Годовой VIP",
    val priceByn: Double = 5.99,
    val oldPriceByn: Double = 29.99,
    val discountPercent: Int = 80,
    val periodRu: String = "год",
    val billingNoteRu: String = "списание один раз"
) {
    val priceLabel: String get() = formatByn(priceByn)
    val oldPriceLabel: String get() = formatByn(oldPriceByn)
    val periodLabel: String get() = "за $periodRu"
    val discountLabel: String get() = "Скидка $discountPercent% только сегодня"

    private fun formatByn(value: Double): String =
        String.format(Locale.US, "%.2f BYN", value)
}
