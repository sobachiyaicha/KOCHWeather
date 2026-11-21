package com.koch.weather.ui.components

import coil3.ImageLoader

/**
 * Шов процессного загрузчика сетевых фото (владелец — ui-porter, фаза 2).
 *
 * Coil 3 на Android требует `ImageLoader.Builder(context)` (default-конструктор
 * есть только в ios-варианте), поэтому сборка лоадера — платформенная:
 * components/KtorNetworkFetcherFactory/User-Agent одинаковы (см. actual).
 * UA — [ImageCatalog.WIKIMEDIA_USER_AGENT] дословно (1:1 с Android-исходником).
 */
expect fun wikimediaImageLoader(): ImageLoader
