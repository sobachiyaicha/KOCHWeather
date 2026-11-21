package com.koch.weather.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.koch.weather.ui.design.KochIconButton
import com.koch.weather.ui.theme.LocalKochColors
import com.koch.weather.ui.theme.KochSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Android-actual: osmdroid MapView в AndroidView — тело карты перенесено 1:1 из
 * WeatherMapScreen (см. её комментарии: маркер создаётся ПРЯМО в factory, пин —
 * BitmapDrawable из Canvas, Configuration.load в IO).
 *
 * Зона platform (package com.koch.weather.platform): сама карта, маркер, центрирование и кнопка «Центр карты»
 * (управление нативным MapView). Стекло экрана (плашка города, градиент,
 * карточка с атрибуцией) остаётся в ui.components/WeatherMapScreen.
 */
@Composable
actual fun PlatformMap(
    cityName: String,
    latitude: Double,
    longitude: Double,
    currentTemp: Int?,
    conditionRu: String?,
    modifier: Modifier
) {
    val colors = LocalKochColors.current
    val context = LocalContext.current
    var mapReady by remember { mutableStateOf(false) }
    // Обычный холдер без snapshot-state: пишем в него из factory, читаем из колбэков.
    val mapState = remember { MapStateHolder() }
    val pinColor = colors.accent

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

    // Смена города/погоды: двигаем уже созданный маркер и центрируем карту.
    LaunchedEffect(latitude, longitude, cityName, currentTemp, conditionRu, mapReady) {
        val view = mapState.view ?: return@LaunchedEffect
        val point = GeoPoint(latitude, longitude)
        view.controller.setCenter(point)
        val marker = mapState.marker
        if (marker != null) {
            marker.position = point
            marker.title = cityName
            marker.snippet = markerSnippet(currentTemp, conditionRu)
        } else {
            view.overlays.removeAll { it is Marker }
            view.overlays.add(
                buildMarker(view, point, cityName, currentTemp, conditionRu, pinColor)
            )
        }
        view.invalidate()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val view = mapState.view ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapState.view?.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
    ) {
        if (mapReady) {
            AndroidView(
                factory = { ctx ->
                    val view = MapView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setUseDataConnection(true)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(latitude, longitude))
                    }
                    // Маркер — здесь же: не зависим от порядка «состояние → эффект».
                    val marker = buildMarker(
                        view,
                        GeoPoint(latitude, longitude),
                        cityName,
                        currentTemp,
                        conditionRu,
                        pinColor
                    )
                    view.overlays.add(marker)
                    view.invalidate()
                    mapState.view = view
                    mapState.marker = marker
                    view
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        KochIconButton(
            icon = Icons.Default.MyLocation,
            contentDescription = "Центр карты — выбранный город",
            onClick = {
                mapState.view?.let { view ->
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
    }
}

private class MapStateHolder {
    var view: MapView? = null
    var marker: Marker? = null
}

/** Подпись маркера: «2° · Облачно» либо честное «Нет данных». */
internal fun markerSnippet(currentTemp: Int?, conditionRu: String?): String = when {
    currentTemp == null -> "Нет данных"
    conditionRu.isNullOrBlank() -> "$currentTemp°"
    else -> "$currentTemp° · $conditionRu"
}

private fun buildMarker(
    mapView: MapView,
    point: GeoPoint,
    cityName: String,
    currentTemp: Int?,
    conditionRu: String?,
    color: Color
): Marker = Marker(mapView).apply {
    position = point
    title = cityName
    snippet = markerSnippet(currentTemp, conditionRu)
    icon = pinDrawable(mapView.context, color)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    // Тап по маркеру: центрируем и показываем info-window с городом и погодой.
    setOnMarkerClickListener { clicked, view ->
        view.controller.animateTo(clicked.position)
        clicked.showInfoWindow()
        view.invalidate()
        true
    }
}

/**
 * Маркер-булавка: `BitmapDrawable` (intrinsic size = размеру битмапа) с белым кольцом и
 * точкой цвета акцента. Размер в px считается от density, чтобы пин не был 1 px.
 */
private fun pinDrawable(context: Context, color: Color): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (24f * density).toInt().coerceAtLeast(24)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Белое кольцо-подложка.
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(radius, radius, radius, paint)

    // Точка в цвете акцента.
    paint.color = color.toArgb()
    canvas.drawCircle(radius, radius, radius * 0.62f, paint)

    return BitmapDrawable(context.resources, bitmap).apply {
        setBounds(0, 0, sizePx, sizePx)
    }
}
