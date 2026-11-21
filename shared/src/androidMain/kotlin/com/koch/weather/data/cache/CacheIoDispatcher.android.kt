package com.koch.weather.data.cache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Эталон Android: withContext(Dispatchers.IO) — сохранён 1:1. */
actual fun cacheIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
