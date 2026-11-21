@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.koch.weather.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.ComposeUIViewController
import com.koch.weather.platform.appForegroundFlow
import com.koch.weather.platform.isDebugBuild
import com.koch.weather.platform.mediaUrl
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.currentItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSLog
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.darwin.NSObjectProtocol

/**
 * iOS-actual [PlatformVideo] — AVPlayer + AVPlayerLayer (port plan §6).
 *
 * Заменяет прежний MPMoviePlayerController (легаси MediaPlayer.framework):
 * в klib 2.4.21 биндинги `AVPlayer`/`AVPlayerItem`/`AVPlayerLayer` имеют все нужные
 * члены (play/pause/currentItem/replaceCurrentItemWithPlayerItem/seekToTime,
 * `AVPlayerLayer.player` + `videoGravity`), поэтому отклонение фазы 3 отменено.
 *
 * Поведение 1:1 с Android-actual:
 *  - звук: `AVAudioSession` категория `.playback` + `setActive(true)` один раз на процесс;
 *  - AspectFit: `AVLayerVideoGravityResizeAspect`, контент не обрезается;
 *  - никаких контролов, жестов и паузы по тапу;
 *  - конец ролика (`AVPlayerItemDidPlayToEndTimeNotification`): `isLooping` → seek 0 + play,
 *    иначе `onEnded` ровно один раз;
 *  - пауза в фоне / продолжение по возврату (`appForegroundFlow` + UIApplication-нотификации);
 *    после конца незацикленного ролика `resume()` не перезапускает его;
 *  - плеер живёт ровно столько, сколько живёт композиция: повторное открытие диалога
 *    создаёт новый плеер и ролик начинается с нуля.
 *
 * Файлы роликов лежат в бандле в `media/` (единственная копия в репо —
 * `shared/src/androidMain/res/raw`, упаковка — `scripts/ios-build.sh`), см. [mediaUrl].
 */
@Composable
actual fun PlatformVideo(
    mediaKey: String,
    aspect: Float,
    modifier: Modifier,
    isLooping: Boolean,
    onEnded: (() -> Unit)?
) {
    val url = mediaUrl(mediaKey)
    val currentOnEnded = rememberUpdatedState(onEnded)
    val session = remember(mediaKey, isLooping) {
        IosVideoSession(url = url, isLooping = isLooping) { currentOnEnded.value?.invoke() }
    }

    UIKitView(
        factory = { session.obtainView() },
        modifier = modifier,
        onRelease = { session.release() }
    )

    // Пауза/продолжение при уходе/возврате приложения (аналог ON_STOP/ON_START).
    LaunchedEffect(session) {
        appForegroundFlow().collect { isForeground ->
            if (isForeground) session.resume() else session.pause()
        }
    }
    DisposableEffect(session) {
        onDispose { session.release() }
    }
}

/**
 * Контейнер под [AVPlayerLayer]: `layoutSubviews` перекладывает frame слоя в bounds.
 * Без этого CALayer остаётся нулевого размера и видео не видно (AVPlayerLayer не
 * участвует в autolayout, как и любой вручную добавленный sublayer).
 */
private class VideoContainerView : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    var videoLayer: AVPlayerLayer? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        // Отключаем неявные анимации CALayer при ресайзе контейнера.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        layer.setFrame(bounds)
        videoLayer?.setFrame(bounds)
        CATransaction.commit()
    }
}

/**
 * Сессия одного ролика: `AVPlayer` создаётся один раз на показ диалога/листа.
 * Освобождение идемпотентно (UIKitView.onRelease + DisposableEffect.onDispose).
 */
