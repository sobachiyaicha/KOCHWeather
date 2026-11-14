package com.koch.weather.data.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetNoResponse(
    val properties: MetNoProperties? = null
)

@Serializable
data class MetNoProperties(
    val meta: MetNoMeta? = null,
    val timeseries: List<MetNoTimeseries> = emptyList()
)

@Serializable
data class MetNoMeta(
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class MetNoTimeseries(
    val time: String = "",
    val data: MetNoData = MetNoData()
)

@Serializable
data class MetNoData(
    val instant: MetNoInstant? = null,
    @SerialName("next_1_hours") val next1Hours: MetNoNext? = null,
    @SerialName("next_6_hours") val next6Hours: MetNoNext? = null,
    @SerialName("next_12_hours") val next12Hours: MetNoNext? = null
)

@Serializable
data class MetNoInstant(
    val details: MetNoInstantDetails = MetNoInstantDetails()
)

@Serializable
data class MetNoInstantDetails(
    @SerialName("air_temperature") val airTemperature: Double? = null,
    @SerialName("relative_humidity") val relativeHumidity: Double? = null,
    @SerialName("wind_speed") val windSpeed: Double? = null,
    @SerialName("cloud_area_fraction") val cloudAreaFraction: Double? = null,
    @SerialName("air_pressure_at_sea_level") val airPressureAtSeaLevel: Double? = null
)

@Serializable
data class MetNoNext(
    val summary: MetNoSummary? = null,
    val details: MetNoNextDetails? = null
)

@Serializable
data class MetNoSummary(
    @SerialName("symbol_code") val symbolCode: String? = null
)

@Serializable
data class MetNoNextDetails(
    @SerialName("precipitation_amount") val precipitationAmount: Double? = null
)
