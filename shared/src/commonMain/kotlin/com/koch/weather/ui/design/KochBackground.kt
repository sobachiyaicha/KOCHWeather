package com.koch.weather.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.LocalKochColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Фон приложения.
 *
 * 1) База — градиент темы (всегда видим: пока фото нет, офлайн, а также у тем без фона).
 * 2) Если тема даёт локальный ресурс ([imageKey], `AppThemeMode.defaultBackgroundKey`:
 *    у темы по умолчанию — `theme_default.jpg` без блюра, у остальных —
 *    `AppThemeMode.backgroundKey`) — фото рисуется
 *    поверх градиента с `Crop`. Это ЛОКАЛЬНЫЙ файл `res/drawable-nodpi/theme_*.jpg`
 *    (1080×1920/2323, фото с размытием 4 px и зерном), поэтому фон не зависит ни от сети, ни от Coil.
 *    Битмап декодируется через `platform.decodeImage` вне UI-потока и живёт в LRU-кеше
 *    на 2 записи ([ThemeBackgroundCache]) — `painterResource` здесь больше не используется,
 *    потому что кеш ресурсов навсегда оставлял в памяти все посещённые темы.
 * 3) Над фото — скрим 40/30/48 %, чтобы белый текст и стекло читались на светлых фото,
 *    плюс отдельный верхний скрим под статус-бар.
 * 4) Без фото — процедурные облака (дешёвые овалы, без blur).
 *
 * Отличия от Android-исходника (фаза 2, KMP):
 *  - вместо `@DrawableRes Int` — ключ файла ([decodeLocalDrawable]): composeResources
 *    приезжают в фазе 3, Android читает res/drawable-nodpi как раньше;
 *  - `ImageBitmap` не recycle'ится (общий тип без платформенного recycle): вытесненные
 *    кадры отдаются GC — на API 26+ нативная память Bitmap'ов учитывается GC'ем;
 *    страховка [acquire]/[release] от «recycled bitmap» больше не нужна;
 *  - декод идёт на `Dispatchers.Default` (Dispatchers.IO internal в common);
 *    тяжёлый декод по-прежнему вне UI-потока (perf-qa).
 */
@Composable
fun KochSkyBackground(
    modifier: Modifier = Modifier,
    imageKey: String? = null,
    showClouds: Boolean = true
) {
    val colors = LocalKochColors.current
    val hasPhoto = imageKey != null
    val photo = rememberThemeBackground(imageKey)

    Box(modifier = modifier.fillMaxSize().background(colors.gradient)) {
        // Облака — фолбэк для тем без локального фона.
        if (!hasPhoto && showClouds) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCloudLayer(colors.bgTop, colors.bgBottom)
            }
        }

        if (imageKey != null) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Скрим поверх фото под белый текст: 40 % сверху, 30 % в середине, 48 % снизу.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x66000000),
                                Color(0x4D000000),
                                Color(0x7A000000)
                            )
                        )
                    )
            )
            // Отдельный верхний скрим под статус-бар: белые иконки читаются на любом фото.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x73000000), Color(0x00000000))
                        )
                    )
            )
        }
    }
}

/**
 * Полный фон темы для композиции. Попадание в кеш отдаётся без декода, промах —
 * декодируется на `Dispatchers.Default`. Пока новый кадр готовится, в состоянии остаётся
 * предыдущий (`produceState` не сбрасывает значение при смене ключа), поэтому тема
 * меняется без мигания градиентом.
 */
@Composable
private fun rememberThemeBackground(key: String?): ImageBitmap? {
    val bitmap by produceState(
        initialValue = if (key == null) {
            null
        } else {
            ThemeBackgroundCache.get(key) ?: ThemeBackgroundCache.shownOrNull()
        },
        key1 = key
    ) {
        if (key == null) {
            value = null
            return@produceState
        }
        ThemeBackgroundCache.get(key)?.let { cached ->
            value = cached
            return@produceState
        }
        val decoded = decodeThemeBackground(key)
        // Нет ресурса или декод не удался: остаётся градиент темы.
        value = decoded?.let { ThemeBackgroundCache.put(key, it) }
    }

    // Последний показанный кадр запоминается в кеше: он подставляется, пока
    // декодируется новая тема (аналог прежнего DisposableEffect acquire/release).
    DisposableEffect(bitmap) {
        bitmap?.let(ThemeBackgroundCache::acquire)
        onDispose { }
    }

    return bitmap
}

