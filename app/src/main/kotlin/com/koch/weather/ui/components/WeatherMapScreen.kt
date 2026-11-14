package com.koch.weather.ui.components

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * R12: full-screen real OSM map (osmdroid MapView + MAPNIK tiles).
 *
 * No API keys, no VPN: standard OpenStreetMap raster tiles. The map is centred on the currently
 * selected city from [com.koch.weather.data.model.MainScreenUiState] and has a marker + a close
 * button; the system back gesture closes the screen as well.
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

    val context = LocalContext.current
    val mapView = remember {
        // Standard osmdroid bootstrap: initialise the cache paths, then identify the app to the
        // OSM tile servers (their usage policy requires a real user agent).
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
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Header with city name, temperature and close/back controls.
        Surface(
            color = Color(0xF0161F2B),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cityName,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
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
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Центр карты — выбранный город",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .width(20.dp)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть карту",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // OpenStreetMap attribution is mandatory when OSM tiles are displayed.
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = Color(0xCC000000),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
