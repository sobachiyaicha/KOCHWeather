@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.koch.weather.platform

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.koch.weather.ui.design.KochIconButton
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.LocalKochColors
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cValue
import kotlinx.cinterop.pin
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGContextFillEllipseInRect
import platform.CoreGraphics.CGContextSetFillColorWithColor
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegion
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKOverlayProtocol
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKTileOverlay
import platform.MapKit.MKTileOverlayPath
import platform.MapKit.MKTileOverlayRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.darwin.NSObject
import platform.objc.sel_registerName

/**
 * iOS-actual: MKMapView в UIKitView — 1:1 с androidMain PlatformMap (Phase 5).
 *
 * Соответствие Android-эталону (osmdroid MAPNIK = стандартные OSM-тайлы):
 * - тайлы: MKTileOverlay с шаблоном https://tile.openstreetmap.org/{z}/{x}/{y}.png,
 *   canReplaceMapContent = true — базовая карта Apple не рисуется (аналог
 *   setUseDataConnection(true) + MAPNIK вместо системного источника);
 * - User-Agent: переопределяется loadTileAtPath(path:result:) (селектор-матчинг,
 *   метод категории CustomLoading отсутствует в сгенерированном биндинге) —
 *   каждый тайл грузится через Ktor с честным appUserAgent() (политика OSM);
 * - пин: MKAnnotationView с рисованной UIImage (белое кольцо 24pt + точка 0.62
 *   радиуса в цвете акцента) — аналог pinDrawable из Canvas; тап по аннотации →
 *   didSelectAnnotationView → центрирование с анимацией (аналог animateTo) и
 *   callout с title = город и subtitle = «28° · Ясно» / «Нет данных» / «28°»
 *   (аналог info-window, формат 1:1 с markerSnippet);
 * - кнопка «Центр карты»: KochIconButton 1:1 (TopEnd, statusBarsPadding,
 *   top = 84.dp, end = KochSpace.l, tint = accent, containerAlpha = 0.85f);
 *   клик → setRegion(город, span z12) с анимацией — аналог animateTo + setZoom(12);
 * - зум: см. [Z12_SPAN_DEGREES];
 * - центрирование при смене города: LaunchedEffect двигает аннотацию и центр
 *   (первый показ — animated = false, смена города — animated = true; span
 *   сохраняется — аналог setCenter без смены зума);
 * - фон за картой Color(0xFF101820) — как на Android; без сети тайлы
 *   не грузятся, остаётся тёмный фон (1:1 деградация). Атрибуция OSM —
 *   в футер-карточке WeatherMapScreen (1:1 с Android; внутри карты её
 *   не дублируем), плюс системная строка Apple «© OpenStreetMap contributors …
 *   Правовые документы», которую MKMapView рисует всегда (платформенное
 *   отличие, зафиксировано в отчёте port-lead).
 *
 * Отступление от «чистого» MapKit-API (зафиксировано в отчёте phase-5): биндинг
 * этого toolchain не экспортирует категорию MKMapView (OverlaysAPI) с addOverlay:
 * и категорию MKTileOverlay (CustomLoading) с loadTileAtPath:result:, поэтому
 * - добавление оверлея идёт динамически: performSelector("addOverlay:", ...);
 * - загрузка тайлов — через метод-двойник loadTileAtPath (K/N экспортирует его
 *   селектором loadTileAtPath:result:), сеть — Ktor/Darwin с заголовком User-Agent.
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
    // Обычный холдер без snapshot-state: пишем из factory, читаем из делегата/эффектов.
    val mapState = remember { IosMapStateHolder() }
    // Клиент и скоуп для tile-загрузки — один на экземпляр карты.
    val tileClient = remember { tileHttpClient() }
    val tileScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    // Подпись маркера — 1:1 с androidMain markerSnippet.
    val snippet = markerSnippet(currentTemp, conditionRu)

    DisposableEffect(Unit) {
        onDispose {
            tileScope.cancel()
            tileClient.close()
        }
    }

    // Смена города/погоды: двигаем уже созданную аннотацию и центрируем карту.
    LaunchedEffect(latitude, longitude, cityName, currentTemp, conditionRu) {
        val view = mapState.view ?: return@LaunchedEffect
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
            // Round 4 фикс: тот же порядок — сильная ссылка до addAnnotation.
            mapState.annotation = created
            view.addAnnotation(created)
        }
        // Первый показ — мгновенно; смена города — с анимацией (аналог animateTo).
        // Span не трогаем: на Android setCenter не меняет зум.
        val animated = mapState.centeredOnce
        mapState.centeredOnce = true
        view.setRegion(centeredRegion(view, latitude, longitude), animated = animated)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
    ) {
        UIKitView(
            factory = {
                // Пин и делегат создаются до карты: делегат у MKMapView слабый,
                // держим сильную ссылку в mapState.
                mapState.pinImage = pinImage(colors.accent.toUiColor())
                val tileOverlay = UaTileOverlay(tileClient, tileScope, OSM_TILE_TEMPLATE).apply {
                    setCanReplaceMapContent(true)
                }
                val mapDelegate = MapDelegate(mapState)
                // Round 2 фикс: MKMapView.delegate — weak, локальной переменной
                // мало, GC собирал делегат (пин был дефолтный красный Apple).
                // Сильная ссылка живёт в mapState всё время жизни карты.
                mapState.delegate = mapDelegate
                MKMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
                    setRegion(
                        region(latitude, longitude, Z12_SPAN_DEGREES),
                        animated = false
                    )
                    delegate = mapDelegate
                    val annotation = MKPointAnnotation().apply {
                        setCoordinate(
                            cValue<CLLocationCoordinate2D> {
                                this.latitude = latitude
                                this.longitude = longitude
                            }
                        )
                        setTitle(cityName)
                        setSubtitle(snippet)
                    }
                    // Round 4 фикс: state.annotation присваиваем ДО addAnnotation —
                    // MapKit зовёт mapView:viewForAnnotation: уже внутри
                    // addAnnotation (или на первом layout), и при строгой проверке
                    // пин получал дефолтный красный MKPinAnnotationView.
                    mapState.annotation = annotation
                    addAnnotation(annotation)
                    // addOverlay: отсутствует в биндинге (категория OverlaysAPI) —
                    // динамический вызов настоящего метода MKMapView.
                    performSelector(
                        sel_registerName("addOverlay:"),
                        withObject = tileOverlay
                    )
                    mapState.view = this
                    this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { /* обновления через LaunchedEffect выше */ }
        )

        KochIconButton(
            icon = Icons.Default.MyLocation,
            contentDescription = "Центр карты — выбранный город",
            onClick = {
                // Android: animateTo(point) + setZoom(12.0) — зум сбрасывается к 12.
                mapState.view?.let { view ->
                    view.setRegion(
                        region(latitude, longitude, Z12_SPAN_DEGREES),
                        animated = true
                    )
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

private class IosMapStateHolder {
    var view: MKMapView? = null
    var annotation: MKPointAnnotation? = null
    var delegate: MapDelegate? = null
    var pinImage: UIImage? = null
    var centeredOnce: Boolean = false
    // Round 2 фикс: регион, заданный в factory при frame = 0x0, MapKit теряет
    // при первом layout; применяем z12 один раз после загрузки карты делегатом.
    var initialRegionApplied: Boolean = false
}

/** Стандартные OSM-тайлы — то же, что osmdroid MAPNIK (TileSourceFactory). */
private const val OSM_TILE_TEMPLATE = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

/**
 * Видимый span карты — аналог osmdroid controller.setZoom(12.0).
 * 360° / 2^12 ≈ 0.0879° — угловой размер тайла 256px на z12; берём его как
 * ширину/высоту видимой области MKCoordinateSpan (значение согласовано с
 * port-lead: диапазон 0.08–0.09°). Одинаково используется при создании карты
 * и по кнопке «Центр карты», как setZoom(12.0) на Android.
 */
private const val Z12_SPAN_DEGREES = 0.088

/** Таймаут одного tile-запроса, мс. */
private const val TILE_TIMEOUT_MS = 15_000L

private const val PIN_REUSE_ID = "koch-city-pin"

/** Размер пина в pt — 1:1 с Android (24dp). */
private const val PIN_SIZE_PT = 24.0

/** Доля радиуса точки внутри кольца — 1:1 с Android pinDrawable (0.62). */
private const val PIN_DOT_RADIUS_RATIO = 0.62

/** OSM-политика: каждый tile-запрос помечается честным User-Agent приложения. */
private const val USER_AGENT_HEADER = "User-Agent"

/** Ktor-клиент для tile-загрузки (Darwin engine, как остальная сеть приложения). */
private fun tileHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        requestTimeoutMillis = TILE_TIMEOUT_MS
    }
}

/** Подпись маркера: «2° · Облачно» либо честное «Нет данных» (1:1 с androidMain). */
private fun markerSnippet(currentTemp: Int?, conditionRu: String?): String = when {
    currentTemp == null -> "Нет данных"
    conditionRu.isNullOrBlank() -> "$currentTemp°"
    else -> "$currentTemp° · $conditionRu"
}

/** Region с центром в городе и span'ом z12 (создание карты, кнопка «Центр карты»). */
private fun region(latitude: Double, longitude: Double, spanDegrees: Double) =
    cValue<MKCoordinateRegion> {
        center.latitude = latitude
        center.longitude = longitude
        span.latitudeDelta = spanDegrees
        span.longitudeDelta = spanDegrees
    }

/**
 * Region с центром в городе и ТЕКУЩИМ span'ом карты — аналог setCenter без
 * смены зума; если текущий span вырожден, откат к z12.
 */
private fun centeredRegion(view: MKMapView, latitude: Double, longitude: Double) =
    region(latitude, longitude, currentSpanDegrees(view))

private fun currentSpanDegrees(view: MKMapView): Double =
    view.region().useContents {
        val lat = span.latitudeDelta
        val lon = span.longitudeDelta
        if (lat > 0.0 && lon > 0.0 && lat.isFinite() && lon.isFinite()) {
            (lat + lon) / 2.0
        } else {
            Z12_SPAN_DEGREES
        }
    }

/**
 * ByteArray → NSData. Биндинг Foundation не экспортирует ни один конструктор
 * NSData с байтами (ни bytes/bytesNoCopy/contentOfFile, ни appendBytes/appendData
 * у NSMutableData), поэтому данные отдаются сабклассом NSData, который
 * селектор-матчингом переопределяет bytes/length поверх pin()-нутого массива
 * (тот же механизм, что и loadTileAtPath у UaTileOverlay). Pinned держится
 * полем объекта, поэтому указатель жив, пока жив NSData.
 */
private class TileData(
    private val pinned: Pinned<ByteArray>,
    private val byteCount: Int
) : NSData() {
    override fun bytes(): CPointer<ByteVar> = pinned.addressOf(0)
    override fun length(): ULong = byteCount.toULong()
}

private fun ByteArray.toNsData(): NSData = TileData(pinned = pin(), byteCount = size)

/**
 * Tile-оверлей с OSM-шаблоном и честным User-Agent. Биндинг не экспортирует
 * категорию CustomLoading MKTileOverlay, поэтому метод-двойник loadTileAtPath
 * объявлен с именем/типами, дающими селектор loadTileAtPath:result: — MapKit
 * вызывает его вместо дефолтного загрузчика. URL собирается из шаблона,
 * сеть — Ktor с заголовком User-Agent = appUserAgent() (политика OSM).
 * Без сети колбэк приходит с ошибкой/nil — MapKit остаётся на фоновом цвете,
 * как osmdroid без сети на Android.
 */
private class UaTileOverlay(
    private val client: HttpClient,
    private val scope: CoroutineScope,
    template: String
) : MKTileOverlay(uRLTemplate = template) {

    fun loadTileAtPath(
        path: CValue<MKTileOverlayPath>,
        result: (NSData?, NSError?) -> Unit
    ) {
        val tmpl = URLTemplate()
        if (tmpl == null) {
            result(null, null)
            return
        }
        val (x, y, z) = path.useContents { Triple(x, y, z) }
        val urlString = tmpl
            .replace("{z}", z.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
            .replace("{scale}", "")
        scope.launch {
            try {
                val bytes: ByteArray = client.get(urlString) {
                    header(USER_AGENT_HEADER, appUserAgent())
                }.body()
                result(bytes.toNsData(), null)
            } catch (t: Throwable) {
                result(null, null)
            }
        }
    }
}

/**
 * Делегат карты: пин-вью с рисованной UIImage и реакция на тап.
 * Пин: белое кольцо + точка акцента (аналог pinDrawable); callout = info-window
 * (title = город, subtitle = погода). Тап: центрирование с анимацией на
 * координату аннотации — аналог animateTo(clicked.position) + showInfoWindow.
 */
private class MapDelegate(private val state: IosMapStateHolder) : NSObject(),
    MKMapViewDelegateProtocol {

    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        // Round 4 фикс: строгая проверка `!== state.annotation` фейлила, потому
        // что MapKit вызывает viewForAnnotation уже на addAnnotation (или на
        // первом layout), когда state.annotation мог быть ещё не присвоен —
        // пин получал дефолтный красный MKPinAnnotationView. Теперь защита
        // типовая: системная MKUserLocation (синий дарт) остаётся системной,
        // всё прочее — наш пин.
        if (viewForAnnotation !is MKPointAnnotation) return null
        val view = mapView.dequeueReusableAnnotationViewWithIdentifier(PIN_REUSE_ID)
            ?: MKAnnotationView(annotation = viewForAnnotation, reuseIdentifier = PIN_REUSE_ID)
        view.annotation = viewForAnnotation
        view.canShowCallout = true
        state.pinImage?.let { view.image = it }
        return view
    }

    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        val coordinate = didSelectAnnotationView.annotation?.coordinate ?: return
        val lat = coordinate.useContents { latitude }
        val lon = coordinate.useContents { longitude }
        // Зум сохраняем (Android animateTo меняет только центр).
        mapView.setRegion(centeredRegion(mapView, lat, lon), animated = true)
    }

    override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
        val tile = rendererForOverlay as? MKTileOverlay
            ?: return MKOverlayRenderer(overlay = rendererForOverlay)
        return MKTileOverlayRenderer(tileOverlay = tile)
    }

    /**
     * Round 2 фикс: регион из factory задавался при frame = 0x0 и терялся при
     * первом layout (на экране был системный span ~35-40°). После загрузки
     * карты применяем z12-регион ровно один раз, центрируя по ТЕКУЩЕЙ
     * координате аннотации (город мог смениться, пока карта грузилась).
     */
    override fun mapViewDidFinishLoadingMap(mapView: MKMapView) {
        if (state.initialRegionApplied) return
        state.initialRegionApplied = true
        val coordinate = state.annotation?.coordinate ?: return
        mapView.setRegion(
            region(
                coordinate.useContents { latitude },
                coordinate.useContents { longitude },
                Z12_SPAN_DEGREES
            ),
            animated = false
        )
    }
}

