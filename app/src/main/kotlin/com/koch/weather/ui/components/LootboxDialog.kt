package com.koch.weather.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.LootReward
import com.koch.weather.data.model.LootboxState
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochEmptyState
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Лутбоксы. Bottom-sheet (refs/premium_ads.png, 1:57): янтарный герой-ореол без рамки,
 * компактная история дропов, синяя капсула CTA. Никакой магенты и пустоты на пол-экрана.
 */
@Composable
fun LootboxDialog(
    lootboxState: LootboxState,
    onOpenLootbox: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current

    val pulse = rememberInfiniteTransition(label = "chest_pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chest_scale"
    )

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.8f,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = KochSpace.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = colors.accentPromo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(KochSpace.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Погодные Лутбоксы",
                        style = KochType.titleCard,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Не повезло с погодой? Повезёт тут!",
                        style = KochType.sub,
                        color = colors.textTertiary,
                        maxLines = 1
                    )
                }
            }

            // Герой-зона: только ореол и иконка, без вложенной панели и рамки.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 152.dp)
                    .padding(top = KochSpace.s),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 220.dp, height = 132.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    colors.accentPromo.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = colors.accentPromo,
                        modifier = Modifier
                            .size(56.dp)
                            .scale(if (lootboxState.isOpening) scale * 1.1f else scale)
                    )
                    Spacer(Modifier.height(KochSpace.m))
                    Text(
                        text = if (lootboxState.isOpening) {
                            "Открываем ящик удачи..."
                        } else {
                            "Сундук Утешения #2025"
                        },
                        style = KochType.bodyStrong,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Шанс на iPhone 16 Pro Max: 0.001%",
                        style = KochType.sub,
                        color = colors.textSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KochSpace.s, bottom = KochSpace.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "История дропов:",
                    style = KochType.titleSection,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Открыто: ${lootboxState.totalOpened}",
                    style = KochType.caption,
                    color = colors.textTertiary
                )
            }

            val reward = lootboxState.lastReward
            if (reward != null) {
                LootRewardRow(reward)
            } else {
                KochEmptyState(
                    icon = Icons.Default.CardGiftcard,
                    title = "Вы еще не открывали сундуки сегодня.",
                    subtitle = "Жмите кнопку ниже!"
                )
            }
        },
        footer = {
            KochPrimaryButton(
                text = "Открыть лутбокс БЕСПЛАТНО",
                onClick = onOpenLootbox,
                enabled = !lootboxState.isOpening,
                loading = lootboxState.isOpening
            )
        }
    )
}

@Composable
private fun LootRewardRow(reward: LootReward) {
    val colors = LocalKochColors.current
    val rarity = Color(reward.rarity.colorHex)
    KochCard(material = KochMaterial.Solid) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 40.dp)
                    .clip(CircleShape)
                    .background(rarity)
            )
            Spacer(Modifier.width(KochSpace.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reward.title,
                    style = KochType.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = 2
                )
                Text(
                    text = "${reward.rarity.titleRu} · ${reward.subtitle}",
                    style = KochType.sub,
                    color = colors.textTertiary,
                    maxLines = 2
                )
            }
            if (reward.unlockedThemeName != null) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = colors.textQuaternary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
