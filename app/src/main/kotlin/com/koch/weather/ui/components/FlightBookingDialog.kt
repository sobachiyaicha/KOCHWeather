package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.FlightBooking
import com.koch.weather.data.model.FlightOffer
import com.koch.weather.data.model.FlightState
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochChip
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.KochListRow
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSection
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.formatRuAmount
import com.koch.weather.ui.theme.KochHeroScrim
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Авиабилеты. Bottom-sheet с фото-героем (refs/premium_ads.png, 1:57 — фото как герой карточки),
 * офферы строками без обрезки, капсула CTA и экран подтверждения.
 */
@Composable
fun FlightBookingDialog(
    flightState: FlightState,
    onSelectOffer: (FlightOffer) -> Unit,
    onSelectSeat: (String) -> Unit,
    onBookFlight: (String) -> Unit,
    onDismiss: () -> Unit,
    heroImageUrl: String? = null
) {
    val colors = LocalKochColors.current
    val passengerName = remember { "Пассажир Погоды" }
    val seats = remember {
        listOf("14A" to "у окна", "14B" to "центр", "14C" to "проход", "12F" to "у окна")
    }
    var showSeats by remember { mutableStateOf(false) }

    val activeBooking = flightState.activeBooking

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.9f,
        hero = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KochSize.hero)
            ) {
                RemotePhoto(
                    url = heroImageUrl,
                    contentDescription = "Дубай",
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    placeholderIcon = Icons.Default.Flight,
                    placeholderTint = colors.textSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KochHeroScrim)
                )
                Text(
                    text = "Авиабилеты в тепло",
                    style = KochType.titleCard,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(KochSpace.xl)
                )
                KochCloseButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KochSpace.s)
                )
            }
        },
        content = {
            Text(
                text = "Спасайтесь от холода и слякоти",
                style = KochType.sub,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = KochSpace.m)
            )

            if (activeBooking != null) {
                BoardingPassCard(
                    booking = activeBooking,
                    modifier = Modifier.padding(top = KochSpace.m)
                )
            } else {
                KochSection(title = "Доступные рейсы сегодня")
                Column(modifier = Modifier.fillMaxWidth()) {
                    flightState.offers.forEach { offer ->
                        FlightOfferRow(
                            offer = offer,
                            isSelected = offer.id == flightState.selectedOffer?.id,
                            onClick = { onSelectOffer(offer) }
                        )
                    }
                }

                KochSection(title = "Место в салоне")
                if (showSeats) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(KochSpace.s),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(seats) { seat ->
                            KochChip(
                                text = seat.first,
                                selected = flightState.selectedSeat == seat.first,
                                onClick = {
                                    onSelectSeat(seat.first)
                                    showSeats = false
                                }
                            )
                        }
                    }
                } else {
                    KochChip(
                        text = "Место ${flightState.selectedSeat} · выбрать",
                        onClick = { showSeats = true }
                    )
                }
                // Нижний зазор: последняя карточка/чипы не прилипают к CTA.
                Spacer(Modifier.height(KochSpace.m))
            }
        },
        footer = {
            if (activeBooking != null) {
                KochPrimaryButton(
                    text = "Отлично, летим в тепло!",
                    onClick = onDismiss
                )
            } else {
                val price = flightState.selectedOffer?.priceRu
                KochPrimaryButton(
                    text = if (price != null) {
                        "Забронировать за ${formatRuAmount(price)} ₽"
                    } else {
                        "Выберите рейс"
                    },
                    enabled = !flightState.isBookingInProgress && price != null,
                    loading = flightState.isBookingInProgress,
                    onClick = { onBookFlight(passengerName) }
                )
            }
        }
    )
}

@Composable
private fun FlightOfferRow(
    offer: FlightOffer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    KochListRow(
        title = "${offer.fromCity} → ${offer.toCity}",
        subtitle = buildString {
            append(offer.airline)
            append(" · Рейс ")
            append(offer.flightNumber)
            append("\n")
            append(offer.departureTime)
            append(" — ")
            append(offer.arrivalTime)
            append(" · ")
            append(offer.durationText)
            append("\n")
            append(offer.weatherAtDestination)
        },
        titleMaxLines = 2,
        subtitleMaxLines = 3,
        minHeight = 84.dp,
        selected = isSelected,
        showDivider = true,
        onClick = onClick,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatRuAmount(offer.priceRu)} ₽",
                    style = KochType.numberPrice,
                    color = colors.textPrimary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    )
}

@Composable
private fun BoardingPassCard(
    booking: FlightBooking,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current
    KochCard(modifier = modifier, material = KochMaterial.Solid) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colors.accentSuccess,
                modifier = Modifier.padding(end = KochSpace.s).size(16.dp)
            )
            Text(
                text = "БИЛЕТ ПОДТВЕРЖДЕН!",
                style = KochType.microCaps,
                color = colors.accentSuccess
            )
        }
        Spacer(Modifier.height(KochSpace.m))
        Text(
            text = "${booking.flight.fromCity} → ${booking.flight.toCity}",
            style = KochType.titleCard,
            color = colors.textPrimary
        )
        Text(
            text = "Бронь №: ${booking.bookingReference}",
            style = KochType.caption,
            color = colors.accentPromo
        )
        Spacer(Modifier.height(KochSpace.m))
        HorizontalDivider(color = colors.textPrimary.copy(alpha = 0.12f))
        Spacer(Modifier.height(KochSpace.m))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BoardingPassCell("Место", booking.seat)
            BoardingPassCell("Гейт", booking.gate)
            BoardingPassCell("Рейс", booking.flight.flightNumber)
            BoardingPassCell("Вылет", booking.flight.departureTime)
        }
        Spacer(Modifier.height(KochSpace.l))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color.White, RoundedCornerShape(KochRadius.badge))
                .padding(horizontal = KochSpace.m, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(26) { index ->
                val barWidth = if (index % 3 == 0) 3.dp else if (index % 2 == 0) 1.5.dp else 2.dp
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(barWidth)
                        .background(Color.Black)
                )
            }
        }
    }
}

@Composable
private fun BoardingPassCell(label: String, value: String) {
    val colors = LocalKochColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = KochType.caption,
            color = colors.textTertiary
        )
        Text(
            text = value,
            style = KochType.numberRow,
            color = colors.textPrimary,
            maxLines = 1
        )
    }
}
