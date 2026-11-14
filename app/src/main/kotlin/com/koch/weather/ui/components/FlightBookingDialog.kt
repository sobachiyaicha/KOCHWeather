package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koch.weather.data.model.FlightBooking
import com.koch.weather.data.model.FlightOffer
import com.koch.weather.data.model.FlightState

@Composable
fun FlightBookingDialog(
    flightState: FlightState,
    onSelectOffer: (FlightOffer) -> Unit,
    onSelectSeat: (String) -> Unit,
    onBookFlight: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passengerName by remember { mutableStateOf("Пассажир Погоды") }
    val seats = remember { listOf("14A (у окна)", "14B (центр)", "14C (проход)", "12F (у окна)", "3A (бизнес)") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141E30),
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E88E5).copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Flight,
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Авиабилеты в тепло",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Спасайтесь от холода и слякоти",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // If booking confirmed
                val activeBooking = flightState.activeBooking
                if (activeBooking != null) {
                    BoardingPassCard(
                        booking = activeBooking,
                        onDismiss = onDismiss
                    )
                } else {
                    // Flight list & booking form
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        item(key = "offers_header") {
                            Text(
                                text = "Доступные рейсы сегодня:",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        items(flightState.offers, key = { it.id }) { offer ->
                            val isSelected = offer.id == flightState.selectedOffer?.id
                            FlightOfferCard(
                                offer = offer,
                                isSelected = isSelected,
                                onClick = { onSelectOffer(offer) }
                            )
                        }

                        item(key = "seat_selector") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Выберите место в салоне:",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(seats) { seat ->
                                    val isSeatSelected = seat.startsWith(flightState.selectedSeat) || seat == flightState.selectedSeat
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSeatSelected) Color(0xFF1976D2) else Color(0x25FFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (isSeatSelected) Color(0xFF90CAF9) else Color(0x30FFFFFF)
                                        ),
                                        modifier = Modifier.clickable { onSelectSeat(seat.substringBefore(" ")) }
                                    ) {
                                        Text(
                                            text = seat,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSeatSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Booking button
                    Button(
                        onClick = { onBookFlight(passengerName) },
                        enabled = !flightState.isBookingInProgress && flightState.selectedOffer != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E88E5),
                            contentColor = Color.White
                        )
                    ) {
                        if (flightState.isBookingInProgress) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Бронирование билета...", fontSize = 14.sp)
                        } else {
                            val price = flightState.selectedOffer?.priceRu
                            Text(
                                text = if (price != null) {
                                    "Забронировать за $price ₽ ›"
                                } else {
                                    "Выберите рейс"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightOfferCard(
    offer: FlightOffer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF1B3B6F) else Color(0x1FFFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) Color(0xFF64B5F6) else Color(0x25FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    // The route must never be clipped: the destination city is the whole point
                    // of the offer (device audit_06 showed an arrow with no city).
                    text = "${offer.fromCity} → ${offer.toCity}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${offer.priceRu} ₽",
                    color = Color(0xFFFFD54F),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.wrapContentWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${offer.airline} • Рейс ${offer.flightNumber}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${offer.priceByn} BYN",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.5.sp,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.wrapContentWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${offer.departureTime} — ${offer.arrivalTime} (${offer.durationText})",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x35FFB300)
                ) {
                    Text(
                        text = offer.weatherAtDestination,
                        color = Color(0xFFFFE082),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardingPassCard(
    booking: FlightBooking,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F3057),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4FC3F7)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x354CAF50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "БИЛЕТ ПОДТВЕРЖДЕН!",
                        color = Color(0xFFC8E6C9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${booking.flight.fromCity} → ${booking.flight.toCity}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Бронь №: ${booking.bookingReference}",
                color = Color(0xFFFFD54F),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x30FFFFFF))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Место", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(booking.seat, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Гейт", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(booking.gate, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Рейс", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(booking.flight.flightNumber, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Вылет", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(booking.flight.departureTime, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barcode graphic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(24) { index ->
                    val barWidth = if (index % 3 == 0) 3.dp else if (index % 2 == 0) 1.5.dp else 2.dp
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(barWidth)
                            .background(Color.Black)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
            ) {
                Text("Отлично, летим в тепло!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
