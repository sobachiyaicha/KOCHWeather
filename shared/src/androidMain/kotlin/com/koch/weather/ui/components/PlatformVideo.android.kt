package com.koch.weather.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Android-actual [PlatformVideo]: тот же `VideoView`-стек, что в исходнике
 * (AdPromoDialog.AdVideoSession): USAGE_MEDIA/CONTENT_TYPE_MOVIE, звук 1f/1f,
 * пауза на ON_STOP/продолжение на ON_START локального lifecycle + ProcessLifecycleOwner.
 *
 * Зацикленный ролик магазина зонтов в исходнике жил на `TextureView`+`MediaPlayer`
 * (SurfaceView VideoView не обрезается скруглением Compose). Здесь тот же выбор:
 * `isLooping = true` → TextureView+MediaPlayer, иначе VideoView.
 */
@Composable
actual fun PlatformVideo(
    mediaKey: String,
    aspect: Float,
    modifier: Modifier,
    isLooping: Boolean,
    onEnded: (() -> Unit)?
) {
    val url = com.koch.weather.platform.mediaUrl(mediaKey)
    val currentOnEnded = rememberUpdatedState(onEnded)
    val lifecycleOwner = LocalLifecycleOwner.current

    if (isLooping) {
        // Ветвь магазина зонтов (UmbrellaVideoController, 1:1): TextureView+MediaPlayer,
        // loop, звук 1f/1f, бокс повторяет пропорции исходника — Fit достигается
        // равенством аспектов, Matrix-трансформация исходника не нужна.
        val controller = remember(mediaKey) {
            LoopingTextureViewSession(url)
        }
        AndroidView(
            modifier = modifier,
            factory = { ctx -> controller.obtainView(ctx) },
            onRelease = { controller.release() }
        )
        DisposableEffect(lifecycleOwner, controller) {
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
    } else {
        // Ветвь рекламы (AdVideoSession, 1:1): VideoView без контролов и жестов.
        val session = remember(mediaKey) {
            SingleShotVideoSession(url) { currentOnEnded.value?.invoke() }
        }
        AndroidView(
            factory = { ctx -> session.obtainView(ctx) },
            modifier = modifier,
            onRelease = { session.release() }
        )
        DisposableEffect(lifecycleOwner, session) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> session.pause()
                    Lifecycle.Event.ON_START -> session.resume()
                    else -> Unit
                }
            }
            // Локальный lifecycle ловит уход в фон, если диалог получает собственный
            // lifecycle-owner; ProcessLifecycleOwner — гарантированный дубль на Home/звонок.
            lifecycleOwner.lifecycle.addObserver(observer)
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
                session.release()
            }
        }
    }
}

/**
 * Рекламный ролик (порт AdVideoSession): [onEnded] зовётся один раз, когда ролик
 * доиграл до конца; после [release] колбэк не срабатывает.
 */
private class SingleShotVideoSession(
    private val url: String,
    private val onEnded: () -> Unit
) {
    private var videoView: VideoView? = null
    private var isCompleted = false
    private var isReleased = false

    fun obtainView(context: Context): VideoView {
        videoView?.let { existing ->
            if (!isReleased) return existing
        }
        val view = VideoView(context).apply {
            // Никаких контролов: панель не ставим, пауза/перемотка жестами не нужны.
            setMediaController(null)
            setOnTouchListener { _, _ -> true }
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = false
                // Звук не глушим: ролик звучит как в оригинале.
                mediaPlayer.setVolume(1f, 1f)
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                start()
            }
            setOnCompletionListener {
                if (isReleased) return@setOnCompletionListener
                // Ролик доиграл: поп-ап закрывается сам (для фото автозакрытия нет).
                isCompleted = true
                onEnded()
            }
            setOnErrorListener { _, _, _ -> true }
            setVideoURI(Uri.parse(url))
        }
        videoView = view
        isCompleted = false
        isReleased = false
        return view
    }

    /** Пауза при уходе в фон: ролик не должен звучать из кармана. */
    fun pause() {
        val view = videoView ?: return
        if (!isCompleted && view.isPlaying) view.pause()
    }

    /** Возврат из фона: продолжаем с того же места, но не перезапускаем законченный ролик. */
    fun resume() {
        val view = videoView ?: return
        if (!isCompleted && !view.isPlaying) view.start()
    }

    /** Освобождение плеера (закрытие поп-апа/выход из композиции). Идемпотентно. */
    fun release() {
        if (isReleased) return
        isReleased = true
        videoView?.let { view ->
            view.setOnPreparedListener(null)
            view.setOnCompletionListener(null)
            view.setOnErrorListener(null)
            view.stopPlayback()
        }
        videoView = null
    }
}

/**
 * Зацикленный ролик магазина зонтов (порт UmbrellaVideoController): TextureView
 * (SurfaceView не обрезается скруглением), MediaPlayer loop, звук 1f/1f.
 */
private class LoopingTextureViewSession(private val url: String) {

    private var mediaPlayer: android.media.MediaPlayer? = null
    private var isReleased = false
    private var isPrepared = false
    private var isForeground = true

    fun obtainView(context: Context): android.view.TextureView {
        val view = android.view.TextureView(context)
        view.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (isReleased) return
                start(context, surfaceTexture)
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) = Unit

            override fun onSurfaceTextureDestroyed(
                surfaceTexture: android.graphics.SurfaceTexture
            ): Boolean {
                // true — систему просим освободить SurfaceTexture (1:1 с исходником).
                stop()
                return true
            }

            override fun onSurfaceTextureUpdated(
                surfaceTexture: android.graphics.SurfaceTexture
            ) = Unit
        }
        // TextureView уже с готовой поверхностью — колбэк available повторно не придёт.
        if (view.isAvailable) {
            view.surfaceTexture?.let { start(context, it) }
        }
        return view
    }

    private fun start(context: Context, surfaceTexture: android.graphics.SurfaceTexture) {
        stop()
        if (isReleased) return
        val mp = android.media.MediaPlayer()
        mediaPlayer = mp
        runCatching {
            mp.setSurface(android.view.Surface(surfaceTexture))
            mp.setDataSource(context, Uri.parse(url))
            mp.isLooping = true
            mp.setOnPreparedListener { prepared ->
                if (isReleased || mediaPlayer !== prepared) return@setOnPreparedListener
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
        }.onFailure { stop() }
    }

    fun pause() {
        isForeground = false
        val mp = mediaPlayer ?: return
        runCatching { if (isPrepared && mp.isPlaying) mp.pause() }
    }

    fun resume() {
        isForeground = true
        val mp = mediaPlayer ?: return
        runCatching { if (isPrepared && !mp.isPlaying) mp.start() }
    }

    fun release() {
        isReleased = true
        stop()
    }

    private fun stop() {
        isPrepared = false
        mediaPlayer?.let { mp ->
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
        mediaPlayer = null
    }
}
