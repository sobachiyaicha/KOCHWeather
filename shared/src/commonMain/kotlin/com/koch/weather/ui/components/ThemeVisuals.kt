package com.koch.weather.ui.components

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
import androidx.compose.ui.layout.ContentScale
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.ui.design.backgroundKey
import com.koch.weather.ui.design.decodeLocalDrawable
import com.koch.weather.ui.design.previewKey
import com.koch.weather.ui.theme.kochColors
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

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
 * В шве [decodeLocalDrawable] аналог inSampleSize=2 включается границей ширины.
 */
private const val PREVIEW_SAMPLE_THRESHOLD = 0

/**
 * Кеш уменьшенных превью: не больше 8 записей (6 тем + запас), каждая ≈ 0.5 МБ,
 * то есть потолок ~4 МБ. Полный кадр 1080×1920 сюда не попадает. Живёт вне
 * композиции, поэтому прокрутка галереи и листание стрелками в лутбоксе не
 * декодируют битмап заново.
 * Замена LruCache (KMP): copy-on-write Map через AtomicReference (вытеснение —
 * самые старые записи, порядок вставки сохраняется).
 */
@OptIn(ExperimentalAtomicApi::class)
private object ThemePreviewCache {
    private const val MAX_ENTRIES = 8

    private val cache = AtomicReference<Map<String, ImageBitmap>>(emptyMap())

    fun get(key: String): ImageBitmap? = cache.load()[key]

    fun put(key: String, bitmap: ImageBitmap) {
        while (true) {
            val current = cache.load()
            if (current[key] != null) return
            val next = (current + (key to bitmap)).let { entries ->
                if (entries.size > MAX_ENTRIES) {
                    entries.entries.drop(entries.size - MAX_ENTRIES).associate { it.toPair() }
                } else {
                    entries
                }
            }
            if (cache.compareAndSet(current, next)) return
        }
    }
}

/**
 * Декодирует уменьшенную копию локального фона темы. Шов [decodeLocalDrawable]
 * вместо `BitmapFactory`: он умеет `inSampleSize` (аналог прежнего поведения —
 * painterResource тянет полный битмап 540×960 и оставляет его в кеше ресурсов).
 * `inScaled = false` не даёт декодеру дополнительно масштабировать кадр под
 * плотность экрана (drawable-nodpi; Android-actual шва). Ошибка/`OOM` не роняет
 * экран — вызывающий откатывается на градиент.
 */
private fun decodeSampledPreview(key: String): ImageBitmap? =
    runCatching { decodeLocalDrawable(key, PREVIEW_SAMPLE_THRESHOLD) }.getOrNull()

/**
 * Локальный фон темы для ПРЕВЬЮ (карточка галереи, мокап телефона, билет):
 * уменьшенная копия `theme.previewKey` (фолбэк — `theme.backgroundKey`) из кеша
 * (≤ 8 записей), без сети и Coil.
 *
 * Для полноэкранного фона главной используется `KochSkyBackground` с собственным
 * кешем полных битмапов; здесь — только уменьшенные превью.
 */
@Composable
internal fun ThemePreviewImage(
    theme: AppThemeMode,
    modifier: Modifier = Modifier
) {
    val key = theme.previewKey ?: theme.backgroundKey
    if (key == null) {
        Box(modifier = modifier.background(themePreviewBrush(theme)).fillMaxSize())
        return
    }
    val image = remember(key) {
        ThemePreviewCache.get(key) ?: decodeSampledPreview(key)?.also {
            ThemePreviewCache.put(key, it)
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
