package com.koch.weather.data.cache

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Диспетчер для suspend-вызовов кэша (замена эталонного withContext(Dispatchers.IO)).
 *
 * Почему expect/actual, а не Dispatchers.IO напрямую: в kotlinx-coroutines 1.10.2
 * `Dispatchers.IO` — internal в Kotlin/Native klib (JVM-концепция blocking pool),
 * commonMain скомпилировать его не может. Android сохраняет ровно Dispatchers.IO
 * из эталона; iOS — Default (блокирующих вызовов в телах методов WeatherCache нет:
 * DataStore сам исполняет файловый IO в scope своего актора).
 */
expect fun cacheIoDispatcher(): CoroutineDispatcher
