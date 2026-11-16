package com.koch.weather.ui.components

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.backgroundRes
import com.koch.weather.data.model.previewRes
import com.koch.weather.ui.theme.kochColors

/**
 * Превью темы градиентом — только фолбэк для тем без локального ресурса
 * (единый источник — kochColors, поэтому у каждой темы свой перепад).
 */
internal fun themePreviewBrush(theme: AppThemeMode): Brush {
    val colors = kochColors(theme)
    return Brush.verticalGradient(
        listOf(colors.bgTop, colors.bgUpper, colors.bgLower, colors.bgBottom)
    )
}

/** Основной цвет темы — для подложки/подсветки, когда нужен один цвет, а не градиент. */
internal fun themeAccentColor(theme: AppThemeMode): Color = kochColors(theme).bgLower

/**
 * Во сколько раз уменьшать превью: 540×960 / 2 = 270×480 (≈ 0.5 МБ ARGB_8888).
 * Полный кадр (8.3 МБ) для карточки 160×104 dp не нужен — раньше 7 тем давали
 * пик ~58 МБ на экране «Вид» (M4 код-ревью Фазы 5). Сейчас тем 6, потолок тот же.
 */
private const val PREVIEW_SAMPLE_SIZE = 2

/**
 * Кеш уменьшенных превью: не больше 8 записей (6 тем + запас), каждая ≈ 0.5 МБ,
 * то есть потолок ~4 МБ. Полный кадр 1080×1920 сюда не попадает и в кеше ресурсов
 * не остаётся. Живёт вне композиции, поэтому прокрутка галереи и листание стрелками
 * в лутбоксе не декодируют битмап заново.
 */
private object ThemePreviewCache {
    private const val MAX_ENTRIES = 8

    private val cache = object : LruCache<Int, ImageBitmap>(MAX_ENTRIES) {
        override fun sizeOf(key: Int, value: ImageBitmap): Int = 1
    }

    fun get(resId: Int): ImageBitmap? = cache.get(resId)

    fun put(resId: Int, bitmap: ImageBitmap) {
        cache.put(resId, bitmap)
    }
}

/**
 * Декодирует уменьшенную копию локального фона темы. `BitmapFactory` вместо
 * `painterResource`, потому что только он умеет `inSampleSize`: painterResource
 * тянет полный битмап 540×960 и оставляет его в кеше ресурсов. `inScaled = false`
 * не даёт декодеру дополнительно масштабировать кадр под плотность экрана
 * (`drawable-nodpi`). Ошибка/`OOM` не роняет экран — вызывающий откатывается на градиент.
 */
private fun decodeSampledPreview(resources: Resources, resId: Int): ImageBitmap? =
    runCatching {
        val options = BitmapFactory.Options().apply {
            inSampleSize = PREVIEW_SAMPLE_SIZE
            inScaled = false
        }
        BitmapFactory.decodeResource(resources, resId, options)?.asImageBitmap()
    }.getOrNull()

/**
 * Локальный фон темы для ПРЕВЬЮ (карточка галереи, мокап телефона, билет):
 * уменьшенная копия `theme.previewRes` (фолбэк — `theme.backgroundRes`) из кеша
 * (≤ 8 записей), без сети и Coil.
 *
 * Для полноэкранного фона главной используется `KochSkyBackground` с собственным
 * LRU-кешем полных битмапов; здесь — только уменьшенные превью.
 */
@Composable
internal fun ThemePreviewImage(
    theme: AppThemeMode,
    modifier: Modifier = Modifier
) {
    val res = theme.previewRes ?: theme.backgroundRes
    if (res == null) {
        Box(modifier = modifier.background(themePreviewBrush(theme)).fillMaxSize())
        return
    }
    val resources = LocalContext.current.resources
    val image = remember(res) {
        ThemePreviewCache.get(res) ?: decodeSampledPreview(resources, res)?.also {
            ThemePreviewCache.put(res, it)
        }
    }
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Декод не удался: градиент темы. Полный ресурс здесь не грузим — иначе превью
        // снова оставляло бы 1080×1920 в кеше ресурсов (дефект памяти при смене темы).
        Box(modifier = modifier.background(themePreviewBrush(theme)).fillMaxSize())
    }
}
