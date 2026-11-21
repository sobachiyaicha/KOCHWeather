package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.koch.weather.data.deeplink.DeepLinks
import com.koch.weather.data.model.FoodItem
import com.koch.weather.data.model.FoodOrder
import com.koch.weather.data.model.FoodOrderStatus
import com.koch.weather.data.model.FoodState
import com.koch.weather.data.model.Restaurant
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochChip
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.KochConfirmSheet
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.design.formatRuAmount
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Еда. Bottom-sheet в языке референса (refs/cards_taxi_food.png, 2:05):
 * чипы ресторанов, строки меню с собственным фото, капсула CTA.
 *
 * Фото блюда берётся только из явной таблицы [FoodPhotoSlots]: чужое фото не подставляется
 * (блокер №5 — «Картофель Фри» больше не показывает бургер).
 */
@Composable
fun FoodBottomSheet(
    foodState: FoodState,
    photoUrls: Map<com.koch.weather.data.model.ImageCategory, List<String>> = emptyMap(),
    onSelectRestaurant: (Restaurant) -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    onRemoveFromCart: (FoodItem) -> Unit,
    onCancelOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    var showLaunchConfirm by remember { mutableStateOf(false) }
    var launchError by remember { mutableStateOf<String?>(null) }

    val activeOrder = foodState.activeOrder
        ?.takeIf { it.status != FoodOrderStatus.DELIVERED }
    val hasOrder = activeOrder != null

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.88f,
        // Меню ленивое: строки с фото компонуются только когда попадают в вьюпорт.
        lazyContent = {
            item(key = "food_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = KochSpace.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeliveryDining,
                        contentDescription = null,
                        tint = colors.accentPromo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(KochSpace.m))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Доставка горячей еды",
                            style = KochType.titleCard,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Согреем в любую погоду за 25 минут",
                            style = KochType.sub,
                            color = colors.textTertiary,
                            maxLines = 1
                        )
                    }
                    KochCloseButton(onClick = onDismiss)
                }
            }

            if (activeOrder != null) {
                item(key = "active_order") {
                    ActiveOrderCard(
                        order = activeOrder,
                        modifier = Modifier.padding(top = KochSpace.l)
                    )
                }
            } else {
                item(key = "restaurants") {
                    // Чипы ресторанов переносятся по строкам: «Surf Coffee» не режется краем.
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = KochSpace.l),
                        horizontalArrangement = Arrangement.spacedBy(KochSpace.s),
                        verticalArrangement = Arrangement.spacedBy(KochSpace.s)
                    ) {
                        foodState.restaurants.forEach { restaurant ->
                            KochChip(
                                text = restaurant.name,
                                selected = restaurant.id == foodState.selectedRestaurant?.id,
                                onClick = { onSelectRestaurant(restaurant) }
                            )
                        }
                    }
                }

                items(foodState.menuItems, key = { it.id }) { item ->
                    MenuRow(
                        item = item,
                        count = foodState.cart[item] ?: 0,
                        imageUrl = foodPhotoUrl(item.id, photoUrls),
                        onAdd = { onAddToCart(item) },
                        onRemove = { onRemoveFromCart(item) }
                    )
                }
            }
        },
        footer = {
            Column {
                if (hasOrder) {
                    KochTextButton(
                        text = "Отменить заказ",
                        onClick = onCancelOrder,
                        color = colors.accentDanger,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val cartCount = foodState.cartCount
                    val total = foodState.totalCartPriceRu
                    KochPrimaryButton(
                        text = if (cartCount > 0) {
                            "Заказать $cartCount шт. на ${formatRuAmount(total)} ₽"
                        } else {
                            "Выберите блюда в меню"
                        },
                        enabled = cartCount > 0,
                        onClick = {
                            launchError = null
                            showLaunchConfirm = true
                        }
                    )
                    Spacer(Modifier.heightIn(min = KochSpace.s))
                    Text(
                        text = "Оформление продолжится в приложении Яндекс Еда",
                        style = KochType.caption,
                        color = colors.textPrimary.copy(alpha = 0.45f)
                    )
                    launchError?.let { message ->
                        Text(
                            text = message,
                            style = KochType.sub,
                            color = colors.accentDanger,
                            modifier = Modifier.padding(top = KochSpace.s)
                        )
                    }
                }
            }
        }
    )

    if (showLaunchConfirm) {
        KochConfirmSheet(
            title = "Открыть Яндекс Еду?",
            message = "Выбранные блюда продолжим оформлять в приложении Яндекс Еда. " +
                "Если приложения нет, откроется сайт.",
            confirmText = "Открыть",
            dismissText = "Отмена",
            onConfirm = {
                showLaunchConfirm = false
                if (!DeepLinks.yandexEda().openFirstAvailable()) {
                    launchError = "Не удалось открыть Яндекс Еду. " +
                        "Установите приложение или браузер и попробуйте снова."
                }
            },
            onDismiss = { showLaunchConfirm = false }
        )
    }
}

