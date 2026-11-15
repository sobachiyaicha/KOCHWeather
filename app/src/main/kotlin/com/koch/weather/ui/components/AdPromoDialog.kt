package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.AdAction
import com.koch.weather.data.model.AdCreative
import com.koch.weather.data.model.ImageCategory
import com.koch.weather.data.model.PremiumOffer
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Рекламный лист (item 9 фазы 5). Рисует реальный креатив из `uiState.adCreative`:
 * бренд, заголовок, текст и CTA-кнопку с её собственным действием
 * ([AdAction.PAYWALL]/[AdAction.LOOTBOX]/[AdAction.UMBRELLA_SHOP]/[AdAction.FOOD]).
 *
 * Под CTA — обязательная строка `disclaimerRu` («Реклама. …») и вторичная ссылка
 * на премиум с ценой, периодом и объяснением, что входит: «Отключить рекламу —
 * Премиум 179 ₽/год» + «Без рекламы и все темы оформления сразу». Кнопки «Подписка
 * Премиум» без пояснения больше нет.
 */
@Composable
fun AdPromoDialog(
    creative: AdCreative,
    imageUrl: String?,
    onAction: (AdAction) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    // Цена/период берутся из единого источника (PremiumOffer), а не хардкодятся в UI.
    val offer = remember { PremiumOffer() }

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.88f,
        hero = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KochSize.hero)
            ) {
                if (imageUrl != null) {
                    RemotePhoto(
                        url = imageUrl,
                        contentDescription = "Реклама: ${creative.brandRu}",
                        modifier = Modifier.fillMaxSize(),
                        shape = RectangleShape,
                        placeholderIcon = adIconFor(creative.imageCategory),
                        placeholderTint = colors.textSecondary
                    )
                } else {
                    // Креатив без фото (зонты): честная плашка с иконкой, а не серый бокс.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.accentAdTop, colors.accentAdBottom)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = adIconFor(creative.imageCategory),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // M7: зелёный градиент бренда поверх фото давал креативу зелёный
                        // каст (кадр r14_08). Теперь нейтральный чёрный скрим 40 % —
                        // фото показывает свой цвет, текст и метка «РЕКЛАМА» читаются.
                        .background(Color.Black.copy(alpha = 0.40f))
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(KochSpace.l)
                        .background(
                            colors.accentAdLabel.copy(alpha = 0.92f),
                            RoundedCornerShape(KochRadius.badge)
                        )
                        .padding(horizontal = KochSpace.s, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "РЕКЛАМА",
                        style = KochType.microCaps,
                        color = Color(0xFF1E3A12)
                    )
                }
                KochCloseButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KochSpace.s)
                )
            }
        },
        content = {
            Text(
                text = creative.brandRu,
                style = KochType.microCaps,
                color = colors.accentPromo,
                modifier = Modifier.padding(top = KochSpace.l)
            )
            Text(
                text = creative.headline,
                style = KochType.titleCard,
                color = colors.textPrimary
            )
            Text(
                text = creative.body,
                style = KochType.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = KochSpace.xs)
            )
            Spacer(Modifier.height(KochSpace.m))
            HorizontalDivider(color = colors.textPrimary.copy(alpha = 0.08f), thickness = 0.5.dp)
            Text(
                text = creative.disclaimerRu,
                style = KochType.sub,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = KochSpace.s)
            )
        },
        footer = {
            Column {
                KochPrimaryButton(
                    text = creative.ctaLabel,
                    onClick = { onAction(creative.action) }
                )
                Spacer(Modifier.height(KochSpace.s))
                KochTextButton(
                    text = "Отключить рекламу — Премиум ${offer.priceLabel}/${offer.periodRu}",
                    color = colors.accentPromo,
                    onClick = { onAction(AdAction.PAYWALL) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Без рекламы и все темы оформления сразу",
                    style = KochType.sub,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(KochSpace.xs))
                KochTextButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

/** Иконка креатива по категории фото: без фото карточка не выглядит пустой. */
internal fun adIconFor(category: ImageCategory?): ImageVector = when (category) {
    ImageCategory.AD_LAPTOP -> Icons.Default.LaptopMac
    ImageCategory.IPHONE, ImageCategory.IPHONE_FRONT -> Icons.Default.PhoneIphone
    ImageCategory.FOOD,
    ImageCategory.FOOD_FRIES,
    ImageCategory.FOOD_NUGGETS,
    ImageCategory.FOOD_PIROSHKI,
    ImageCategory.FOOD_HOTDOG,
    ImageCategory.FOOD_COFFEE -> Icons.Default.Fastfood
    ImageCategory.TRAVEL_DUBAI -> Icons.Default.LocalMall
    ImageCategory.SKY_BLUE,
    ImageCategory.SKY_SUNSET,
    ImageCategory.SKY_NORTHERN -> Icons.Default.CardGiftcard
    null -> Icons.Default.LocalMall
}
