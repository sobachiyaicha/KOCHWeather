package com.koch.weather.ui.design

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * iOS-actual шва decodeLocalDrawable (см. commonMain `LocalImages.kt`).
 *
 * Картинки берутся из бандла приложения: `Payload.app/images/<key>.jpg`
 * (упаковка — `scripts/ios-build.sh`, единственная копия в репо —
 * `shared/src/androidMain/res/drawable-nodpi`, как и у видео).
 *
 * Декод — Skia (тот же движок, что у Compose): байты файла → Image.makeFromEncoded.
 * UIKit не нужен, поэтому шов работает и в симуляторе, и на устройстве.
 *
 * [sampleIfWiderThan] пока не используется: полноразмерный декод 19 jpg (фоны тем,
 * превью, фото-креативы) укладывается в память; уменьшение через Skia — оптимизация
 * Phase 7 (performance-engineer), контракт шва при этом не меняется.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun decodeLocalDrawable(key: String, sampleIfWiderThan: Int): ImageBitmap? {
    val bundle = NSBundle.mainBundle
    val path = bundle.pathForResource(key, ofType = "jpg", inDirectory = "images")
        ?: bundle.pathForResource(key, ofType = "png", inDirectory = "images")
        ?: return null
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val size = data.length.toInt()
    if (size <= 0) return null
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
}
