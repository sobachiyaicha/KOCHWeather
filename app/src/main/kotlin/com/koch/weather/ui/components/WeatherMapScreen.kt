package com.koch.weather.ui.components

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.koch.weather.ui.design.KochIconButton
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * R12: полноэкранная карта OSM. Хром — плавающее стекло поверх карты:
 * капсула-хедер, рабочая кнопка «центр», нижняя карточка с атрибуцией.
 */
@Composable
fun WeatherMapScreen(
    cityName: String,
    latitude: Double,
    longitude: Double,
    currentTemp: Int?,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val colors = LocalKochColors.current
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            setUseDataConnection(true)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    LaunchedEffect(latitude, longitude, cityName, currentTemp) {
        mapView.controller.setZoom(12.0)
        mapView.controller.setCenter(GeoPoint(latitude, longitude))
        mapView.overlays.removeAll { it is Marker }
        mapView.overlays.add(
            Marker(mapView).apply {
                position = GeoPoint(latitude, longitude)
                title = cityName
                snippet = currentTemp?.let { "$it°" } ?: "Погода"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
        )
        mapView.invalidate()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Верхний скрим: хром читается на любой карте.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x73000000), Color(0x00000000))
                    )
                )
        )

        // Плавающая капсула-хедер вместо непрозрачной полосы.
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = KochSpace.l, vertical = KochSpace.s),
            shape = KochShape.pill,
            color = colors.sheetSurface,
            border = BorderStroke(KochBorderWidth.hairline, colors.borderStrong),
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KochSpace.xs, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KochIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    onClick = onClose,
                    tint = colors.textPrimary,
                    containerAlpha = 0f
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cityName,
                        style = KochType.bodyStrong,
                        color = colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = buildString {
                            append("Карта · OpenStreetMap")
                            if (currentTemp != null) {
                                append(" · ")
                                append(currentTemp)
                                append("°")
                            }
                        },
                        style = KochType.caption,
                        color = colors.textTertiary,
                        maxLines = 1
                    )
                }
            }
        }

        // Кнопка «центр»: реально возвращает карту к городу (была декоративная иконка + X).
        KochIconButton(
            icon = Icons.Default.MyLocation,
            contentDescription = "Центр карты — выбранный город",
            onClick = {
                mapView.controller.animateTo(GeoPoint(latitude, longitude))
                mapView.controller.setZoom(12.0)
                mapView.invalidate()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 84.dp, end = KochSpace.l),
            tint = colors.accent,
            containerAlpha = 0.85f
        )

        // Нижняя карточка: город, температура и обязательная атрибуция OSM.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = KochSpace.l, vertical = KochSpace.l),
            shape = RoundedCornerShape(KochRadius.card),
            color = colors.sheetSurface,
            border = BorderStroke(KochBorderWidth.hairline, colors.borderStrong),
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KochSpace.l, vertical = KochSpace.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cityName,
                        style = KochType.bodyStrong,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Маркер — выбранный город",
                        style = KochType.caption,
                        color = colors.textTertiary,
                        maxLines = 1
                    )
                    Text(
                        text = "© OpenStreetMap contributors",
                        style = KochType.caption,
                        color = colors.textQuaternary,
                        maxLines = 1
                    )
                }
                if (currentTemp != null) {
                    Spacer(Modifier.width(KochSpace.s))
                    Text(
                        text = "$currentTemp°",
                        style = KochType.displayCity,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
