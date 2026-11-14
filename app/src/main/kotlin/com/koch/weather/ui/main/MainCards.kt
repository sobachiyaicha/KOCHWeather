package com.koch.weather.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.AdCreative
import com.koch.weather.data.model.PremiumOffer
import com.koch.weather.ui.components.RemotePhoto
import com.koch.weather.ui.components.adIconFor
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Блоки главного экрана, переведённые на токены и примитивы (этап 2).
 * Порядок блоков задаёт MainScreen: голубая тема — как в refs/clean_main.png,
 * остальные — как в refs/theme_copper.png / f_063.
 */

/**
 * «А знали ли вы…» — по референсу это строка без плашки (refs/premium_ads.png, 1:57).
 *
 * Тап по блоку и явная строка «Другой факт» (иконка `Refresh` + `KochType.caption`)
 * листают факты: [onClick] ведёт в `MainScreenViewModel.refreshAiFact()`. До ревью
 * Фазы 5 блок открывал модалку травы, а `onRefreshAiFact` был мёртвым параметром
 * (item 12). Долгое нажатие — отдельный вход в сатирическую блокировку «Пора на
 * улицу!» (R11): у VM пока нет таймера «слишком долго смотрели погоду», поэтому
 * блокировка остаётся достижимой этим жестом, а обычный тап честно листает факты.
 */
@Composable
internal fun AiFactCard(
    aiFact: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalKochColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KochRadius.panel))
            // M5: блок лежал прямо на фото и на светлой полосе «Медного заката» давал
            // 3.3 : 1 (тело) и 2.46 : 1 (заголовок). Тёмный скрим 48 % держит контраст
            // ≥4.5 : 1 на самом светлом пикселе любого из семи фонов (замер по фонам
            // res/drawable-nodpi + скрим KochBackground: min 4.8 : 1 для золота заголовка,
            // 5.9 : 1 для тела) и не превращает блок в плотную карточку.
            .background(Color.Black.copy(alpha = 0.48f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = KochSpace.l, vertical = KochSpace.s),
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
        // Явная, видимая кнопка-строка: без неё «Другой факт» существовал только в VM.
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(KochShape.pill)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = KochSpace.s, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = colors.accentPromo,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Другой факт",
                style = KochType.caption,
                color = colors.accentPromo
            )
        }
    }
}

/** Премиум-строка: текст слева, синяя капсула справа (refs/premium_ads.png). */
@Composable
internal fun PremiumBanner(
    isPremiumPurchased: Boolean,
    offer: PremiumOffer,
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
                                text = offer.priceLabel,
                                style = KochType.caption,
                                color = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = offer.oldPriceLabel,
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

/**
 * Реклама (item 9 фазы 5): зелёный градиент, метка «РЕКЛАМА», бренд, заголовок и текст
 * реального креатива из `uiState.adCreative`; фото справа во всю высоту.
 *
 * Раньше здесь были пришиты «Дизайнеры больше не нужны» и кнопка покупки премиума —
 * теперь контент приходит из [AdCreative], а CTA-пилюля повторяет её `ctaLabel`
 * и ведёт в функцию креатива (маршрутизацию делает вызывающий экран).
 */
@Composable
internal fun AdBannerCard(
    creative: AdCreative,
    imageUrl: String?,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    // Градиент баннера — объект `Brush`, а не цвет. Раньше он создавался в композиции
    // элемента списка, то есть на каждый пересчёт; вынесен в `remember` по цветам темы.
    val adGradient = remember(colors.accentAdTop, colors.accentAdBottom) {
        Brush.verticalGradient(
            listOf(colors.accentAdTop, colors.accentAdBottom)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // D-3 (feature-qa, run 15): при 148 dp тело креатива «…Достав…» обрывалось
            // на второй строке. +16 dp высоты и третья строка тела — фраза («Доставка
            // курьером за 30 минут…», «…доставка бесплатно.») читается целиком.
            .height(164.dp)
            .clip(RoundedCornerShape(KochRadius.card))
            .background(adGradient)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(KochSpace.m),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "РЕКЛАМА",
                        style = KochType.microCaps,
                        color = colors.accentAdLabel
                    )
                    Spacer(Modifier.width(KochSpace.s))
                    Text(
                        text = creative.brandRu,
                        style = KochType.caption,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = creative.headline,
                    style = KochType.bodyStrong,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // M2c + D-3: тело креатива обрезалось «…зонты с» / «…Достав…». Три строки
                // с Ellipsis: предложение целиком, честный «…» — только на заведомо
                // длинных хвостах (кадр 16/16c с этим согласован).
                Text(
                    text = creative.body,
                    style = KochType.sub,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // CTA-пилюля: видно, куда ведёт баннер, без отдельной «кнопки покупки».
            Row(
                modifier = Modifier
                    .clip(KochShape.pill)
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(start = KochSpace.s, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = creative.ctaLabel,
                    style = KochType.chip,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (imageUrl != null) {
            RemotePhoto(
                url = imageUrl,
                contentDescription = "Реклама: ${creative.brandRu}",
                modifier = Modifier
                    .width(104.dp)
                    .fillMaxHeight(),
                shape = RectangleShape,
                placeholderIcon = adIconFor(creative.imageCategory),
                placeholderTint = Color.White.copy(alpha = 0.7f)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = adIconFor(creative.imageCategory),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(34.dp)
                )
            }
        }
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
            horizontalArrangement = Arrangement.spacedBy(KochSpace.m)
        ) {
            ServiceCard(
                label = "Путешествия",
                title = "Билеты в Дубай ›",
                description = "Вы недавно смотрели погоду в Дубае, хотите приобрести билеты?",
                icon = Icons.Default.Flight,
                iconTint = colors.accent,
                showChevron = true,
                onClick = onOpenFlight,
                modifier = Modifier.weight(1f).heightIn(min = 132.dp)
            )
            ServiceCard(
                label = "Лутбоксы",
                title = "Не повезло с погодой? Повезёт тут!",
                icon = Icons.Default.CardGiftcard,
                iconTint = colors.accentPromo,
                showChevron = true,
                onClick = onOpenLootbox,
                modifier = Modifier.weight(1f).heightIn(min = 132.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(KochSpace.m)
        ) {
            ServiceCard(
                label = "Такси",
                icon = Icons.Default.LocalTaxi,
                iconTint = colors.accentPromo,
                chip = "Куда едем?",
                onClick = onOpenTaxi,
                // m2: у «Такси»/«Еды» контента на ~40 %, низ карточки пустовал.
                // Токен serviceCard (104 dp) прижат к содержимому.
                modifier = Modifier.weight(1f).heightIn(min = KochSize.serviceCard)
            )
            ServiceCard(
                label = "Еда",
                icon = Icons.Default.Fastfood,
                iconTint = Color(0xFFFF8A65),
                chip = "Что желаете?",
                onClick = onOpenFood,
                modifier = Modifier.weight(1f).heightIn(min = KochSize.serviceCard)
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (description != null) {
            Text(
                text = description,
                style = KochType.sub,
                color = colors.textTertiary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
