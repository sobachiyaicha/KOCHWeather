package com.koch.weather.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * iOS-actual шрифтового шва (см. commonMain `Tokens.kt`).
 *
 * Inter (SIL OFL 1.1, лицензия — `androidApp/src/main/assets/fonts/Inter-OFL.txt`),
 * 4 статических начертания — те же файлы, что у Android: единственная копия в репо
 * `shared/src/androidMain/res/font` (4 файла .ttf), упаковка в бандл — `scripts/ios-build.sh`
 * (`Payload.app/fonts/`).
 *
 * Декод не нужен: `Font(identity, data, weight)` принимает TTF байтами напрямую
 * (Compose Multiplatform), поэтому Compose Resources для шрифтов не требуется.
 * Если файла нет — честный фолбэк на системный шрифт, чтобы приложение не падало.
 */
@OptIn(ExperimentalForeignApi::class)
private fun loadInter(name: String, weight: FontWeight): Font? {
    val bundle = NSBundle.mainBundle
    val path = bundle.pathForResource(name, ofType = "ttf", inDirectory = "fonts") ?: return null
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val size = data.length.toInt()
    if (size <= 0) return null
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    // Фабрика байтового Font живёт в androidx.compose.ui.text.platform (skiko/native):
    // у неё то же имя, что у типа, поэтому вызов — полным именем.
    return runCatching {
        androidx.compose.ui.text.platform.Font(identity = name, data = bytes, weight = weight)
    }.getOrNull()
}

actual val KochFontFamily: FontFamily = run {
    val fonts = listOfNotNull(
        loadInter("inter_regular", FontWeight.Normal),
        loadInter("inter_medium", FontWeight.Medium),
        loadInter("inter_semibold", FontWeight.SemiBold),
        loadInter("inter_bold", FontWeight.Bold),
        loadInter("inter_bold", FontWeight.ExtraBold)
    )
    if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
}
