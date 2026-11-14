package com.koch.weather.ui.design

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.LocalKochColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Фон приложения.
 *
 * 1) База — градиент темы (всегда видим: пока фото нет, офлайн, а также у тем без фона).
 * 2) Если тема даёт локальный ресурс ([imageRes], `AppThemeMode.backgroundRes`) — фото рисуется
 *    поверх градиента с `Crop`. Это ЛОКАЛЬНЫЙ файл `res/drawable-nodpi/theme_*.jpg`
 *    (1080×1920, заранее размытый и затемнённый), поэтому фон не зависит ни от сети, ни от Coil.
 *    Битмап декодируется через `BitmapFactory` в RGB_565 вне UI-потока и живёт в LRU-кеше
 *    на 2 записи ([ThemeBackgroundCache]) — `painterResource` здесь больше не используется,
 *    потому что кеш ресурсов навсегда оставлял в памяти все посещённые темы.
 * 3) Над фото — скрим 40/30/48 %, чтобы белый текст и стекло читались на светлых фото,
 *    плюс отдельный верхний скрим под статус-бар.
 * 4) Без фото — процедурные облака (дешёвые овалы, без blur).
 */
@Composable
fun KochSkyBackground(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    showClouds: Boolean = true
) {
    val colors = LocalKochColors.current
    val hasPhoto = imageRes != null
    val photo = rememberThemeBackground(imageRes)

    Box(modifier = modifier.fillMaxSize().background(colors.gradient)) {
        // Облака — фолбэк для тем без локального фона.
        if (!hasPhoto && showClouds) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCloudLayer(colors.bgTop, colors.bgBottom)
            }
        }

        if (imageRes != null) {
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
 * декодируется на `Dispatchers.IO`. Пока новый кадр готовится, в состоянии остаётся
 * предыдущий (`produceState` не сбрасывает значение при смене ключа), поэтому тема
 * меняется без мигания градиентом. `ImageBitmap` оборачивается через `remember`,
 * чтобы не пересоздавать обёртку на каждую рекомпозицию.
 */
@Composable
private fun rememberThemeBackground(@DrawableRes resId: Int?): ImageBitmap? {
    val resources = LocalContext.current.resources
    val bitmap by produceState(
        initialValue = if (resId == null) {
            null
        } else {
            ThemeBackgroundCache.get(resId) ?: ThemeBackgroundCache.shownOrNull()
        },
        key1 = resId
    ) {
        if (resId == null) {
            value = null
            return@produceState
        }
        ThemeBackgroundCache.get(resId)?.let { cached ->
            value = cached
            return@produceState
        }
        val decoded = decodeThemeBackground(resources, resId)
        // Нет ресурса или декод не удался: остаётся градиент темы.
        value = decoded?.let { ThemeBackgroundCache.put(resId, it) }
    }

    // Пока кадр в композиции, кеш не имеет права его recycle: смена темы не должна
    // освобождать битмап, который ещё рисуется этим экраном. Вытесненные «занятые»
    // кадры попадают в очередь и освобождаются в onDispose.
    DisposableEffect(bitmap) {
        bitmap?.let(ThemeBackgroundCache::acquire)
        onDispose { bitmap?.let(ThemeBackgroundCache::release) }
    }

    return remember(bitmap) { bitmap?.asImageBitmap() }
}

/**
 * Декод полного фона в RGB_565. Вызывать только с `Dispatchers.IO`: 1080×1920 нельзя
 * декодировать на UI-потоке (perf-qa: синхронный декод 8.3 МБ давал тяжёлый кадр
 * при смене темы). Ошибка/OOM не роняет экран — вернётся null, покажется градиент.
 */
private suspend fun decodeThemeBackground(
    resources: Resources,
    @DrawableRes resId: Int
): Bitmap? = withContext(Dispatchers.IO) {
    val options = BitmapFactory.Options().apply {
        // 2 байта на пиксель вместо 4: 1080×1920 = 4.15 МБ вместо 8.3 МБ.
        // Фото затемнено скримом 40–48 %, поэтому квантование 565 визуально не заметно.
        inPreferredConfig = Bitmap.Config.RGB_565
        // drawable-nodpi: при декоде фон не масштабируется под плотность экрана.
        inScaled = false
        inMutable = false
    }
    val bitmap = runCatching {
        BitmapFactory.decodeResource(resources, resId, options)
    }.getOrNull()
    // Пока декодировали, тему успели сменить — битмап уже никому не нужен.
    if (bitmap != null && !coroutineContext.isActive) {
        bitmap.recycle()
        null
    } else {
        bitmap
    }
}

/** Сколько полных фонов темы держим одновременно: текущий и предыдущий. */
private const val BACKGROUND_CACHE_ENTRIES = 2

/**
 * Кеш полноразмерных фонов темы.
 *
 * Почему не `painterResource`: он кладёт `BitmapDrawable` в кеш `Resources`, и каждый
 * посещённый фон остаётся в памяти до конца процесса (замерено: +8 МБ PSS и +8 МБ
 * GPU-текстуры за смену темы). `BitmapFactory.decodeResource` кеш ресурсов не трогает,
 * поэтому объём памяти ограничиваем сами: две записи по ключу `resId`.
 *
 * Рециклинг: вытесненный `Bitmap` освобождается сразу, только если композиция его
 * не рисует ([inUse] ведётся через `DisposableEffect` в [rememberThemeBackground]).
 * Иначе он попадает в [pendingRecycle] и освобождается в момент, когда экран от него
 * отказался, — это страховка от «trying to use a recycled bitmap» на смене темы.
 */
private object ThemeBackgroundCache {
    private val cache = object : LruCache<Int, Bitmap>(BACKGROUND_CACHE_ENTRIES) {
        override fun sizeOf(key: Int, value: Bitmap): Int = 1

        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (oldValue !== newValue) recycleIfUnused(oldValue)
        }
    }

    /** Кадры, которые сейчас рисует композиция; их нельзя recycle. */
    private val inUse = mutableSetOf<Bitmap>()

    /** Вытесненные, но ещё рисуемые кадры: освобождаем после выхода из композиции. */
    private val pendingRecycle = mutableSetOf<Bitmap>()

    /** Последний кадр композиции — показывается, пока декодируется новая тема. */
    private var shown: Bitmap? = null

    @Synchronized
    fun get(resId: Int): Bitmap? = cache.get(resId)?.takeUnless { it.isRecycled }

    /** Кладёт свежий декод; вытесненный кадр освобождается, если он не на экране. */
    @Synchronized
    fun put(resId: Int, bitmap: Bitmap): Bitmap {
        cache.put(resId, bitmap)
        return bitmap
    }

    /** Кадр, который сейчас на экране: подставляется, пока декодируется новый. */
    @Synchronized
    fun shownOrNull(): Bitmap? = shown?.takeUnless { it.isRecycled }

    /** Композиция начала рисовать [bitmap]. */
    @Synchronized
    fun acquire(bitmap: Bitmap) {
        inUse.add(bitmap)
        shown = bitmap
    }

    /** Композиция больше не рисует [bitmap]; если он был вытеснен — освобождаем. */
    @Synchronized
    fun release(bitmap: Bitmap) {
        inUse.remove(bitmap)
        if (shown === bitmap) shown = null
        if (pendingRecycle.remove(bitmap) && !bitmap.isRecycled) bitmap.recycle()
    }

    @Synchronized
    private fun recycleIfUnused(bitmap: Bitmap) {
        when {
            bitmap in inUse -> pendingRecycle.add(bitmap)
            !bitmap.isRecycled -> bitmap.recycle()
        }
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
