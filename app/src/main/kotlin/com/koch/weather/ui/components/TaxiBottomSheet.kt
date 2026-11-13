package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koch.weather.data.model.*

@Composable
fun TaxiBottomSheet(
    taxiState: TaxiState,
    trafficScore: Int,
    trafficAdvice: String,
    onSelectTariff: (TaxiTariffType) -> Unit,
    onSelectDestination: (TaxiDestination) -> Unit,
    onOrderTaxi: () -> Unit,
    onCancelTaxi: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E232A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)),
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
                            color = Color(0xFFFFB300).copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🚖 Такси «Сухой салон»",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Пробки: $trafficScore/10 • $trafficAdvice",
                                fontSize = 11.5.sp,
                                color = Color(0xFFFFD54F)
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

                val activeRide = taxiState.activeRide
                if (activeRide != null && activeRide.status != TaxiRideStatus.COMPLETED) {
                    // Active Ride Card
                    ActiveRideView(
                        ride = activeRide,
                        onCancel = onCancelTaxi
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "destinations_header") {
                            Text(
                                text = "Куда едем?",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        items(DefaultTaxiDestinations.items, key = { it.id }) { dest ->
                            val isSelected = dest.id == taxiState.selectedDestination.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF37474F) else Color(0x15FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                    color = if (isSelected) Color(0xFFFFB300) else Color(0x20FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDestination(dest) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dest.title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text(dest.subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                    Text(
                                        text = "${dest.distanceKm} км",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        item(key = "tariffs_header") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Тариф поездки:",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        items(TaxiTariffType.values(), key = { it.name }) { tariff ->
                            val isSelected = tariff == taxiState.selectedTariff
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF455A64) else Color(0x15FFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                    color = if (isSelected) Color(0xFFFFC107) else Color(0x20FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTariff(tariff) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tariff.iconEmoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tariff.titleRu, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${tariff.carModel} • ~${tariff.etaMinutes} мин",
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${tariff.basePriceRu} ₽",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${tariff.basePriceByn} BYN",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onOrderTaxi,
                        enabled = !taxiState.isOrdering,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color(0xFF1E232A)
                        )
                    ) {
                        Text(
                            text = "Вызвать ${taxiState.selectedTariff.titleRu} за ${taxiState.selectedTariff.basePriceRu} ₽ ›",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRideView(
    ride: TaxiRide,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF263238),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFFB300)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFB300).copy(alpha = 0.2f)
            ) {
                Text(
                    text = ride.status.titleRu,
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(ride.tariff.iconEmoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${ride.carModel} • ${ride.carPlate}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Водитель: ${ride.driverName} (4.9 ★)",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x30FFFFFF))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Пункт назначения:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(ride.destination.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Стоимость поездки:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("${ride.priceRu} ₽ / ${ride.priceByn} BYN", color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Text("Отменить заказ", fontWeight = FontWeight.Bold)
            }
        }
    }
}
