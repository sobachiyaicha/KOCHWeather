package com.koch.weather.ui.components

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.AnyRes
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Одноразовый ролик «+1 попытка» для листа лутбокса.
 *
 * Почему отдельный плеер, а не [AdVideoEngine]. Постоянная выходная поверхность
 * промо-оверлея — единственная на процесс: если отдать её кодеку другой `View`,
 * придётся менять выход (`MediaCodec.setOutputSurface`), а на realme RMX3151
 * (Android 12, MediaTek) `CCodec` при каждой смене поверхности ждёт
 * несуществующий `AtlasService` ~5 с. Поэтому награда в лутбоксе играет своим
 * короткоживущим `ExoPlayer` в своём окне: она не трогает поверхность и кодек
 * промо-рекламы, а освобождается вместе с окном (`clearVideoSurface()` до
 * `release()`, чтобы `MediaCodec.setOutputSurface` не получил уже освобождённую
 * `Surface`).
 *
 * Контролов, жестов и индикаторов загрузки нет: чёрный кадр, `Fit` без обрезки,
 * звук включён. По концу ролика — [onEnded] (начисление попытки и автовозврат),
 * закрытие окна и системный «назад» остаются за вызывающим.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun AdVideoPlayer(
    @AnyRes videoRes: Int,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnEnded by rememberUpdatedState(onEnded)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val player = remember(videoRes) {
        ExoPlayer.Builder(context).build().apply {
            volume = 1f
            repeatMode = Player.REPEAT_MODE_OFF
            setMediaItem(
                MediaItem.fromUri(
                    Uri.parse("android.resource://${context.packageName}/$videoRes")
                )
            )
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_ENDED) return
                // Колбэк плеера не должен разрушать окно внутри себя: сначала
                // возвращаемся в цикл main, потом отдаём конец ролика наружу.
                mainHandler.post { currentOnEnded() }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            mainHandler.removeCallbacksAndMessages(null)
            // Порядок строгий: поверхность отвязываем до release().
            runCatching { player.clearVideoSurface() }
            runCatching { player.release() }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                // Индикаторов загрузки нет: до первого кадра — чёрный кадр.
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                this.player = player
            }
        },
        onRelease = { view ->
            runCatching { view.player = null }
        }
    )
}
