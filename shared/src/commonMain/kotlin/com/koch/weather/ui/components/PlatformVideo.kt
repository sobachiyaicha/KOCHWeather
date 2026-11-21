package com.koch.weather.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Платформенный видеоплеер без контролов, паузы и перемотки (port plan §6).
 *
 * [mediaKey] — имя файла: Android — `res/raw/<имя без расширения>` (расширение
 * режет platform.mediaUrl), iOS — `compose-resources/files/<полное имя>`
 * (фаза 2: файлы ещё не переехали — путь расчётный, играет media-engineer в фазе 4).
 *
 * [aspect] — пропорции исходника, захардкоженные (port plan §6: вычисление через
 * AVAsset async запрещено): `ad_lion` 540×720 → 0.75, `ad_slippers` 720×720 → 1.0,
 * `umbrella_face` 1280×1000 → 1.28.
 *
 * Звук обязателен: Android — USAGE_MEDIA/CONTENT_TYPE_MOVIE (1:1 с исходником),
 * iOS — AVAudioSession .playback + setActive(true).
 * Уход в фон — пауза, возврат — продолжение с того же места.
 * Жизненный цикл плеера — ровно на время композиции: открытие диалога/листа
 * создаёт плеер заново, ролик начинается с нуля (1:1 с исходником).
 */
@Composable
expect fun PlatformVideo(
    mediaKey: String,
    aspect: Float,
    modifier: Modifier = Modifier,
    isLooping: Boolean = false,
    onEnded: (() -> Unit)? = null
)
