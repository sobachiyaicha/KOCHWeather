package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.DefaultTaxiDestinations
import com.koch.weather.data.model.TaxiDestination
import com.koch.weather.data.model.TaxiRide
import com.koch.weather.data.model.TaxiRideStatus
import com.koch.weather.data.model.TaxiState
import com.koch.weather.data.model.TaxiTariffType
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.KochConfirmSheet
import com.koch.weather.ui.design.KochListRow
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSection
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.design.formatByn
import com.koch.weather.ui.design.formatRuAmount
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Такси. Bottom-sheet по языку референса (refs/cards_taxi_food.png, 2:05):
 * плотные строки, один синий акцент, капсула CTA; deep link в Яндекс Go не меняется.
 */
@Composable
fun TaxiBottomSheet(
    taxiState: TaxiState,
    trafficScore: Int?,
    trafficAdvice: String,
    city: CityLocation?,
    onSelectTariff: (TaxiTariffType) -> Unit,
    onSelectDestination: (TaxiDestination) -> Unit,
    onCancelTaxi: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLaunchConfirm by remember { mutableStateOf(false) }
    var launchError by remember { mutableStateOf<String?>(null) }

    val activeRide = taxiState.activeRide
        ?.takeIf { it.status != TaxiRideStatus.COMPLETED }
    val hasRide = activeRide != null

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.86f,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = KochSpace.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = colors.accentPromo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(KochSpace.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Такси «Сухой салон»",
                        style = KochType.titleCard,
                        color = colors.textPrimary
                    )
                    Text(
                        // Честные данные: без ключа пробок цифры нет — только текст.
                        text = if (trafficScore != null) {
                            "Пробки: $trafficScore/10 · $trafficAdvice"
                        } else {
                            trafficAdvice
                        },
                        style = KochType.sub,
                        color = if (trafficScore != null) colors.accentPromo else colors.textTertiary,
                        maxLines = 2
                    )
                }
                KochCloseButton(onClick = onDismiss)
            }

            if (activeRide != null) {
                ActiveRideCard(ride = activeRide, modifier = Modifier.padding(top = KochSpace.l))
            } else {
                KochSection(title = "Куда едем?")
                KochCard(padding = PaddingValues(vertical = KochSpace.xs)) {
                    DefaultTaxiDestinations.items.forEachIndexed { index, destination ->
                        KochListRow(
                            title = destination.title,
                            subtitle = destination.subtitle,
                            selected = destination.id == taxiState.selectedDestination.id,
                            showDivider = index != DefaultTaxiDestinations.items.lastIndex,
                            onClick = { onSelectDestination(destination) },
                            leading = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (destination.id == taxiState.selectedDestination.id) {
                                        colors.accent
                                    } else {
                                        colors.textTertiary
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailing = {
                                Text(
                                    text = "${destination.distanceKm} км",
                                    style = KochType.caption,
                                    color = colors.textTertiary
                                )
                            }
                        )
                    }
                }

                KochSection(title = "Тариф поездки")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(KochSpace.s),
                    contentPadding = PaddingValues(vertical = KochSpace.xs)
                ) {
                    items(TaxiTariffType.values().toList(), key = { it.name }) { tariff ->
                        TariffCard(
                            tariff = tariff,
                            selected = tariff == taxiState.selectedTariff,
                            onClick = { onSelectTariff(tariff) }
                        )
                    }
                }
            }
        },
        footer = {
            Column {
                if (hasRide) {
                    KochTextButton(
                        text = "Отменить заказ",
                        onClick = onCancelTaxi,
                        color = colors.accentDanger,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    KochPrimaryButton(
                        text = "Вызвать ${taxiState.selectedTariff.titleRu} за " +
                            "${formatRuAmount(taxiState.selectedTariff.basePriceRu)} ₽",
                        onClick = {
                            launchError = null
                            showLaunchConfirm = true
                        }
                    )
                    Spacer(Modifier.heightIn(min = KochSpace.s))
                    Text(
                        text = "Заказ продолжится в приложении Яндекс Go",
                        style = KochType.caption,
                        color = colors.textPrimary.copy(alpha = 0.45f)
                    )
                    launchError?.let { message ->
                        Text(
                            text = message,
                            style = KochType.sub,
                            color = colors.accentDanger,
                            modifier = Modifier.padding(top = KochSpace.s)
                        )
                    }
                }
            }
        }
    )

    if (showLaunchConfirm) {
        KochConfirmSheet(
            title = "Открыть Яндекс Go?",
            message = "Маршрут откроется в приложении Яндекс Go. " +
                "Проверьте точки подачи и назначения.",
            confirmText = "Открыть",
            dismissText = "Отмена",
            onConfirm = {
                showLaunchConfirm = false
                val selectedCity = city
                if (selectedCity == null) {
                    launchError = "Не удалось определить координаты города для маршрута."
                } else if (!taxiDeepLinkPlan(selectedCity, taxiState.selectedDestination)
                        .openFirstAvailable(context)
                ) {
                    launchError = "Не удалось открыть Яндекс Go. " +
                        "Установите приложение или браузер и попробуйте снова."
                }
            },
            onDismiss = { showLaunchConfirm = false }
        )
    }
}

@Composable
private fun TariffCard(
    tariff: TaxiTariffType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    Column(
        modifier = Modifier
            .width(104.dp)
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(KochRadius.panel))
            .background(if (selected) colors.accent.copy(alpha = 0.14f) else colors.surfaceAlt)
            .then(
                if (selected) {
                    Modifier.border(
                        width = KochBorderWidth.hairline,
                        color = colors.accent.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(KochRadius.panel)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(KochSpace.m)
    ) {
        Text(
            text = tariff.titleRu,
            style = KochType.bodyStrong,
            color = colors.textPrimary,
            maxLines = 1
        )
        Text(
            text = "~${tariff.etaMinutes} мин",
            style = KochType.caption,
            color = colors.textTertiary,
            maxLines = 1
        )
        Spacer(Modifier.heightIn(min = KochSpace.s))
        Text(
            text = "${formatRuAmount(tariff.basePriceRu)} ₽",
            style = KochType.numberPrice,
            color = colors.textPrimary,
            maxLines = 1
        )
        Text(
            text = "${formatByn(tariff.basePriceByn)} BYN",
            style = KochType.caption,
            color = colors.textTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun ActiveRideCard(
    ride: TaxiRide,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current
    KochCard(modifier = modifier, material = KochMaterial.Solid) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = colors.accentPromo,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(KochSpace.s))
            Text(
                text = ride.status.titleRu,
                style = KochType.bodyStrong,
                color = colors.accentPromo
            )
        }
        Spacer(Modifier.heightIn(min = KochSpace.m))
        Text(
            text = "${ride.carModel} · ${ride.carPlate}",
            style = KochType.titleCard,
            color = colors.textPrimary
        )
        Text(
            text = "Водитель: ${ride.driverName}",
            style = KochType.sub,
            color = colors.textSecondary
        )
        Spacer(Modifier.heightIn(min = KochSpace.m))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Пункт назначения",
                style = KochType.caption,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ride.destination.title,
                style = KochType.caption,
                color = colors.textPrimary
            )
        }
        Spacer(Modifier.heightIn(min = KochSpace.xs))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Стоимость",
                style = KochType.caption,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${formatRuAmount(ride.priceRu)} ₽ · ${formatByn(ride.priceByn)} BYN",
                style = KochType.numberRow,
                color = colors.textPrimary
            )
        }
    }
}
