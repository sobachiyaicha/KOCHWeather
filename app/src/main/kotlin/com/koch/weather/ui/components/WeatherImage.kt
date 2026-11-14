package com.koch.weather.ui.components

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import com.koch.weather.data.model.ImageCatalog
import okhttp3.OkHttpClient

/**
 * Coil image loader for the real Wikimedia Commons photos (R17).
 *
 * upload.wikimedia.org answers 429 to the default empty User-Agent, so every request carries
 * [ImageCatalog.WIKIMEDIA_USER_AGENT]. The loader is a process-wide singleton: one OkHttp client
 * and one memory/disk cache for all screens.
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
                .crossfade(true)
                .build()
            instance = loader
            return loader
        }
    }
}

/**
 * Network photo with mandatory loading and error placeholders (no emoji, no heavy blur).
 * Cropped to fill the slot, which is what the card/thumbnail layouts need.
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

    SubcomposeAsyncImage(
        model = url,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clip(shape),
        loading = { fallback?.invoke() ?: PhotoPlaceholder(placeholderIcon, placeholderTint) },
        error = { fallback?.invoke() ?: PhotoPlaceholder(placeholderIcon, placeholderTint) }
    )
}

@Composable
private fun PhotoPlaceholder(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x33000000)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