private class IosVideoSession(
    private val url: String,
    private val isLooping: Boolean,
    private val onEnded: (() -> Unit)?
) {
    private var player: AVPlayer? = null
    private var playerLayer: AVPlayerLayer? = null
    private var container: VideoContainerView? = null
    private var endObserver: NSObjectProtocol? = null
    private var backgroundObserver: NSObjectProtocol? = null
    private var foregroundObserver: NSObjectProtocol? = null
    private var isCompleted = false
    private var isReleased = false

    fun obtainView(): UIView {
        val existing = container
        if (existing != null && !isReleased) return existing

        configureAudioSession()

        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) return VideoContainerView()

        val item = AVPlayerItem(uRL = nsUrl)
        val avPlayer = AVPlayer(playerItem = item)
        val avLayer = AVPlayerLayer()
        avLayer.player = avPlayer
        // Fit: контент вписан целиком, без обрезки (эквивалент ContentScale.Fit).
        avLayer.setVideoGravity(AVLayerVideoGravityResizeAspect)

        val view = VideoContainerView()
        view.videoLayer = avLayer
        view.layer.addSublayer(avLayer)

        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue

        // Конец ролика: луп — с начала; реклама — onEnded ровно один раз.
        endObserver = center.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            avPlayer.currentItem,
            queue
        ) { _ ->
            if (isReleased) return@addObserverForName
            if (isLooping) {
                avPlayer.seekToTime(CMTimeMake(0, 1))
                avPlayer.play()
            } else if (!isCompleted) {
                isCompleted = true
                onEnded?.invoke()
            }
        }

        // Страховка к appForegroundFlow: ролик не должен звучать из кармана.
        backgroundObserver = center.addObserverForName(
            UIApplicationDidEnterBackgroundNotification,
            null,
            queue
        ) { _ -> pause() }

        foregroundObserver = center.addObserverForName(
            UIApplicationWillEnterForegroundNotification,
            null,
            queue
        ) { _ -> resume() }

        avPlayer.play()

        player = avPlayer
        playerLayer = avLayer
        container = view
        return view
    }

    /** Пауза при уходе в фон. */
    fun pause() {
        if (isReleased) return
        player?.pause()
    }

    /** Возврат из фона: продолжаем с того же места, но не перезапускаем законченный ролик. */
    fun resume() {
        if (isReleased || isCompleted) return
        player?.play()
    }

    /** Освобождение плеера, слоя и уведомлений (закрытие диалога). Идемпотентно. */
    fun release() {
        if (isReleased) return
        isReleased = true
        val center = NSNotificationCenter.defaultCenter
        endObserver?.let { center.removeObserver(it) }
        backgroundObserver?.let { center.removeObserver(it) }
        foregroundObserver?.let { center.removeObserver(it) }
        endObserver = null
        backgroundObserver = null
        foregroundObserver = null
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        playerLayer?.removeFromSuperlayer()
        playerLayer?.player = null
        playerLayer = null
        player = null
        container = null
    }
}

/** Аудиосессия .playback + активна: один раз на процесс (иначе звук рекламы глушится silent switch). */
private var audioSessionConfigured = false

private fun configureAudioSession() {
    if (audioSessionConfigured) return
    audioSessionConfigured = true
    val session = AVAudioSession.sharedInstance()
    session.setCategory(AVAudioSessionCategoryPlayback, error = null)
    session.setActive(true, error = null)
}

private fun LOG(message: String) {
    NSLog("[KOCHMedia] $message")
}

// ---------------------------------------------------------------------------
// DEBUG-ONLY QA-шов (Phase 4): запуск видео без тап-автоматизации
// ---------------------------------------------------------------------------

/**
 * Скриншот-проверка плеера без UI-автоматизации: `simctl launch <udid> <bundle> -koch-media-probe`.
 * Второй необязательный аргумент выбирает ролик: `umbrella` (1280×1000, луп) или
 * `slippers` (720×720); по умолчанию `ad_lion.mp4` (540×720).
 *
 * Работает ТОЛЬКО в debug-сборке ([isDebugBuild]) и только при явном аргументе:
 * обычный запуск и release не затрагиваются. Выключение — просто не передавать аргумент
 * (или удалить этот блок и ветку в iosApp/iosApp/iOSApp.swift).
 */
private const val MEDIA_PROBE_ARG = "-koch-media-probe"

private data class MediaProbeRequest(
    val mediaKey: String,
    val aspect: Float,
    val isLooping: Boolean
)

private fun mediaProbeRequest(): MediaProbeRequest? {
    if (!isDebugBuild()) return null
    val args = NSProcessInfo.processInfo.arguments.mapNotNull { it as? String }
    val index = args.indexOf(MEDIA_PROBE_ARG)
    if (index < 0) return null
    return when (args.getOrNull(index + 1)) {
        "umbrella" -> MediaProbeRequest("umbrella_face.mp4", 1.28f, isLooping = true)
        "slippers" -> MediaProbeRequest("ad_slippers.mp4", 1.0f, isLooping = false)
        else -> MediaProbeRequest("ad_lion.mp4", 0.75f, isLooping = false)
    }
}

/**
 * Точка входа шва для Swift-хоста: null — обычный запуск (MainViewController),
 * иначе полноэкранный [PlatformVideo] с выбранным роликом.
 */
fun mediaProbeViewController(): UIViewController? {
    val request = mediaProbeRequest() ?: return null
    LOG("probe screen: ${request.mediaKey} looping=${request.isLooping}")
    return ComposeUIViewController {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlatformVideo(
                    mediaKey = request.mediaKey,
                    aspect = request.aspect,
                    modifier = Modifier.fillMaxSize().aspectRatio(request.aspect),
                    isLooping = request.isLooping
                )
            }
        }
    }
}
