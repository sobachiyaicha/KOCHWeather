package com.koch.weather.data.model

import androidx.compose.runtime.Immutable

/** Куда ведёт CTA рекламного креатива. UI маршрутизирует, данные не знают про экраны. */
enum class AdAction {
    PAYWALL,
    LOOTBOX,
    UMBRELLA_SHOP,
    FOOD
}

/**
 * Рекламный креатив (Реклама — item 9 фазы 5).
 *
 * Все тексты — на русском, без эмодзи; [disclaimerRu] обязателен и показывается
 * мелким шрифтом под CTA. [imageCategory] == null означает «фото нет, рисуем иконку».
 */
@Immutable
data class AdCreative(
    val id: String,
    val brandRu: String,
    val headline: String,
    val body: String,
    val ctaLabel: String,
    val action: AdAction,
    val imageCategory: ImageCategory?,
    val disclaimerRu: String
)

/**
 * Каталог рекламы (4 креатива). Ротация детерминированная:
 * `creatives[(dayOfYear + openCount) % size]` — «всегда одна и та же реклама»
 * больше не воспроизводится, но и случайности без данных нет.
 */
object AdCatalog {

    val creatives: List<AdCreative> = listOf(
        AdCreative(
            id = "ad_ai_course",
            brandRu = "НейроШкола",
            headline = "Дизайнеры больше не нужны",
            body = "Научим собирать сайты с помощью нейросетей за 15 минут в день. " +
                "Первое занятие бесплатно, второе — уже нет.",
            ctaLabel = "Смотреть премиум-условия",
            action = AdAction.PAYWALL,
            imageCategory = ImageCategory.AD_LAPTOP,
            disclaimerRu = "Реклама. Курс не связан с разработчиком приложения."
        ),
        AdCreative(
            id = "ad_iphone_lootbox",
            brandRu = "Погодные лутбоксы",
            headline = "iPhone 16 Pro Max",
            body = "Шанс выиграть 0.001 %. Кто-то же должен выиграть — почему не вы? " +
                "Остальным достанется дождь на завтра.",
            ctaLabel = "Открыть лутбоксы",
            action = AdAction.LOOTBOX,
            imageCategory = ImageCategory.IPHONE,
            disclaimerRu = "Реклама. Розыгрыш развлекательный, приз не выдаётся."
        ),
        AdCreative(
            id = "ad_umbrella_shop",
            brandRu = "Магазин зонтов",
            headline = "Зонтики от 649 ₽",
            body = "Автоматические зонты с ветрозащитой до 15 м/с. " +
                "Доставка курьером за 30 минут, пока дождь ещё идёт.",
            ctaLabel = "В магазин зонтов",
            action = AdAction.UMBRELLA_SHOP,
            imageCategory = null,
            disclaimerRu = "Реклама. Магазин работает внутри приложения."
        ),
        AdCreative(
            id = "ad_yandex_food",
            brandRu = "Яндекс Еда",
            headline = "Бургер за 30 минут",
            body = "Курьер приедет даже в ливень. Первый заказ по промокоду ПИТЕР — " +
                "доставка бесплатно.",
            ctaLabel = "Открыть Еду",
            action = AdAction.FOOD,
            imageCategory = ImageCategory.FOOD,
            disclaimerRu = "Реклама. Сервис не связан с разработчиком приложения."
        )
    )

    /**
     * Детерминированный выбор креатива. Один и тот же день + число открытий
     * дают один и тот же креатив (без Random и без пересборки на рекомпозиции).
     */
    fun creativeFor(dayOfYear: Int, openCount: Int = 0): AdCreative {
        val index = (dayOfYear + openCount).mod(creatives.size)
        return creatives[index]
    }
}
