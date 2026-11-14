package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koch.weather.data.deeplink.DeepLinks
import com.koch.weather.data.model.*

@Composable
fun FoodBottomSheet(
    foodState: FoodState,
    foodImageUrls: List<String> = emptyList(),
    onSelectRestaurant: (Restaurant) -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    onRemoveFromCart: (FoodItem) -> Unit,
    onCancelOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showLaunchConfirm by remember { mutableStateOf(false) }
    var launchError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF261815),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7043)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFF7043).copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fastfood,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8A65),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Доставка горячей еды",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Согреем в любую погоду за 25 минут",
                                fontSize = 11.5.sp,
                                color = Color(0xFFFFAB91)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val activeOrder = foodState.activeOrder
                if (activeOrder != null && activeOrder.status != FoodOrderStatus.DELIVERED) {
                    // Active order view
                    ActiveOrderView(order = activeOrder, onCancel = onCancelOrder)
                } else {
                    // Restaurants horizontal row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(foodState.restaurants, key = { it.id }) { rest ->
                            val isSelected = rest.id == foodState.selectedRestaurant?.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFD84315) else Color(0x20FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                    color = if (isSelected) Color(0xFFFFAB91) else Color(0x20FFFFFF)
                                ),
                                modifier = Modifier.clickable { onSelectRestaurant(rest) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fastfood,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFFFFAB91),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(rest.name, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        Text("${rest.rating} ★ • ~${rest.deliveryTimeMinutes} мин", color = Color.White.copy(alpha = 0.7f), fontSize = 10.5.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Menu items list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(foodState.menuItems, key = { it.id }) { item ->
                            val count = foodState.cart[item] ?: 0
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x18FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x25FFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    RemotePhoto(
                                        url = foodImageUrl(item.id, foodImageUrls),
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        placeholderIcon = Icons.Default.Fastfood,
                                        placeholderTint = Color(0xFFFFAB91)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            item.description,
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${item.priceRu} ₽ / ${item.priceByn} BYN • ${item.weightGrams} г",
                                                color = Color(0xFFFFCC80),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Counter buttons are next to the price now, so the
                                            // "+" button never clips the dish description.
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (count > 0) {
                                                    IconButton(
                                                        onClick = { onRemoveFromCart(item) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Remove,
                                                            contentDescription = "Убрать",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "$count",
                                                        color = Color.White,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onAddToCart(item) },
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .background(Color(0xFFFF7043), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Добавить",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Order Button: opens Yandex Eda (app first, web fallback) after confirmation.
                    val cartCount = foodState.cartCount
                    val totalPrice = foodState.totalCartPriceRu
                    Button(
                        onClick = {
                            launchError = null
                            showLaunchConfirm = true
                        },
                        enabled = cartCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF7043),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (cartCount > 0) "Заказать $cartCount шт. на $totalPrice ₽ ›" else "Выберите блюда в меню",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Оформление продолжится в приложении Яндекс Еда",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.5.sp
                    )

                    launchError?.let { message ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            color = Color(0xFFFF8A80),
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (showLaunchConfirm) {
        AlertDialog(
            onDismissRequest = { showLaunchConfirm = false },
            containerColor = Color(0xFF261815),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.85f),
            title = { Text("Открыть Яндекс Еду?") },
            text = {
                Text(
                    "Выбранные блюда продолжим оформлять в приложении Яндекс Еда. " +
                        "Если приложения нет, откроется сайт."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLaunchConfirm = false
                        if (!DeepLinks.yandexEda().openFirstAvailable(context)) {
                            launchError = "Не удалось открыть Яндекс Еду. " +
                                "Установите приложение или браузер и попробуйте снова."
                        }
                    }
                ) {
                    Text("Открыть", color = Color(0xFFFFCC80), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLaunchConfirm = false }) {
                    Text("Отмена", color = Color.White.copy(alpha = 0.75f))
                }
            }
        )
    }
}

/** Подбор реального фото блюда из ImageCategory.FOOD по стабильному id позиции. */
private fun foodImageUrl(itemId: String, urls: List<String>): String? {
    if (urls.isEmpty()) return null
    val index = when (itemId) {
        "f_pepperoni", "f_cheese_chicken", "f_dodster" -> 1
        "f_flat_white", "f_bumble" -> 2
        "f_cherry_pie", "f_croissant" -> 3
        else -> 0
    }
    return urls.getOrNull(index) ?: urls.first()
}

@Composable
private fun ActiveOrderView(
    order: FoodOrder,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF3E2723),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFF7043)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF7043).copy(alpha = 0.2f)
            ) {
                Text(
                    text = order.status.titleRu,
                    color = Color(0xFFFFCC80),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Icon(
                imageVector = Icons.Default.DeliveryDining,
                contentDescription = null,
                tint = Color(0xFFFFAB91),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = order.restaurantName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Состав: ${order.itemsSummary}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x30FFFFFF))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Время доставки:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("~${order.etaMinutes} мин", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Сумма заказа:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("${order.totalPriceRu} ₽ / ${order.totalPriceByn} BYN", color = Color(0xFFFFCC80), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Text("Отменить доставку", fontWeight = FontWeight.Bold)
            }
        }
    }
}
