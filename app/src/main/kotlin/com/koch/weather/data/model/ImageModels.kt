package com.koch.weather.data.model

enum class ImageCategory(val titleRu: String) {
    TRAVEL_DUBAI("Дубай и путешествия"),
    IPHONE("iPhone 16 Pro Max"),
    IPHONE_FRONT("iPhone, фронтально с включённым экраном"),
    AD_LAPTOP("Реклама: ноутбук"),
    FOOD("Блюда и рестораны"),
    FOOD_FRIES("Картофель фри"),
    FOOD_NUGGETS("Наггетсы"),
    FOOD_PIROSHKI("Пирожки"),
    FOOD_HOTDOG("Хот-дог"),
    FOOD_COFFEE("Кофе")
}

/**
 * Фото с Wikimedia Commons. URL — прямой файл upload.wikimedia.org без
 * utm-параметров; sourcePageUrl нужен для атрибуции.
 */
data class RemoteImage(
    val id: String,
    val category: ImageCategory,
    val url: String,
    val descriptionRu: String,
    val sourcePageUrl: String,
    val source: String = "Wikimedia Commons"
)

/**
 * Проверенные 2026-11-14 прямые URL (HTTP 200, image/jpeg|png), превью 500 px
 * под слоты 72–180 dp. Wikimedia отклоняет произвольные ширины (480px → HTTP 400),
 * поэтому используется поддерживаемый 500px. При пустом/дефолтном User-Agent
 * Wikimedia отдаёт 429, поэтому Coil должен выставлять WIKIMEDIA_USER_AGENT.
 */
object ImageCatalog {

    const val WIKIMEDIA_USER_AGENT = "KOCHWeather/1.0 (Android; contact: dev@koch.example)"

    private const val UPLOAD = "https://upload.wikimedia.org/wikipedia/commons/thumb"
    private const val COMMONS_FILE = "https://commons.wikimedia.org/wiki/File:"

