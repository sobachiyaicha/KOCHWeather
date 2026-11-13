package com.koch.weather.data.repository

import com.koch.weather.data.model.FlightBooking
import com.koch.weather.data.model.FlightOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

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
            priceByn = 1120.0,
            weatherAtDestination = "☀️ Солнечно, +31°C, море 28°C",
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
            priceByn = 899.0,
            weatherAtDestination = "☀️ Солнечно, +31°C, штиль",
            isDirect = true
        ),
        FlightOffer(
            id = "fl_minsk_msq",
            fromCity = "Санкт-Петербург (LED)",
            toCity = "Минск (MSQ)",
            airline = "Белавиа (Belavia)",
            flightNumber = "B2-946",
            departureTime = "19:20",
            arrivalTime = "20:45",
            durationText = "1 ч 25 мин",
            priceRu = 5400,
            priceByn = 175.0,
            weatherAtDestination = "⛅ Облачно, +21°C",
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
            priceByn = 125.0,
            weatherAtDestination = "🌤 Переменная облачность, +7°C",
            isDirect = true
        )
    )

    fun getFlightOffers(fromCity: String = ""): List<FlightOffer> {
        return defaultOffers
    }

    fun getAvailableSeats(): List<String> {
        return listOf("12A", "12B", "14A", "14F", "15C", "18D", "21A", "22F")
    }

    suspend fun bookFlight(
        offer: FlightOffer,
        seat: String,
        passengerName: String
    ): FlightBooking = withContext(Dispatchers.IO) {
        val ref = "TKT-" + UUID.randomUUID().toString().take(7).uppercase()
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
