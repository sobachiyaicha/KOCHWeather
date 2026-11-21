package com.koch.weather.data.repository

import com.koch.weather.data.model.FlightBooking
import com.koch.weather.data.model.FlightOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FlightRepository {

    private val defaultOffers = listOf(
        FlightOffer(
            id = "fl_dubai_dxb",
            fromCity = "Санкт-Петербург (LED)",
            toCity = "Дубай (DXB)",
            airline = "Emirates Airlines",
            flightNumber = "EK-176",
            departureTime = "16:45",
            arrivalTime = "23:15",
            durationText = "5 ч 30 мин",
            priceRu = 34500,
            weatherAtDestination = "Солнечно, море тёплое",
            isDirect = true
        ),
        FlightOffer(
            id = "fl_dubai_fly",
            fromCity = "Москва (VKO)",
            toCity = "Дубай (DWC)",
            airline = "FlyDubai",
            flightNumber = "FZ-978",
            departureTime = "02:10",
            arrivalTime = "08:35",
            durationText = "5 ч 25 мин",
            priceRu = 27900,
            weatherAtDestination = "Солнечно, штиль",
            isDirect = true
        ),
        FlightOffer(
            id = "fl_sochi_aer",
            fromCity = "Санкт-Петербург (LED)",
            toCity = "Сочи (AER)",
            airline = "Россия",
            flightNumber = "FV-6543",
            departureTime = "09:40",
            arrivalTime = "12:20",
            durationText = "2 ч 40 мин",
            priceRu = 6900,
            weatherAtDestination = "Солнечно, +26",
            isDirect = true
        ),
        FlightOffer(
            id = "fl_moscow_svo",
            fromCity = "Санкт-Петербург (LED)",
            toCity = "Москва (SVO)",
            airline = "Аэрофлот (Aeroflot)",
            flightNumber = "SU-023",
            departureTime = "11:00",
            arrivalTime = "12:30",
            durationText = "1 ч 30 мин",
            priceRu = 3800,
            weatherAtDestination = "Переменная облачность",
            isDirect = true
        )
    )

    fun getFlightOffers(fromCity: String = ""): List<FlightOffer> {
        return defaultOffers
    }

    fun getAvailableSeats(): List<String> {
        return listOf("12A", "12B", "14A", "14F", "15C", "18D", "21A", "22F")
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun bookFlight(
        offer: FlightOffer,
        seat: String,
        passengerName: String
    ): FlightBooking = withContext(Dispatchers.Default) {
        // java.util.UUID.randomUUID() → kotlin.uuid.Uuid.random(): тот же
        // каноничный формат 8-4-4-4-12 в нижнем регистре — текст брони 1:1.
        val ref = "TKT-" + Uuid.random().toString().take(7).uppercase()
        val gate = "G" + (10..34).random()
        FlightBooking(
            bookingReference = ref,
            flight = offer,
            passengerName = passengerName.ifBlank { "Пассажир 2025" },
            seat = seat,
            gate = gate,
            isConfirmed = true
        )
    }
}