/**
 * Пин 1:1 с Android pinDrawable: белое кольцо 24pt + точка 0.62 радиуса в цвете
 * акцента. UIGraphicsImageRenderer рисует в scale главного экрана (аналог
 * density на Android), чтобы пин не был мыльным на @2x/@3x.
 */
private fun pinImage(accent: UIColor): UIImage {
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(PIN_SIZE_PT, PIN_SIZE_PT))
    return renderer.imageWithActions { context ->
        val cg = context?.CGContext ?: return@imageWithActions
        CGContextSetFillColorWithColor(cg, UIColor.whiteColor.CGColor)
        CGContextFillEllipseInRect(cg, CGRectMake(0.0, 0.0, PIN_SIZE_PT, PIN_SIZE_PT))
        CGContextSetFillColorWithColor(cg, accent.CGColor)
        val dot = PIN_SIZE_PT * PIN_DOT_RADIUS_RATIO
        val offset = (PIN_SIZE_PT - dot) / 2.0
        CGContextFillEllipseInRect(cg, CGRectMake(offset, offset, dot, dot))
    }
}

/** Compose Color → UIColor по компонентам (аргб из toArgb). */
private fun Color.toUiColor(): UIColor {
    val argb = this.toArgb()
    val alpha = ((argb ushr 24) and 0xFF) / 255.0
    val red = ((argb ushr 16) and 0xFF) / 255.0
    val green = ((argb ushr 8) and 0xFF) / 255.0
    val blue = (argb and 0xFF) / 255.0
    return UIColor(red = red, green = green, blue = blue, alpha = alpha)
}
