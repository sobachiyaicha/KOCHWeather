package com.koch.weather.data.model

/**
 * Правило «факта дня» (item 12 фазы 5, D5 с устройства).
 *
 * Сид факта зависит только от города и дня года. Но «город» для факта дня —
 * это постоянный гео-город (текущее место), а не последний открытый город:
 * иначе после перезапуска приложения, который восстанавливает последний
 * просмотренный город, факт менялся бы в течение одного календарного дня.
 *
 * Смена выбранного города факт не меняет; другой факт выдаёт только
 * `MainScreenViewModel.refreshAiFact()`.
 */
object FactOfDayPolicy {

    /**
     * Город-якорь для сида факта: гео-город из списка, иначе первый город,
     * иначе явный fallback. Порядок списка и выбранный город не влияют.
     */
    fun anchorCityName(
        cities: List<CityLocation>,
        fallback: String = DefaultCities.GEO_CITY_NAME
    ): String =
        cities.firstOrNull { DefaultCities.isGeoCity(it) }?.name
            ?: cities.firstOrNull()?.name
            ?: fallback
}
