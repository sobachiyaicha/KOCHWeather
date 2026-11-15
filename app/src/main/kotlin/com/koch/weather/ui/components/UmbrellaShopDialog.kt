package com.koch.weather.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.koch.weather.R
import com.koch.weather.ui.design.KochCloseButton
import com.koch.weather.ui.design.KochColorDot
import com.koch.weather.ui.design.KochPanel
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import java.lang.ref.WeakReference

/**
 * Магазин зонтов. Тот же язык, что у остальных листов: карточка товара, три расцветки,
 * цена и одна капсула. Локальное состояние покупки сохранено (бэкенда нет).
 */
@Composable
fun UmbrellaShopDialog(
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var isPurchased by remember { mutableStateOf(false) }

    val swatches = remember {
        listOf(
            Triple("Классический чёрный", "чёрный", Color(0xFF212121)),
            Triple("Небесно-голубой", "голубой", Color(0xFF42A5F5)),
            Triple("Золотой VIP", "золотой", Color(0xFFE9BF60))
        )
    }

    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.72f,
        content = {
            // Ролик открывается заново при каждом показе листа: композиция листа
            // создаётся с нуля, а MediaPlayer освобождается в DisposableEffect.
            // Ширина листа, пропорции исходника (1280×1000), скругление.
            UmbrellaShopVideo(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(UMBRELLA_VIDEO_ASPECT)
                    .clip(RoundedCornerShape(KochRadius.panel))
                    .background(Color.Black)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = KochSpace.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BeachAccess,
                    contentDescription = null,
                    tint = colors.accentPromo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(KochSpace.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Зонты KOCH Protect",
                        style = KochType.titleCard,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Складной, антиштормовой",
                        style = KochType.sub,
                        color = colors.textTertiary
                    )
                }
                KochCloseButton(onClick = onDismiss)
            }

            Spacer(Modifier.height(KochSpace.l))

            KochPanel {
                Text(
                    text = "Автоматический антиштормовой зонт",
                    style = KochType.bodyStrong,
                    color = colors.textPrimary
                )
                Text(
                    text = "12 карбоновых спиц, влагоотталкивающее покрытие Teflon",
                    style = KochType.sub,
                    color = colors.textTertiary
                )
                Spacer(Modifier.height(KochSpace.m))
                Text(
                    text = swatches[selectedColorIndex].first,
                    style = KochType.caption,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(KochSpace.s))
                Row(horizontalArrangement = Arrangement.spacedBy(KochSpace.s)) {
                    swatches.forEachIndexed { index, swatch ->
                        KochColorDot(
                            color = swatch.third,
                            selected = index == selectedColorIndex,
                            contentDescription = "Расцветка: ${swatch.second}",
                            onClick = { selectedColorIndex = index }
                        )
                    }
                }
            }

            Spacer(Modifier.height(KochSpace.l))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Специальная цена",
                        style = KochType.caption,
                        color = colors.textTertiary
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "649 ₽",
                            style = KochType.numberPrice,
                            color = colors.textPrimary
                        )
                    }
                }
            }

            if (isPurchased) {
                Spacer(Modifier.height(KochSpace.m))
                Text(
                    text = "Курьер уже бежит к вам сквозь дождь!",
                    style = KochType.caption,
                    color = colors.accentSuccess
                )
            }
        },
        footer = {
            Column {
                if (isPurchased) {
                    KochTextButton(
                        text = "Закрыть",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    KochPrimaryButton(
                        text = "Купить за 649 ₽",
                        onClick = { isPurchased = true }
                    )
                }
            }
        }
    )
}

/**
 * Размер кадра ролика известен заранее (`res/raw/umbrella_face.mp4` — 640×500),
 * поэтому fit-матрица `TextureView` выставляется до первого кадра: декод не
 * успевает показать растянутый кадр до `onVideoSizeChanged`.
 */
private const val UMBRELLA_VIDEO_WIDTH = 640
private const val UMBRELLA_VIDEO_HEIGHT = 500

/** Пропорции ролика: исходник 640×500 = 1.28 : 1, бокс без чёрных полей. */
private const val UMBRELLA_VIDEO_ASPECT = 1.28f

/**
 * Ролик магазина зонтов (`res/raw/umbrella_face.mp4`).
 *
 * Играет сразу при открытии листа, зациклен, со звуком, без контролов, паузы и перемотки.
 * На уход приложения в фон — `pause()`, на возврат — `start()`. `DisposableEffect`
 * освобождает `MediaPlayer`/`Surface` при закрытии листа, поэтому повторное открытие
 * каждый раз создаёт новый плеер и ролик начинается сначала.
 *
 * `TextureView`, а не `VideoView`: `SurfaceView` внутри `VideoView` рисуется отдельным
 * слоем поверх окна и не обрезается скруглением Compose — углы ролика остались бы
 * прямыми. `TextureView` — обычный View, поэтому `clip(RoundedCornerShape)` работает.
 * Бокс повторяет пропорции исходника (640×500, 1.28 : 1), поэтому кадр заполняет его
 * целиком без чёрных полей и без обрезки (`Fit`).
 */
