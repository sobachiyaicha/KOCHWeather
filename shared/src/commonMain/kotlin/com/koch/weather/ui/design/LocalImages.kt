package com.koch.weather.ui.design

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Шов локальных drawable-картинок (владелец — ui-porter, фаза 2).
 *
 * Ключ — имя ресурса без расширения (1:1 с `AdCreative.mediaKey`: «ad_photo1»,
 * «advice_1», «theme_default»). Замена `painterResource(R.drawable.key)`/
 * `BitmapFactory.decodeResource`: у обоих нет KMP-аналога до переезда ресурсов
 * в composeResources (фаза 3, resources-porter — тогда шов заменяется на
 * `painterResource(Res.drawable.*)`/`Res.readBytes`).
 *
 * [sampleIfWiderThan] — ширина исходника в px, выше которой картинка декодируется
 * в половинном размере (`inSampleSize = 2`, эквивалент прежнего AdviceImage).
 * Фон-блокировка по границе используется точечно, у фонов тем — [Int.MAX_VALUE].
 *
 * null — на платформе нет такого ресурса (iOS до фазы 3: файлы ещё не в
 * composeResources) или декод не удался; вызывающий рисует фолбэк-бокс.
 */
expect fun decodeLocalDrawable(key: String, sampleIfWiderThan: Int = Int.MAX_VALUE): ImageBitmap?
