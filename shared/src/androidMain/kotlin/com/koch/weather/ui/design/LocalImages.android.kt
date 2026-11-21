package com.koch.weather.ui.design

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.koch.weather.platform.AndroidPlatform

/**
 * Android-ключ drawable-ресурса → R.drawable. Полный набор картинок UI:
 * фоны тем, их превью, картинки-советы и фото-креативы рекламы.
 * Расширения не включены — ключи совпадают с AdCreative.mediaKey.
 */
private val LocalDrawableResIds = mapOf(
    "theme_default" to com.koch.weather.R.drawable.theme_default,
    "theme_copper_sunset" to com.koch.weather.R.drawable.theme_copper_sunset,
    "theme_mars" to com.koch.weather.R.drawable.theme_mars,
    "theme_cyberpunk" to com.koch.weather.R.drawable.theme_cyberpunk,
    "theme_aurora_borealis" to com.koch.weather.R.drawable.theme_aurora_borealis,
    "theme_default_preview" to com.koch.weather.R.drawable.theme_default_preview,
    "theme_copper_sunset_preview" to com.koch.weather.R.drawable.theme_copper_sunset_preview,
    "theme_mars_preview" to com.koch.weather.R.drawable.theme_mars_preview,
    "theme_cyberpunk_preview" to com.koch.weather.R.drawable.theme_cyberpunk_preview,
    "theme_bloody_sunset_preview" to com.koch.weather.R.drawable.theme_bloody_sunset_preview,
    "theme_aurora_borealis_preview" to com.koch.weather.R.drawable.theme_aurora_borealis_preview,
    "advice_1" to com.koch.weather.R.drawable.advice_1,
    "advice_2" to com.koch.weather.R.drawable.advice_2,
    "advice_3" to com.koch.weather.R.drawable.advice_3,
    "ad_photo1" to com.koch.weather.R.drawable.ad_photo1,
    "ad_photo2" to com.koch.weather.R.drawable.ad_photo2,
    "ad_slippers" to com.koch.weather.R.drawable.ad_slippers,
    "ad_son" to com.koch.weather.R.drawable.ad_son
)

/**
 * Android-actual: `BitmapFactory.decodeResource` в RGB_565 для фонов тем (1:1 с
 * прежним KochBackground — 2 байта/пиксель, фото затемнено скримом, квантование
 * не видно) и ARGB_8888 для остальных (1:1 с прежним AdviceImage/painterResource);
 * `inScaled = false` (drawable-nodpi: без масштабирования под плотность);
 * `inSampleSize = 2` при ширине больше [sampleIfWiderThan] (1:1 с AdviceImage).
 */
actual fun decodeLocalDrawable(key: String, sampleIfWiderThan: Int): ImageBitmap? {
    val resId = LocalDrawableResIds[key] ?: return null
    val resources = AndroidPlatform.appContext?.resources ?: return null
    return runCatching {
        var options = BitmapFactory.Options().apply { inScaled = false }
        if (sampleIfWiderThan != Int.MAX_VALUE) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(resources, resId, bounds)
            options = options.apply {
                inSampleSize = if (bounds.outWidth > sampleIfWiderThan) 2 else 1
            }
        }
        if (key.startsWith("theme_")) options.inPreferredConfig = Bitmap.Config.RGB_565
        BitmapFactory.decodeResource(resources, resId, options)?.asImageBitmap()
    }.getOrNull()
}
