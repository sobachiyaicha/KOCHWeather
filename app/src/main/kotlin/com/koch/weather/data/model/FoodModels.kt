package com.koch.weather.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val category: String,
    val deliveryTimeMinutes: Int,
    val rating: Double,
    val bannerColorHex: Long
)

@Serializable
data class FoodItem(
    val id: String,
    val restaurantId: String,
    val name: String,
    val description: String,
    val priceRu: Int,
    val priceByn: Double,
    val weightGrams: Int
)

enum class FoodOrderStatus(val titleRu: String) {
    IDLE("Нет активного заказа"),
    COOKING("Готовится на кухне ресторана"),
    DELIVERING("Курьер спешит к вам сквозь непогоду"),
    DELIVERED("Заказ доставлен! Приятного аппетита")
}

@Serializable
data class FoodOrder(
    val orderId: String,
    val restaurantName: String,
    val itemsSummary: String,
    val totalPriceRu: Int,
    val totalPriceByn: Double,
    val status: FoodOrderStatus,
    val etaMinutes: Int
)

data class FoodState(
    val isSheetOpen: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val selectedRestaurant: Restaurant? = null,
    val menuItems: List<FoodItem> = emptyList(),
    val cart: Map<FoodItem, Int> = emptyMap(),
    val activeOrder: FoodOrder? = null,
    val isOrdering: Boolean = false
) {
    val cartCount: Int get() = cart.values.sum()
    val totalCartPriceRu: Int get() = cart.entries.sumOf { it.key.priceRu * it.value }
    val totalCartPriceByn: Double get() = cart.entries.sumOf { it.key.priceByn * it.value }
}