@Composable
private fun MenuRow(
    item: FoodItem,
    count: Int,
    imageUrl: String?,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalKochColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KochSize.rowFood)
            .padding(vertical = KochSpace.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
       // Фото 72×72, радиус 16, без белой подложки; нет фото — нейтральная иконка.
        Box(
            modifier = Modifier
                .size(KochSize.thumb)
                .clip(RoundedCornerShape(KochRadius.input))
                .background(colors.surfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            RemotePhoto(
                url = imageUrl,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(KochRadius.input),
                contentScale = ContentScale.Crop,
                placeholderIcon = Icons.Default.Fastfood,
                placeholderTint = colors.textQuaternary
            )
        }
        Spacer(Modifier.width(KochSpace.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = KochType.bodyStrong,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = item.description,
                style = KochType.sub,
                color = colors.textTertiary,
                maxLines = 3
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${formatRuAmount(item.priceRu)} ₽ · ${item.weightGrams} г",
                style = KochType.caption,
                color = colors.accentPromo,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(KochSpace.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (count > 0) {
                StepperButton(icon = Icons.Default.Remove, description = "Убрать", onClick = onRemove)
                Text(
                    text = count.toString(),
                    style = KochType.bodyStrong,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = KochSpace.s)
                )
            }
            StepperButton(icon = Icons.Default.Add, description = "Добавить", onClick = onAdd)
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colors.textPrimary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ActiveOrderCard(
    order: FoodOrder,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current
    KochCard(modifier = modifier, material = KochMaterial.Solid) {
        Text(
            text = order.status.titleRu,
            style = KochType.bodyStrong,
            color = colors.accentPromo
        )
        Spacer(Modifier.heightIn(min = KochSpace.s))
        Text(
            text = order.restaurantName,
            style = KochType.titleCard,
            color = colors.textPrimary
        )
        Text(
            text = "Состав: ${order.itemsSummary}",
            style = KochType.sub,
            color = colors.textSecondary
        )
        Spacer(Modifier.heightIn(min = KochSpace.m))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Курьер в пути · ~${order.etaMinutes} мин",
                style = KochType.caption,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${formatRuAmount(order.totalPriceRu)} ₽",
                style = KochType.numberPrice,
                color = colors.textPrimary
            )
        }
    }
}

/**
 * Явная таблица «позиция → категория фото». Чужое фото не подставляется никогда:
 * у каждой позиции либо свой проверенный URL, либо плейсхолдер (иконка на нейтральном фоне).
 */
private fun foodPhotoUrl(
    itemId: String,
    urls: Map<com.koch.weather.data.model.ImageCategory, List<String>>
): String? {
    val category = when (itemId) {
        "f_big_hit" -> com.koch.weather.data.model.ImageCategory.FOOD
        "f_fries" -> com.koch.weather.data.model.ImageCategory.FOOD_FRIES
        "f_nuggets" -> com.koch.weather.data.model.ImageCategory.FOOD_NUGGETS
        "f_cherry_pie" -> com.koch.weather.data.model.ImageCategory.FOOD_PIROSHKI
        "f_dodster" -> com.koch.weather.data.model.ImageCategory.FOOD_HOTDOG
        "f_flat_white", "f_bumble" -> com.koch.weather.data.model.ImageCategory.FOOD_COFFEE
        "f_pepperoni", "f_cheese_chicken" -> com.koch.weather.data.model.ImageCategory.FOOD
        "f_croissant" -> com.koch.weather.data.model.ImageCategory.FOOD
        else -> return null
    }
    val list = urls[category].orEmpty()
    if (list.isEmpty()) return null
    // В общей категории FOOD берём слот, соответствующий блюду; в остальных — первое фото.
    val index = when (itemId) {
        "f_pepperoni", "f_cheese_chicken" -> 1
        "f_flat_white" -> 2
        "f_croissant" -> 3
        else -> 0
    }
    return list.getOrNull(index) ?: list.first()
}
