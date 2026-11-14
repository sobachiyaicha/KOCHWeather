package com.koch.weather.data.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SevenTimerResponse(
    val product: String = "",
    val init: String = "",
    val dataseries: List<SevenTimerPoint> = emptyList()
)

@Serializable
data class SevenTimerPoint(
    @SerialName("timepoint") val timepoint: Int = 0,
    @SerialName("temp2m") val temp2m: Int? = null,
    val rh2m: String? = null,
    val wind10m: SevenTimerWind? = null,
    val weather: String? = null
)

@Serializable
data class SevenTimerWind(
    val direction: String? = null,
    val speed: Int? = null
)
