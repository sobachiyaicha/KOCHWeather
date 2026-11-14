package com.koch.weather.data.repository

import com.koch.weather.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID

class FoodRepository {

    private val restaurants = listOf(
        Restaurant(
            id = "rest_vkusno",
            name = "Вкусно — и точка",
            category = "Бургеры, фастфуд, картофель",
            deliveryTimeMinutes = 25,
            rating = 4.8,
            bannerColorHex = 0xFFD32F2F
        ),
        Restaurant(
            id = "rest_dodo",
            name = "Додо Пицца",
            category = "Горячая пицца, закуски, десерты",
            deliveryTimeMinutes = 30,
            rating = 4.9,
            bannerColorHex = 0xFFFF6D00
        ),
        Restaurant(
            id = "rest_surf",
            name = "Surf Coffee",
            category = "Спешелти кофе, выпечка, какао",
            deliveryTimeMinutes = 20,
            rating = 4.9,
            bannerColorHex = 0xFF1976D2
        )
    )

    private val menuItems = listOf(
        // Вкусно — и точка
        FoodItem("f_big_hit", "rest_vkusno", "Биг Хит", "Две сочные котлеты из 100% говядины, сыр, маринованные огурчики", 195, 6.50, 230),
        FoodItem("f_fries", "rest_vkusno", "Картофель Фри (Гранд)", "Хрустящий золотистый картофель с солью", 115, 3.80, 160),
        FoodItem("f_nuggets", "rest_vkusno", "Наггетсы (9 шт)", "Нежное куриное филе в золотистой хрустящей панировке", 149, 4.90, 175),
        FoodItem("f_cherry_pie", "rest_vkusno", "Пирожок Вишневый", "Горячий хрустящий пирожок со сладкой вишней", 89, 2.90, 80),

        // Додо Пицца
        FoodItem("f_pepperoni", "rest_dodo", "Пепперони Фреш (30 см)", "Пикантная пепперони, увеличенная порция моцареллы, томаты", 549, 17.90, 480),
        FoodItem("f_cheese_chicken", "rest_dodo", "Сырный цыпленок (30 см)", "Цыпленок, сырный соус, моцарелла, свежие томаты", 589, 19.20, 510),
        FoodItem("f_dodster", "rest_dodo", "Додстер с моцареллой", "Горячая сытная закуска в тонкой пшеничной лепешке", 219, 7.10, 210),

        // Surf Coffee
        FoodItem("f_flat_white", "rest_surf", "Флэт Уайт (350 мл)", "Двойной эспрессо с бархатистой молочной микропенкой", 270, 8.80, 350),
        FoodItem("f_bumble", "rest_surf", "Бамбл Кофе (400 мл)", "Освежающий микс эспрессо и свежевыжатого апельсинового сока", 350, 11.40, 400),
        FoodItem("f_croissant", "rest_surf", "Круассан с миндалем", "Свежайшая парижская выпечка с миндальным кремом", 220, 7.20, 120)
    )

    fun getRestaurants(): List<Restaurant> = restaurants

    fun getMenuItems(restaurantId: String): List<FoodItem> =
        menuItems.filter { it.restaurantId == restaurantId }

    suspend fun createOrderSimulation(
        restaurant: Restaurant,
        cart: Map<FoodItem, Int>
    ): Flow<FoodOrder> = flow {
        val orderId = "FOOD-" + UUID.randomUUID().toString().take(6).uppercase()
        val summary = cart.entries.joinToString(", ") { "${it.key.name} x${it.value}" }
        val totalRu = cart.entries.sumOf { it.key.priceRu * it.value }
        val totalByn = kotlin.math.round(cart.entries.sumOf { it.key.priceByn * it.value } * 100.0) / 100.0

        // 1. Cooking
        var order = FoodOrder(
            orderId = orderId,
            restaurantName = restaurant.name,
            itemsSummary = summary,
            totalPriceRu = totalRu,
            totalPriceByn = totalByn,
            status = FoodOrderStatus.COOKING,
            etaMinutes = restaurant.deliveryTimeMinutes
        )
        emit(order)
        delay(2500)

        // 2. Delivering
        order = order.copy(
            status = FoodOrderStatus.DELIVERING,
            etaMinutes = 12
        )
        emit(order)
        delay(3500)

        // 3. Delivered
        order = order.copy(
            status = FoodOrderStatus.DELIVERED,
            etaMinutes = 0
        )
        emit(order)
    }
}
