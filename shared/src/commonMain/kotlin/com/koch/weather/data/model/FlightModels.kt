package com.koch.weather.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Immutable
@Serializable
data class FlightOffer(
    val id: String,
    val fromCity: String,
    val toCity: String,
    val airline: String,
    val flightNumber: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationText: String,
    val priceRu: Int,
    val weatherAtDestination: String,
    val isDirect: Boolean = true
)

@Immutable
@Serializable
data class FlightBooking(
    val bookingReference: String,
    val flight: FlightOffer,
    val passengerName: String,
    val seat: String,
    val gate: String,
    val isConfirmed: Boolean = true,
    val bookingTimestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Immutable
data class FlightState(
    val isSheetOpen: Boolean = false,
    val offers: List<FlightOffer> = emptyList(),
    val selectedOffer: FlightOffer? = null,
    val selectedSeat: String = "14A",
    val activeBooking: FlightBooking? = null,
    val isBookingInProgress: Boolean = false
)
