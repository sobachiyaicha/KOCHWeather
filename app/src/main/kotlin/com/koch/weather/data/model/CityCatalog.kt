package com.koch.weather.data.model

import kotlin.math.abs

/**
 * Правила каталога сохранённых городов (item 6 фазы 5): добавление из поиска
 * и из списка, дедупликация по имени+координатам, лимит списка.
 *
 * Здесь только чистые функции — их покрывают JVM-тесты, ViewModel их вызывает.
 */
object CityCatalog {

    /** Больше 12 городов в списке не держим: «Список» перестаёт быть списком. */
    const val MAX_SAVED_CITIES: Int = 12

    /** Два города считаются одним местом, если точки ближе ~2 км. */
    private const val SAME_PLACE_DEGREES: Double = 0.02

    /** Имя совпало, а координаты разошлись сильнее ~5 км — это разные места. */
    private const val SAME_NAME_DEGREES: Double = 0.05

    /**
     * Совпадает ли место. Дубликат — это либо та же точка (в пределах ~2 км)
     * под любым именем, либо то же имя в пределах ~5 км. Однофамильцы в разных
     * регионах остаются разными городами.
     */
    fun isSamePlace(a: CityLocation, b: CityLocation): Boolean {
        val dLat = abs(a.latitude - b.latitude)
        val dLon = abs(a.longitude - b.longitude)
        if (dLat < SAME_PLACE_DEGREES && dLon < SAME_PLACE_DEGREES) return true
        return a.name.trim().equals(b.name.trim(), ignoreCase = true) &&
            dLat < SAME_NAME_DEGREES && dLon < SAME_NAME_DEGREES
    }

    fun indexOfPlace(cities: List<CityLocation>, city: CityLocation): Int =
        cities.indexOfFirst { isSamePlace(it, city) }

    fun contains(cities: List<CityLocation>, city: CityLocation): Boolean =
        indexOfPlace(cities, city) >= 0

    /** Убирает дубликаты, сохраняя порядок. Флаг гео-города не переизобретаем. */
    fun dedupe(cities: List<CityLocation>): List<CityLocation> {
        val result = mutableListOf<CityLocation>()
        for (city in cities) {
            if (!contains(result, city)) {
                result.add(city)
            }
        }
        return result
    }

    /**
     * План добавления: [city] уже в списке → только выбор существующего;
     * иначе — новый список (с лимитом) и индекс нового города.
     *
     * Флаг [CityLocation.isCurrentLocation] НЕ ставится по имени: город из поиска
     * всегда остаётся не-гео-городом, даже если он называется «Санкт-Петербург».
     * Флаг текущего места есть только у города из каталога дефолтов.
     *
     * Возвращает null, если добавление запрещено лимитом.
     */
    fun planAdd(
        cities: List<CityLocation>,
        city: CityLocation
    ): AddCityPlan? {
        val existingIndex = indexOfPlace(cities, city)
        if (existingIndex >= 0) {
            return AddCityPlan(
                cities = cities,
                selectedIndex = existingIndex,
                isNew = false
            )
        }
        if (cities.size >= MAX_SAVED_CITIES) return null
        val updated = cities + city
        return AddCityPlan(
            cities = updated,
            selectedIndex = updated.lastIndex,
            isNew = true
        )
    }

    /** Нормализация списка из DataStore: дедупликация + актуальные флаги. */
    fun normalizeSaved(cities: List<CityLocation>): List<CityLocation> = dedupe(cities)

    data class AddCityPlan(
        val cities: List<CityLocation>,
        val selectedIndex: Int,
        val isNew: Boolean
    )
}
