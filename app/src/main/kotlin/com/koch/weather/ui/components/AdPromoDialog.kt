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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
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
 * Рекламный лист (refs/premium_ads.png, 1:57): фото-герой, метка «РЕКЛАМА»,
 * заголовок и одна CTA в премиум. Никакой зелёной рамки и квадратной иконки.
 */
@Composable
fun AdPromoDialog(
    onOpenPaywall: () -> Unit,
    onDismiss: () -> Unit,
    imageUrl: String? = null
) {
    val colors = LocalKochColors.current

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.8f,
        hero = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KochSize.hero)
            ) {
                RemotePhoto(
                    url = imageUrl,
                    contentDescription = "Реклама: ноутбук",
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    placeholderIcon = Icons.Default.LaptopMac,
                    placeholderTint = colors.textSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, colors.accentAdBottom.copy(alpha = 0.85f))
                            )
                        )
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
                text = "Дизайнеры больше не нужны!",
                style = KochType.titleCard,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = KochSpace.l)
            )
            Text(
                text = "Научим делать сайты с помощью нейросетей за 15 минут",
                style = KochType.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = KochSpace.xs)
            )
            Spacer(Modifier.height(KochSpace.xs))
        },
        footer = {
            Column {
                KochPrimaryButton(
                    text = "Подписка Премиум · 5.99 BYN",
                    onClick = onOpenPaywall
                )
                Spacer(Modifier.height(KochSpace.s))
                KochTextButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
