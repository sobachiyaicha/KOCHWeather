@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.koch.weather.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.MapKit.MKCoordinateRegion
import platform.MapKit.MKCoordinateSpan
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

/**
 * iOS-actual: MKMapView в UIKitView (playbook Phase 5 — MapKit-first).
 *
 * Минимально-компилируемо для фазы 2 и видно в симуляторе: карта центрируется
 * на координатах города, пин-аннотация с title = город и subtitle = «t° · условие»
 * (формат 1:1 с Android markerSnippet), стандартный зум (span ~0.04°),
 * подпись «© OpenStreetMap contributors» в нижнем углу поверх карты.
 *
 * Тюнинг для Phase 5 (map-engineer): типовые MKAnnotationView/пин-иконка в стиле
 * Android-булавки, кнопка «Центр карты» (на Android она внутри PlatformMap),
 * масштаб зума/анимации, локальный кэш тайлов и LiteMode-аналог.
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
    // Подпись маркера — 1:1 с androidMain markerSnippet (там она internal).
    val snippet = when {
        currentTemp == null -> "Нет данных"
        conditionRu.isNullOrBlank() -> "$currentTemp°"
        else -> "$currentTemp° · $conditionRu"
    }
    val mapState = remember { IosMapStateHolder() }

    LaunchedEffect(latitude, longitude, cityName, currentTemp, conditionRu) {
        val view = mapState.view ?: return@LaunchedEffect
        val region = cValue<MKCoordinateRegion> {
            center.latitude = latitude
            center.longitude = longitude
            span.latitudeDelta = 0.04
            span.longitudeDelta = 0.04
        }
        val coordinate = cValue<CLLocationCoordinate2D> {
            this.latitude = latitude
            this.longitude = longitude
        }
        val annotation = mapState.annotation
        if (annotation != null) {
            annotation.setCoordinate(coordinate)
            annotation.setTitle(cityName)
            annotation.setSubtitle(snippet)
        } else {
            val created = MKPointAnnotation().apply {
                setCoordinate(coordinate)
                setTitle(cityName)
                setSubtitle(snippet)
            }
            view.addAnnotation(created)
            mapState.annotation = created
        }
        view.setRegion(region, animated = false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
    ) {
        UIKitView(
            factory = {
                MKMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
                    // mapType/атрибуция Apple — умолчания; OSM-тайлы и стиль пина — Phase 5.
                    val region = cValue<MKCoordinateRegion> {
                        center.latitude = latitude
                        center.longitude = longitude
                        span.latitudeDelta = 0.04
                        span.longitudeDelta = 0.04
                    }
                    setRegion(region, animated = false)
                    val coordinate = cValue<CLLocationCoordinate2D> {
                        this.latitude = latitude
                        this.longitude = longitude
                    }
                    val annotation = MKPointAnnotation().apply {
                        setCoordinate(coordinate)
                        setTitle(cityName)
                        setSubtitle(snippet)
                    }
                    addAnnotation(annotation)
                    mapState.view = this
                    mapState.annotation = annotation
                    this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { /* обновления через LaunchedEffect выше */ }
        )

        Text(
            text = "© OpenStreetMap contributors",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 4.dp)
                .background(Color(0x66000000))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private class IosMapStateHolder {
    var view: MKMapView? = null
    var annotation: MKPointAnnotation? = null
}
