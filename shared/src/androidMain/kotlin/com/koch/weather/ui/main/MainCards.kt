package com.koch.weather.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.AdCatalog
import com.koch.weather.data.model.AdCreative
import com.koch.weather.data.model.PremiumOffer
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.theme.AdPlateGreenBottom
import com.koch.weather.ui.theme.AdPlateGreenTop
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 *
 * После 2–4-го нажатия «Другой факт» вместо текста показывается картинка-совет
 * ([aiFactImageRes], advice_1 → advice_3). Когда картинки закончились,
 * [isAnotherFactAvailable] = false и кнопка скрыта до перезапуска приложения.
 */
@Composable
internal fun AiFactCard(
    aiFact: String,
    @DrawableRes aiFactImageRes: Int?,
    isAnotherFactAvailable: Boolean,
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
        if (aiFactImageRes != null) {
            // Квадратный бокс + ContentScale.Fit: картинка видна целиком без растяжения,
            // даже если исходник не квадратный (advice_1..3 — 1284×1333, 2560×2329, 539×540).
            AdviceImage(
                imageRes = aiFactImageRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(KochRadius.input))
            )
        } else {
            Text(
                text = aiFact.trim(),
                style = KochType.sub,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        // Явная, видимая кнопка-строка: без неё «Другой факт» существовал только в VM.
        // После 4-го нажатия (показана advice_3) кнопка исчезает до перезапуска.
        if (isAnotherFactAvailable) {
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
}

/** Ширина исходника, выше которой картинку совета декодируем в половинном размере. */
private const val ADVICE_IMAGE_SAMPLE_THRESHOLD = 1600

/**
 * Картинка-совет вместо текста факта.
 *
 * `painterResource` не используется: advice_2 — 2560×2329, полный ARGB-кадр занял бы
 * ~24 МБ. Здесь `inSampleSize` уменьшает его вдвое (~6 МБ); `ContentScale.Fit` держит
 * пропорции и показывает картинку целиком. Декод идёт на `Dispatchers.IO`, ошибка/OOM
 * не роняют ленту — остаётся тёмный бокс того же размера.
 */
@Composable
private fun AdviceImage(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = imageRes) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(resources, imageRes, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize =
                        if (bounds.outWidth > ADVICE_IMAGE_SAMPLE_THRESHOLD) 2 else 1
                    inScaled = false
                }
                BitmapFactory.decodeResource(resources, imageRes, options)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val bitmap = image
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Совет дня",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.Black.copy(alpha = 0.25f)))
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
 * Рекламный баннер главного экрана (ТЗ «реклама», п. 3).
 *
 * Показывает только фото1/фото2 из [AdCatalog.bannerCreatives]: фото не растягиваем
 * (`ContentScale.Fit`) и прижимаем к правому краю. Слева — зелёная прямоугольная
 * плашка цвета подложки креатива (`#0E7A3C…#12803F`) с белой надписью «СКИДКА 50%»
 * и мелким «только сегодня». Высота баннера прежняя — 164 dp.
 *
 * Ротацию фото1 ↔ фото2 задаёт ViewModel (`adBannerCreative`); здесь только
 * кроссфейд между двумя соседними состояниями, чтобы смена не «мигала».
 */
@Composable
internal fun AdBannerCard(creative: AdCreative) {
    // Градиент плашки — объект `Brush`, а не цвет: создаём один раз, а не на каждый кадр.
    val plateGradient = remember {
        Brush.verticalGradient(listOf(AdPlateGreenTop, AdPlateGreenBottom))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clip(RoundedCornerShape(KochRadius.card))
            .background(plateGradient)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = KochSpace.l, end = KochSpace.s),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = AdCatalog.BANNER_DISCOUNT_RU,
                style = KochType.displayCity,
                color = Color.White,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = AdCatalog.BANNER_NOTE_RU,
                style = KochType.caption,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1
            )
        }
        Crossfade(
            targetState = creative,
            modifier = Modifier
                .fillMaxHeight()
                // Фото занимает правые 42 %: «СКИДКА 50%» (154 dp при 22 sp) гарантированно
                // влезает в плашку даже на 320 dp-экране, а текст не пересекается с фото.
                .fillMaxWidth(0.42f),
            animationSpec = tween(durationMillis = 420),
            label = "ad_banner_rotation"
        ) { current ->
            Image(
                painter = painterResource(id = current.mediaRes),
                contentDescription = "Реклама",
                contentScale = ContentScale.Fit,
                // Fit + CenterEnd: фото не растянуто и прижато к правому краю баннера.
                alignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxSize()
            )
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
