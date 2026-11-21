package com.koch.weather.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import androidx.compose.runtime.remember

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
    if (fallback != null) {
        SubcomposeAsyncImage(
            model = url,
            imageLoader = wikimediaImageLoader(),
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
            imageLoader = wikimediaImageLoader(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.clip(shape),
            placeholder = placeholder,
            error = placeholder
        )
    }
}
