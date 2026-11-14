package com.koch.weather.ui.components

import android.graphics.drawable.GradientDrawable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * R12: полноэкранная карта OSM. Хром — плавающее стекло поверх карты.
 *
 * Производительность (perf-qa):
 *  - `Configuration.load` (диск) вынесен в IO-корутину, а не в `remember` на main;
 *  - `MapView` создаётся в `AndroidView.factory` только после подготовки конфига и только
 *    когда этот экран реально скомпонован (главная под картой больше не живёт);
 *  - `setTilesScaledToDpi` убран — меньше пересчётов тайлов;
 *  - маркер — точка-булавка (GradientDrawable), а не системная иконка «рука».
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
    var mapReady by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val pinColor = colors.accent

    // Дисковый load конфигурации osmdroid — только в IO, не на главном потоке.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            Configuration.getInstance().apply {
                load(
                    context,
                    context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
                )
                userAgentValue = context.packageName
            }
        }
        mapReady = true
    }

    // Центр и маркер обновляются при смене города и ПОСЛЕ создания MapView (ключ mapView):
    // иначе первый проход видит null и маркер не ставится — даже когда погоды нет.
    LaunchedEffect(mapReady, mapView, latitude, longitude, cityName, currentTemp) {
        val view = mapView ?: return@LaunchedEffect
        view.controller.setZoom(12.0)
        view.controller.setCenter(GeoPoint(latitude, longitude))
        view.overlays.removeAll { it is Marker }
        view.overlays.add(
            Marker(view).apply {
                position = GeoPoint(latitude, longitude)
                title = cityName
                snippet = currentTemp?.let { "$it°" } ?: "Нет данных"
                icon = pinDrawable(pinColor)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
        )
        view.invalidate()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val view = mapView
        if (view == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> view.onResume()
                    Lifecycle.Event.ON_PAUSE -> view.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                view.onDetach()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
    ) {
        if (mapReady) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setUseDataConnection(true)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(latitude, longitude))
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

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
                            append(" · ")
                            // Нет погоды — не пропускаем, а честно пишем «Нет данных».
                            append(currentTemp?.let { "$it°" } ?: "Нет данных")
                        },
                        style = KochType.caption,
                        color = colors.textTertiary,
                        maxLines = 1
                    )
                }
            }
        }

        KochIconButton(
            icon = Icons.Default.MyLocation,
            contentDescription = "Центр карты — выбранный город",
            onClick = {
                mapView?.let { view ->
                    view.controller.animateTo(GeoPoint(latitude, longitude))
                    view.controller.setZoom(12.0)
                    view.invalidate()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 84.dp, end = KochSpace.l),
            tint = colors.accent,
            containerAlpha = 0.85f
        )

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

/** Точка-булавка вместо системной иконки: простое кольцо в цвете акцента. */
private fun pinDrawable(color: Color): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color.toArgb())
    setStroke(6, android.graphics.Color.WHITE)
    setBounds(0, 0, 42, 42)
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)