    val images: Map<ImageCategory, List<RemoteImage>> = mapOf(
        ImageCategory.TRAVEL_DUBAI to listOf(
            RemoteImage(
                id = "dubai_skyline_2016",
                category = ImageCategory.TRAVEL_DUBAI,
                url = "$UPLOAD/1/10/Dubai_Skyline_2016.jpg/500px-Dubai_Skyline_2016.jpg",
                descriptionRu = "Панорама Дубая, 2016",
                sourcePageUrl = COMMONS_FILE + "Dubai_Skyline_2016.jpg"
            ),
            RemoteImage(
                id = "dubai_marina_skyline",
                category = ImageCategory.TRAVEL_DUBAI,
                url = "$UPLOAD/e/e6/Dubai_Marina_Skyline.jpg/500px-Dubai_Marina_Skyline.jpg",
                descriptionRu = "Дубай Марина, небоскрёбы у воды",
                sourcePageUrl = COMMONS_FILE + "Dubai_Marina_Skyline.jpg"
            ),
            RemoteImage(
                id = "dubai_beach",
                category = ImageCategory.TRAVEL_DUBAI,
                url = "$UPLOAD/9/96/Beach_in_Dubai.jpg/500px-Beach_in_Dubai.jpg",
                descriptionRu = "Пляж в Дубае",
                sourcePageUrl = COMMONS_FILE + "Beach_in_Dubai.jpg"
            )
        ),
        ImageCategory.IPHONE to listOf(
            RemoteImage(
                id = "iphone_16_pro_max_desert_titanium",
                category = ImageCategory.IPHONE,
                url = "$UPLOAD/e/e2/IPhone_16_Pro_Max_Desert_Titanium_Rear.png/500px-IPhone_16_Pro_Max_Desert_Titanium_Rear.png",
                descriptionRu = "iPhone 16 Pro Max, Desert Titanium",
                sourcePageUrl = COMMONS_FILE + "IPhone_16_Pro_Max_Desert_Titanium_Rear.png"
            ),
            RemoteImage(
                id = "iphone_16_pro_series",
                category = ImageCategory.IPHONE,
                url = "$UPLOAD/f/f8/IPhone_16_Pro_series.jpg/500px-IPhone_16_Pro_series.jpg",
                descriptionRu = "Серия iPhone 16 Pro",
                sourcePageUrl = COMMONS_FILE + "IPhone_16_Pro_series.jpg"
            )
        ),
        ImageCategory.IPHONE_FRONT to listOf(
            RemoteImage(
                id = "iphone_14_pro_max_front_screen_on",
                category = ImageCategory.IPHONE_FRONT,
                url = "$UPLOAD/0/00/Deep_Purple_iPhone_14_Pro_Max_front_photo.jpg/500px-Deep_Purple_iPhone_14_Pro_Max_front_photo.jpg",
                descriptionRu = "iPhone 14 Pro Max, фронтальная сторона, экран включён",
                sourcePageUrl = COMMONS_FILE + "Deep_Purple_iPhone_14_Pro_Max_front_photo.jpg"
            )
        ),
        ImageCategory.AD_LAPTOP to listOf(
            RemoteImage(
                id = "macbook_pro_on_desk",
                category = ImageCategory.AD_LAPTOP,
                url = "$UPLOAD/1/11/Macbook_Pro_On_Desk_%2828217827677%29.jpg/500px-Macbook_Pro_On_Desk_%2828217827677%29.jpg",
                descriptionRu = "Рекламный ноутбук на рабочем столе",
                sourcePageUrl = COMMONS_FILE + "Macbook_Pro_On_Desk_(28217827677).jpg"
            ),
            RemoteImage(
                id = "laptop_on_a_desk",
                category = ImageCategory.AD_LAPTOP,
                url = "$UPLOAD/7/79/Laptop_on_a_desk_%28Unsplash%29.jpg/500px-Laptop_on_a_desk_%28Unsplash%29.jpg",
                descriptionRu = "Ноутбук на столе, рабочая станция",
                sourcePageUrl = COMMONS_FILE + "Laptop_on_a_desk_(Unsplash).jpg"
            )
        ),
        ImageCategory.FOOD to listOf(
            RemoteImage(
                id = "big_mac_hamburger",
                category = ImageCategory.FOOD,
                url = "$UPLOAD/9/9a/Big_Mac_hamburger.jpg/500px-Big_Mac_hamburger.jpg",
                descriptionRu = "Бургер",
                sourcePageUrl = COMMONS_FILE + "Big_Mac_hamburger.jpg"
            ),
            RemoteImage(
                id = "pepperoni_pizza_greggs",
                category = ImageCategory.FOOD,
                url = "$UPLOAD/d/d9/Pepperoni_Pizza_-_Greggs_2024-03-16.jpg/500px-Pepperoni_Pizza_-_Greggs_2024-03-16.jpg",
                descriptionRu = "Пепперони пицца",
                sourcePageUrl = COMMONS_FILE + "Pepperoni_Pizza_-_Greggs_2024-03-16.jpg"
            ),
            RemoteImage(
                id = "cappuccino_cup",
                category = ImageCategory.FOOD,
                url = "$UPLOAD/1/1e/A-cup-of-cappuccino-coffee-dar-es-salaam-cafe.jpg/500px-A-cup-of-cappuccino-coffee-dar-es-salaam-cafe.jpg",
                descriptionRu = "Чашка капучино",
                sourcePageUrl = COMMONS_FILE + "A-cup-of-cappuccino-coffee-dar-es-salaam-cafe.jpg"
            ),
            RemoteImage(
                id = "almond_croissant",
                category = ImageCategory.FOOD,
                url = "$UPLOAD/2/2e/Almond_croissant_-_Bread_%26_Milk_2024-06-27.jpg/500px-Almond_croissant_-_Bread_%26_Milk_2024-06-27.jpg",
                descriptionRu = "Миндальный круассан",
                sourcePageUrl = COMMONS_FILE + "Almond_croissant_-_Bread_%26_Milk_2024-06-27.jpg"
            )
        ),
        ImageCategory.FOOD_FRIES to listOf(
            RemoteImage(
                id = "french_fries_truffle_oil",
                category = ImageCategory.FOOD_FRIES,
                url = "$UPLOAD/8/8e/Truffle_oil_french_fries_%2833024792848%29.jpg/500px-Truffle_oil_french_fries_%2833024792848%29.jpg",
                descriptionRu = "Картофель фри с зеленью",
                sourcePageUrl = COMMONS_FILE + "Truffle_oil_french_fries_(33024792848).jpg"
            )
        ),
        ImageCategory.FOOD_NUGGETS to listOf(
            RemoteImage(
                id = "chicken_nuggets_on_a_plate",
                category = ImageCategory.FOOD_NUGGETS,
                url = "$UPLOAD/6/65/Chicken_nuggets_on_a_plate.jpg/500px-Chicken_nuggets_on_a_plate.jpg",
                descriptionRu = "Куриные наггетсы на тарелке",
                sourcePageUrl = COMMONS_FILE + "Chicken_nuggets_on_a_plate.jpg"
            )
        ),
        ImageCategory.FOOD_PIROSHKI to listOf(
            RemoteImage(
                id = "pirozhki_03",
                category = ImageCategory.FOOD_PIROSHKI,
                url = "$UPLOAD/d/df/%D0%9F%D0%B8%D1%80%D0%BE%D0%B6%D0%BA%D0%B8_03.jpg/500px-%D0%9F%D0%B8%D1%80%D0%BE%D0%B6%D0%BA%D0%B8_03.jpg",
                descriptionRu = "Русские пирожки с начинкой",
                sourcePageUrl = COMMONS_FILE + "%D0%9F%D0%B8%D1%80%D0%BE%D0%B6%D0%BA%D0%B8_03.jpg"
            )
        ),
        ImageCategory.FOOD_HOTDOG to listOf(
            RemoteImage(
                id = "hot_dog_09",
                category = ImageCategory.FOOD_HOTDOG,
                url = "$UPLOAD/3/3a/Hot_Dog_09.jpg/500px-Hot_Dog_09.jpg",
                descriptionRu = "Хот-дог с кетчупом и горчицей",
                sourcePageUrl = COMMONS_FILE + "Hot_Dog_09.jpg"
            )
        ),
        ImageCategory.FOOD_COFFEE to listOf(
            RemoteImage(
                id = "iced_coffee_2024",
                category = ImageCategory.FOOD_COFFEE,
                url = "$UPLOAD/f/f1/Iced_Coffee_20240730-135859.jpg/500px-Iced_Coffee_20240730-135859.jpg",
                descriptionRu = "Айс-кофе со взбитыми сливками",
                sourcePageUrl = COMMONS_FILE + "Iced_Coffee_20240730-135859.jpg"
            )
        )
    )

    fun urls(category: ImageCategory): List<String> =
        images[category].orEmpty().map { it.url }

    fun urlsByCategory(): Map<ImageCategory, List<String>> =
        ImageCategory.values().associateWith { urls(it) }
}