@Composable
private fun UmbrellaShopVideo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(context) {
        UmbrellaVideoController(context.applicationContext)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> TextureView(ctx).also(controller::attach) }
    )

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> controller.pause()
                Lifecycle.Event.ON_START -> controller.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
        }
    }
}

/**
 * Плеер ролика без UI-контролов. Держит `MediaPlayer` и `Surface` ровно пока жива
 * композиция листа; все переходы состояния защищены `runCatching`/флагом [isReleased],
 * чтобы поздний колбэк `prepareAsync` не запустил уже освобождённый плеер.
 */
private class UmbrellaVideoController(private val context: Context) {

    private var player: MediaPlayer? = null
    private var surface: Surface? = null

    /** Размер кадра известен заранее: матрица `Fit` готова до первого кадра. */
    private var videoWidth = UMBRELLA_VIDEO_WIDTH
    private var videoHeight = UMBRELLA_VIDEO_HEIGHT
    private var isPrepared = false
    private var isForeground = true
    private var isReleased = false
    private var viewRef: WeakReference<TextureView>? = null

    fun attach(view: TextureView) {
        viewRef = WeakReference(view)
        if (isReleased) return
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (isReleased) return
                start(surfaceTexture)
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                fitToView()
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                // Наш Surface освобождаем сами; true — систему просим освободить SurfaceTexture.
                stop()
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
        }
        // TextureView уже с готовой поверхностью — колбэк available повторно не придёт.
        if (view.isAvailable) {
            view.surfaceTexture?.let { start(it) }
        }
    }

    private fun start(surfaceTexture: SurfaceTexture) {
        stop()
        if (isReleased) return
        val mp = MediaPlayer()
        player = mp
        runCatching {
            surface = Surface(surfaceTexture)
            mp.setSurface(surface)
            context.resources.openRawResourceFd(R.raw.umbrella_face)?.use { descriptor ->
                mp.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            } ?: error("res/raw/umbrella_face.mp4 не найден")
            mp.isLooping = true
            mp.setOnVideoSizeChangedListener { _, width, height ->
                videoWidth = width
                videoHeight = height
                fitToView()
            }
            mp.setOnPreparedListener { prepared ->
                if (isReleased || player !== prepared) return@setOnPreparedListener
                isPrepared = true
                // Звук ролика: полная громкость, медиа-поток как у фильма.
                prepared.setVolume(1f, 1f)
                prepared.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                if (isForeground) prepared.start()
            }
            mp.setOnErrorListener { _, _, _ ->
                // Битый/неподдерживаемый файл не роняет лист: остаётся чёрная подложка.
                stop()
                true
            }
            mp.prepareAsync()
            // Матрицу `Fit` ставим сразу, не дожидаясь onVideoSizeChanged:
            // размер кадра известен заранее, растянутого первого кадра нет.
            fitToView()
        }.onFailure { stop() }
    }

    /** Вписать кадр целиком в TextureView, не растягивая: меньший из масштабов. */
    private fun fitToView() {
        val view = viewRef?.get() ?: return
        val viewWidth = view.width
        val viewHeight = view.height
        if (viewWidth <= 0 || viewHeight <= 0) return
        if (videoWidth <= 0 || videoHeight <= 0) {
            view.setTransform(Matrix())
            return
        }
        val scaleX = viewWidth.toFloat() / videoWidth
        val scaleY = viewHeight.toFloat() / videoHeight
        val fit = minOf(scaleX, scaleY)
        view.setTransform(
            Matrix().apply {
                setScale(
                    fit / scaleX,
                    fit / scaleY,
                    viewWidth / 2f,
                    viewHeight / 2f
                )
            }
        )
    }

    fun pause() {
        isForeground = false
        val mp = player ?: return
        runCatching { if (isPrepared && mp.isPlaying) mp.pause() }
    }

    fun resume() {
        isForeground = true
        val mp = player ?: return
        runCatching { if (isPrepared && !mp.isPlaying) mp.start() }
    }

    fun release() {
        isReleased = true
        stop()
        viewRef?.get()?.surfaceTextureListener = null
        viewRef = null
    }

    private fun stop() {
        isPrepared = false
        player?.let { mp ->
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
        player = null
        surface?.let { runCatching { it.release() } }
        surface = null
        // Размер кадра — константа файла, он не «сбрасывается» вместе с плеером.
        videoWidth = UMBRELLA_VIDEO_WIDTH
        videoHeight = UMBRELLA_VIDEO_HEIGHT
    }
}
