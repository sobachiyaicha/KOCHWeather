package com.koch.weather.data.model

/**
 * Восстановление выбранного города при старте (D-2 feature-qa).
 *
 * Пользователь выбирает город на главной/свайпом/из списка — этот выбор
 * сохраняется отдельно от «последнего прогноза» (`last_city_json`, который
 * пишется при каждом сохранении кэша погоды). При старте:
 *
 * 1. сохранённый выбранный город, если он ещё есть в актуальном списке
 *    (сравнение по координатам, [CityCatalog.isSamePlace]);
 * 2. если сохранённого выбора ещё нет (старая установка) — прежнее поведение:
 *    «последний прогноз», если он есть в списке;
 * 3. иначе — гео-город («текущее место»), а не произвольный город.
 *
 * Город, которого больше нет в списке, не возвращается: падаем на гео-город.
 */
object SelectedCityRestorePolicy {

    fun resolve(
        savedSelected: CityLocation?,
        legacyLastCity: CityLocation?,
        cities: List<CityLocation>
    ): CityLocation? {
        if (savedSelected != null) {
            // Сохранённый выбор главнее: если города нет в списке — только гео.
            return cities.firstOrNull { CityCatalog.isSamePlace(it, savedSelected) }
                ?: geoCity(cities)
                ?: savedSelected
        }
        val legacy = legacyLastCity
            ?.let { last -> cities.firstOrNull { CityCatalog.isSamePlace(it, last) } }
        return legacy ?: geoCity(cities) ?: cities.firstOrNull()
    }

    private fun geoCity(cities: List<CityLocation>): CityLocation? =
        cities.firstOrNull { DefaultCities.isGeoCity(it) } ?: cities.firstOrNull()
}
