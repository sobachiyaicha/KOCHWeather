package com.koch.weather.ui.components

import com.koch.weather.data.model.CityLocation

/**
 * Строка местоположения для результатов поиска и списка городов:
 * «Московская область, Россия»; если региона нет — только страна.
 */
internal fun CityLocation.locationLine(): String {
    val parts = listOfNotNull(
        region?.trim()?.takeIf { it.isNotEmpty() },
        country.trim().takeIf { it.isNotEmpty() }
    )
    return parts.joinToString(", ")
}
