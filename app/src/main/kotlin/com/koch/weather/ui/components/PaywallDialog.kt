package com.koch.weather.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import com.koch.weather.data.model.PremiumOffer
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.KochPanel
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Пейволл. Два шага в одном листе:
 *  1) витрина → кнопка только запрашивает покупку (`requestPremiumPurchase`);
 *  2) подтверждение → `confirmPremiumPurchase`, пока идёт запись — спиннер и disabled.
 * Обхода нет: без подтверждения подписка не активируется (блокер №7).
 *
 * Все ценники/период/примечание берутся из одного state — [PremiumOffer] (data-coder):
 * тариф «Годовой VIP», 179 ₽ (старая цена 899 ₽), период «год», «списание один раз».
 * Никаких «в месяц».
 */
@Composable
fun PaywallDialog(
    isPremiumPurchased: Boolean,
    isPending: Boolean,
    offer: PremiumOffer = PremiumOffer(),
    onRequestPurchase: () -> Unit,
    onConfirmPurchase: () -> Unit,
    onCancelPending: () -> Unit,
    onResetPremium: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var confirming by remember { mutableStateOf(false) }
    val showConfirm = step == 2 || isPending

    // После успешной покупки возвращаемся на витрину: там уже статус «ПРЕМИУМ АКТИВЕН».
    androidx.compose.runtime.LaunchedEffect(isPremiumPurchased, isPending) {
        if (isPremiumPurchased && !isPending) {
            step = 1
            confirming = false
        }
    }
    // Страховка: если запись в DataStore не ответила, кнопка не остаётся «вечной».
    androidx.compose.runtime.LaunchedEffect(confirming) {
        if (confirming) {
            kotlinx.coroutines.delay(4000)
            confirming = false
        }
    }

    KochSheet(
        onDismiss = {
            if (isPending) onCancelPending()
            onDismiss()
        },
        maxHeightFraction = 0.86f,
        content = {
            if (showConfirm) {
                ConfirmPurchaseContent(
                    offer = offer,
                    isConfirming = confirming,
                    onConfirm = {
                        confirming = true
                        onConfirmPurchase()
                    },
                    onCancel = {
                        onCancelPending()
                        step = 1
                    }
                )
            } else {
                ShowroomContent(
                    offer = offer,
                    isPremiumPurchased = isPremiumPurchased,
                    onDismiss = onDismiss,
                    onResetPremium = onResetPremium
                )
            }
        },
        footer = {
            if (!showConfirm) {
                if (isPremiumPurchased) {
                    // Активная подписка: статус уже в контенте, CTA не нужна.
                    KochTextButton(
                        text = "Закрыть",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    KochPrimaryButton(
                        text = "Купить подписку Премиум",
                        onClick = {
                            onRequestPurchase()
                            step = 2
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ShowroomContent(
    offer: PremiumOffer,
    isPremiumPurchased: Boolean,
    onDismiss: () -> Unit,
    onResetPremium: () -> Unit
) {
    val colors = LocalKochColors.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = KochSpace.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = colors.accentStar,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(KochSpace.s))
                Text(
                    text = "Погода ПРЕМИУМ",
                    style = KochType.titleSheet,
                    color = colors.textPrimary
                )
            }
            Text(
                text = "Больше советов и эксклюзивных функций",
                style = KochType.sub,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        KochCloseButton(onClick = onDismiss)
    }

    Spacer(Modifier.height(KochSpace.m))

    PremiumFeature("Полное отключение всех рекламных баннеров")
    PremiumFeature("Разблокировка всех тем оформления (Лес, Марс)")
    PremiumFeature("Мгновенный разгон облаков пальцем")
    PremiumFeature("Иммунитет к блокировке «Пора на улицу»")

    Spacer(Modifier.height(KochSpace.m))

    KochPanel {
        Text(
            text = "Тариф «${offer.tariffTitleRu}»",
            style = KochType.bodyStrong,
            color = colors.textPrimary
        )
        Text(
            text = offer.discountLabel,
            style = KochType.sub,
            color = colors.accentPromo
        )
        Text(
            // Период и условие — одной согласованной формулировкой из того же state:
            // «за год · списание один раз». Никакого «в месяц» при годовом тарифе.
            text = "${offer.periodLabel} · ${offer.billingNoteRu}",
            style = KochType.caption,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(KochSpace.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
        ) {
            Text(
                text = offer.oldPriceLabel,
                style = KochType.caption,
                color = colors.textTertiary,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(end = KochSpace.s)
            )
            Text(
                text = offer.priceLabel,
                style = KochType.numberPrice,
                color = colors.textPrimary
            )
        }
    }

    if (isPremiumPurchased) {
        Spacer(Modifier.height(KochSpace.m))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colors.accentSuccess,
                modifier = Modifier.padding(end = KochSpace.s).size(16.dp)
            )
            Text(
                text = "★ ПРЕМИУМ АКТИВЕН",
                style = KochType.bodyStrong,
                color = colors.accentSuccess
            )
        }
        Spacer(Modifier.height(KochSpace.s))
        KochTextButton(
            text = "Сбросить подписку (для теста рекламы)",
            onClick = onResetPremium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConfirmPurchaseContent(
    offer: PremiumOffer,
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalKochColors.current

    Text(
        text = "Подтвердить покупку?",
        style = KochType.titleSheet,
        color = colors.textPrimary,
        modifier = Modifier.padding(top = KochSpace.m, bottom = KochSpace.s)
    )
    Text(
        text = "Погода ПРЕМИУМ · ${offer.tariffTitleRu}",
        style = KochType.bodyStrong,
        color = colors.textPrimary
    )
    Text(
        text = "${offer.priceLabel} ${offer.periodLabel} · ${offer.billingNoteRu}",
        style = KochType.sub,
        color = colors.textTertiary
    )
    Spacer(Modifier.height(KochSpace.l))
    KochPrimaryButton(
        text = if (isConfirming) "Подтверждаем…" else "Подтвердить",
        onClick = onConfirm,
        loading = isConfirming,
        enabled = !isConfirming
    )
    Spacer(Modifier.height(KochSpace.s))
    KochTextButton(
        text = "Отмена",
        onClick = onCancel,
        enabled = !isConfirming,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PremiumFeature(text: String) {
    val colors = LocalKochColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = KochSpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(colors.accentSuccess.copy(alpha = 0.14f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colors.accentSuccess,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.width(KochSpace.s))
        Text(
            text = text,
            style = KochType.body,
            color = colors.textSecondary
        )
    }
}