/**
 * Декод полного фона. Вызывать только с `Dispatchers.Default`: 1080×1920 нельзя
 * декодировать на UI-потоке (perf-qa: синхронный декод давал тяжёлый кадр
 * при смене темы). Ошибка/OOM не роняет экран — вернётся null, покажется градиент.
 * RGB_565/inScaled-опции прежнего BitmapFactory-декода сохранены в Android-actual
 * шва [decodeLocalDrawable].
 */
private suspend fun decodeThemeBackground(key: String): ImageBitmap? = withContext(Dispatchers.Default) {
    val bitmap = runCatching { decodeLocalDrawable(key) }.getOrNull()
    // Пока декодировали, тему успели сменить — кадр уже никому не нужен.
    if (bitmap != null && !coroutineContext.isActive) null else bitmap
}

/** Сколько полных фонов темы держим одновременно: текущий и предыдущий. */
private const val BACKGROUND_CACHE_ENTRIES = 2

/**
 * Кеш полноразмерных фонов темы (копия прежнего LruCache, без recycle):
 *
 * Почему не `painterResource`: он кладёт кадр в кеш ресурсов, и каждый посещённый
 * фон остаётся в памяти до конца процесса (+8 МБ PSS и +8 МБ GPU-текстуры за смену
 * темы на Android). Объём памяти ограничиваем сами: две записи по ключу файла.
 * Вытесненные кадры отдаётся GC (см. доку [KochSkyBackground] про KMP-отличия).
 */
@OptIn(ExperimentalAtomicApi::class)
private object ThemeBackgroundCache {
    private val cache = AtomicReference<Map<String, ImageBitmap>>(emptyMap())

    /** Последний кадр композиции — показывается, пока декодируется новая тема. */
    private val shown = AtomicReference<ImageBitmap?>(null)

    fun get(key: String): ImageBitmap? = cache.load()[key]

    /** Кладёт свежий декод; при переполнении вытесняется самая старая запись. */
    fun put(key: String, bitmap: ImageBitmap): ImageBitmap {
        while (true) {
            val current = cache.load()
            val next = (current + (key to bitmap)).let { entries ->
                if (entries.size > BACKGROUND_CACHE_ENTRIES) {
                    // LinkedHashMap (+ сохраняет порядок вставки): вытесняем самые старые.
                    entries.entries.drop(entries.size - BACKGROUND_CACHE_ENTRIES)
                        .associate { it.toPair() }
                } else {
                    entries
                }
            }
            if (cache.compareAndSet(current, next)) return bitmap
        }
    }

    /** Кадр, который сейчас на экране: подставляется, пока декодируется новый. */
    fun shownOrNull(): ImageBitmap? = shown.load()

    /** Композиция начала рисовать [bitmap]. */
    fun acquire(bitmap: ImageBitmap) {
        shown.store(bitmap)
    }
}

private fun DrawScope.drawCloudLayer(top: Color, bottom: Color) {
    val w = size.width
    val h = size.height
    val topLum = top.luminanceLike()
    val softTop = Color.White.copy(alpha = if (topLum > 0.6f) 0.10f else 0.06f)
    val softMid = Color.White.copy(alpha = 0.05f)
    val shade = Color.Black.copy(alpha = if (bottom.luminanceLike() > 0.5f) 0.03f else 0.05f)

    fun puff(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = Offset(cx, cy),
                radius = maxOf(rx, ry)
            ),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f)
        )
    }

    // Тёплый «солнечный» блик сверху: фото-подобный перепад вместо плоской заливки.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF4E0).copy(alpha = if (topLum > 0.6f) 0.10f else 0.22f),
                Color.Transparent
            ),
            center = Offset(w * 0.72f, h * 0.06f),
            radius = w * 0.75f
        ),
        topLeft = Offset(0f, -h * 0.20f),
        size = Size(w * 1.5f, h * 0.55f)
    )

    puff(w * 0.18f, h * 0.10f, w * 0.62f, h * 0.16f, softTop)
    puff(w * 0.78f, h * 0.16f, w * 0.55f, h * 0.13f, softMid)
    puff(w * 0.46f, h * 0.30f, w * 0.80f, h * 0.18f, shade)
    puff(w * 0.12f, h * 0.42f, w * 0.50f, h * 0.12f, softMid)
    puff(w * 0.66f, h * 0.55f, w * 0.60f, h * 0.10f, shade)

    drawRect(
        brush = Brush.verticalGradient(
            0.62f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.20f),
            startY = 0f,
            endY = h
        )
    )
}

private fun Color.luminanceLike(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
