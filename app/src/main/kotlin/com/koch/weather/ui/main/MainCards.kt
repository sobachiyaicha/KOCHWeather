package com.koch.weather.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.components.RemotePhoto
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Блоки главного экрана, переведённые на токены и примитивы (этап 2).
 * Порядок блоков задаёт MainScreen: голубая тема — как в refs/clean_main.png,
 * остальные — как в refs/theme_copper.png / f_063.
 */

/** «А знали ли вы…» — по референсу это строка без плашки (refs/premium_ads.png, 1:57). */
@Composable
internal fun AiFactCard(
    aiFact: String,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KochRadius.panel))
            .clickable(onClick = onClick)
            .padding(vertical = KochSpace.s),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = colors.accentPromo,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "А знали ли вы...",
                style = KochType.caption,
                color = colors.accentPromo
            )
        }
        Text(
            text = aiFact.trim(),
            style = KochType.sub,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Премиум-строка: текст слева, синяя капсула справа (refs/premium_ads.png). */
@Composable
internal fun PremiumBanner(
    isPremiumPurchased: Boolean,
    onOpenPaywall: () -> Unit
) {
    val colors = LocalKochColors.current
    KochCard(
        material = if (colors.isDark) KochMaterial.Solid else KochMaterial.Glass,
        padding = PaddingValues(horizontal = KochSpace.m, vertical = KochSpace.s),
        onClick = onOpenPaywall
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Больше советов и эксклюзивных функций",
                style = KochType.caption,
                color = colors.textPrimary.copy(alpha = 0.8f),
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(KochSpace.s))
            Row(
                modifier = Modifier
                    .clip(KochShape.pill)
                    .background(
                        if (isPremiumPurchased) colors.accentSuccess else colors.accent
                    )
                    .padding(horizontal = KochSpace.m, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPremiumPurchased) {
                    Text(
                        text = "★ ПРЕМИУМ",
                        style = KochType.chip,
                        color = Color.White
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Подписка Премиум",
                            style = KochType.chip,
                            color = Color.White,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "5.99 BYN",
                                style = KochType.caption,
                                color = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "29.99",
                                style = KochType.caption,
                                color = Color.White.copy(alpha = 0.55f),
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Реклама: зелёный градиент, фото справа во всю высоту (refs/premium_ads.png, 2:17). */
@Composable
internal fun AdBannerCard(
    imageUrl: String?,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(KochRadius.card))
            .background(
                Brush.verticalGradient(
                    listOf(colors.accentAdTop, colors.accentAdBottom)
                )
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(KochSpace.m),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "РЕКЛАМА",
                style = KochType.microCaps,
                color = colors.accentAdLabel
            )
            Text(
                text = "Дизайнеры больше не нужны!",
                style = KochType.bodyStrong,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = "Научим делать сайты с помощью нейросетей за 15 минут",
                style = KochType.sub,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2
            )
        }
        RemotePhoto(
            url = imageUrl,
            contentDescription = "Реклама: ноутбук",
            modifier = Modifier
                .width(104.dp)
                .fillMaxHeight(),
            shape = RectangleShape,
            placeholderIcon = Icons.Default.AutoAwesome,
            placeholderTint = Color.White.copy(alpha = 0.7f)
        )
    }
}

/** Сетка 2×2 сервисов: без фото внутри карточек (refs/cards_taxi_food.png, 2:05). */
@Composable
internal fun ServicesGrid(
    onOpenFlight: () -> Unit,
    onOpenLootbox: () -> Unit,
    onOpenTaxi: () -> Unit,
    onOpenFood: () -> Unit
) {
    val colors = LocalKochColors.current
    Column(verticalArrangement = Arrangement.spacedBy(KochSpace.m)) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(KochSpace.m)
        ) {
            ServiceCard(
                label = "Путешествия",
                title = "Билеты в Дубай",
                description = "Вы недавно смотрели погоду в Дубае, хотите приобрести билеты?",
                icon = Icons.Default.Flight,
                iconTint = colors.accent,
                showChevron = true,
                onClick = onOpenFlight,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ServiceCard(
                label = "Лутбоксы",
                title = "Не повезло с погодой? Повезет тут!",
                icon = Icons.Default.CardGiftcard,
                iconTint = colors.accentPromo,
                showChevron = true,
                onClick = onOpenLootbox,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(KochSpace.m)
        ) {
            ServiceCard(
                label = "Такси",
                icon = Icons.Default.LocalTaxi,
                iconTint = colors.accentPromo,
                chip = "Куда едем?",
                onClick = onOpenTaxi,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ServiceCard(
                label = "Еда",
                icon = Icons.Default.Fastfood,
                iconTint = Color(0xFFFF8A65),
                chip = "Что желаете?",
                onClick = onOpenFood,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ServiceCard(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    chip: String? = null,
    showChevron: Boolean = false
) {
    val colors = LocalKochColors.current
    KochCard(
        modifier = modifier,
        padding = PaddingValues(KochSpace.m),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(KochSpace.xs))
            Text(
                text = label,
                style = KochType.caption,
                color = colors.textPrimary.copy(alpha = 0.75f),
                maxLines = 1
            )
            if (showChevron) {
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (title != null) {
            Spacer(Modifier.height(KochSpace.s))
            Text(
                text = title,
                style = KochType.bodyStrong,
                color = colors.textPrimary,
                maxLines = 2
            )
        }
        if (description != null) {
            Text(
                text = description,
                style = KochType.sub,
                color = colors.textTertiary,
                maxLines = 3
            )
        }
        if (chip != null) {
            Spacer(Modifier.height(KochSpace.s))
            Row(
                modifier = Modifier
                    .clip(KochShape.pill)
                    .background(colors.textPrimary.copy(alpha = 0.12f))
                    .padding(horizontal = KochSpace.s, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chip,
                    style = KochType.chip,
                    color = colors.textPrimary.copy(alpha = 0.9f),
                    maxLines = 1
                )
                Spacer(Modifier.width(KochSpace.s))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
