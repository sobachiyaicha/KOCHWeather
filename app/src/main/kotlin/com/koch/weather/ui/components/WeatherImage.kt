package com.koch.weather.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.koch.weather.data.model.ImageCatalog
import okhttp3.OkHttpClient

/**
 * Coil image loader for the real Wikimedia Commons photos (R17).
 *
 * upload.wikimedia.org answers 429 to the default empty User-Agent, so every request carries
 * [ImageCatalog.WIKIMEDIA_USER_AGENT]. The loader is a process-wide singleton: one OkHttp client
 * and one memory/disk cache for all screens. Crossfade выключен: лишний слой на каждый кадр.
 */
private object WikimediaImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        instance?.let { return it }
        synchronized(this) {
            instance?.let { return it }
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", ImageCatalog.WIKIMEDIA_USER_AGENT)
                            .build()
                    )
                }
                .build()
            val loader = ImageLoader.Builder(context.applicationContext)
                .okHttpClient(client)
                .crossfade(false)
                .build()
            instance = loader
            return loader
        }
    }
}

/**
 * Сетевое фото.
 *
 * Быстрый путь (по умолчанию) — `AsyncImage` без сабкомпозиции: только заливка-плейсхолдер.
 * Медленный (только там, где нужен составной фолбэк, например мокап телефона в рулетке) —
 * `SubcomposeAsyncImage` с переданным [fallback].
 */
@Composable
fun RemotePhoto(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(12.dp),
    placeholderIcon: ImageVector = Icons.Default.Image,
    placeholderTint: Color = Color.White.copy(alpha = 0.6f),
    fallback: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { WikimediaImageLoader.get(context) }

    if (fallback != null) {
        SubcomposeAsyncImage(
            model = url,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.clip(shape),
            loading = { fallback.invoke() },
            error = { fallback.invoke() }
        )
    } else {
        val placeholder = remember { ColorPainter(Color(0x33FFFFFF)) }
        AsyncImage(
            model = url,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.clip(shape),
            placeholder = placeholder,
            error = placeholder
        )
    }
}
