package com.koch.weather.data.model

/**
 * Города по умолчанию (item 11 фазы 5): Санкт-Петербург — гео-город и первый
 * в списке, далее Москва, Сочи и Дубай. Никаких других стран в дефолтах нет.
 *
 * Список живёт в data.model, чтобы тесты проверяли его без Android Context
 * (WeatherRepository.defaultCities ссылается сюда же).
 */
object DefaultCities {

    /** Имя гео-города: только он имеет право на подпись «ТЕКУЩЕЕ МЕСТО». */
    const val GEO_CITY_NAME: String = "Санкт-Петербург"

    val cities: List<CityLocation> = listOf(
        CityLocation(
            name = "Санкт-Петербург",
            latitude = 59.9386,
            longitude = 30.3141,
            country = "Россия",
            region = "Санкт-Петербург",
            isCurrentLocation = true
        ),
        CityLocation(
            name = "Москва",
            latitude = 55.7558,
            longitude = 37.6173,
            country = "Россия",
            region = "Москва"
        ),
        CityLocation(
            name = "Сочи",
            latitude = 43.5855,
            longitude = 39.7231,
            country = "Россия",
            region = "Краснодарский край"
        ),
        CityLocation(
            name = "Дубай",
            latitude = 25.2048,
            longitude = 55.2708,
            country = "ОАЭ",
            region = "Дубай"
        )
    )

    /** Гео-город по умолчанию: первый элемент каталога с флагом текущего места. */
    val geoCity: CityLocation get() = cities.first()

    fun isGeoCity(city: CityLocation?): Boolean = city?.isCurrentLocation == true

    /** Город с именем гео-города, но без флага (из поиска) НЕ считается текущим местом. */
    fun isGeoCityName(name: String?): Boolean =
        name != null && name.trim().equals(GEO_CITY_NAME, ignoreCase = true)
}
