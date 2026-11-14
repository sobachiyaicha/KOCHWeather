package com.koch.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.KochColorDot
import com.koch.weather.ui.design.KochPanel
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Магазин зонтов. Тот же язык, что у остальных листов: карточка товара, три расцветки,
 * цена и одна капсула. Локальное состояние покупки сохранено (бэкенда нет).
 */
@Composable
fun UmbrellaShopDialog(
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var isPurchased by remember { mutableStateOf(false) }

    val swatches = remember {
        listOf(
            Triple("Классический чёрный", "чёрный", Color(0xFF212121)),
            Triple("Небесно-голубой", "голубой", Color(0xFF42A5F5)),
            Triple("Золотой VIP", "золотой", Color(0xFFE9BF60))
        )
    }

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.72f,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = KochSpace.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BeachAccess,
                    contentDescription = null,
                    tint = colors.accentPromo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(KochSpace.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Зонты KOCH Protect",
                        style = KochType.titleCard,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Складной, антиштормовой",
                        style = KochType.sub,
                        color = colors.textTertiary
                    )
                }
                KochCloseButton(onClick = onDismiss)
            }

            Spacer(Modifier.height(KochSpace.l))

            KochPanel {
                Text(
                    text = "Автоматический антиштормовой зонт",
                    style = KochType.bodyStrong,
                    color = colors.textPrimary
                )
                Text(
                    text = "12 карбоновых спиц, влагоотталкивающее покрытие Teflon",
                    style = KochType.sub,
                    color = colors.textTertiary
                )
                Spacer(Modifier.height(KochSpace.m))
                Text(
                    text = swatches[selectedColorIndex].first,
                    style = KochType.caption,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(KochSpace.s))
                Row(horizontalArrangement = Arrangement.spacedBy(KochSpace.s)) {
                    swatches.forEachIndexed { index, swatch ->
                        KochColorDot(
                            color = swatch.third,
                            selected = index == selectedColorIndex,
                            contentDescription = "Расцветка: ${swatch.second}",
                            onClick = { selectedColorIndex = index }
                        )
                    }
                }
            }

            Spacer(Modifier.height(KochSpace.l))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Специальная цена",
                        style = KochType.caption,
                        color = colors.textTertiary
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "29.99 BYN",
                            style = KochType.numberPrice,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width(KochSpace.s))
                        Text(
                            text = "649 ₽",
                            style = KochType.caption,
                            color = colors.textTertiary
                        )
                    }
                }
            }

            if (isPurchased) {
                Spacer(Modifier.height(KochSpace.m))
                Text(
                    text = "Курьер уже бежит к вам сквозь дождь!",
                    style = KochType.caption,
                    color = colors.accentSuccess
                )
            }
        },
        footer = {
            Column {
                if (isPurchased) {
                    KochTextButton(
                        text = "Закрыть",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    KochPrimaryButton(
                        text = "Купить за 29.99 BYN",
                        onClick = { isPurchased = true }
                    )
                }
            }
        }
    )
}
