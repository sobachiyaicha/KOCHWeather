@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.koch.weather.data.cache

import kotlin.concurrent.AtomicInt
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.disposeOnCancellation
import kotlinx.coroutines.CancellableContinuation
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/**
 * Диспетчер для suspend-вызовов кэша и всех таймерных запусков VM на iOS
 * (замена эталонного withContext(Dispatchers.IO) и общего Dispatchers.Default).
 *
 * Android сохраняет ровно Dispatchers.IO из эталона (CacheIoDispatcher.android.kt):
 * `Dispatchers.IO` — JVM-концепция blocking pool, в Kotlin/Native klib она internal,
 * поэтому commonMain компилирует только expect.
 *
 * iOS: главная dispatch-очередь (`dispatch_async`/`dispatch_after`), НЕ Worker'ы K/N
 * и НЕ Dispatchers.Default. История вопроса (phase 2, 2026-11-21, дефект «после
 * стирания вуали нет записи clouds_cleared/first_ad_shown и не открывается поп-ап»):
 *
 * 1. Общий `Dispatchers.Default` (актор DataStore 1.1.1 в `AppContainer.applicationScope`):
 *    воркер K/N на iOS 26.4 симуляторе теряет wakeup (`Worker::park` спит при
 *    доставленной задаче) — потребитель актора замораживается, все `edit` висят
 *    на `ack.await()` (bug-hunter, sample-стеки /tmp/koch_sample*.txt).
 *
 * 2. Выделенный Worker с отложенными задачами `executeAfter(1 мс)` (раунд 1) —
 *    недетерминирован: каждый dispatch и каждый 100-мс квант — новый цикл
 *    park/wake того же ненадёжного механизма.
 *
 * 3. Прямой `Dispatchers.Main` (раунд 2) без собственного Delay: `delay()` уходит
 *    в общий таймер-колесо Default и умирает вместе с ним (порт-лид, раунд 3,
 *    зонд NSLog + println/--console-pty: стартовый всплеск ~530 dispatch/с жив,
 *    через ~4-6 с тишины dispatch/timer не срабатывают до конца процесса; при
 *    этом сеть-resume через реальные ОС-потоки работает).
 *
 * 4. Этот шим (раунд 4): планировка через `dispatch_after` главной очереди —
 *    CFRunLoop-main не теряет пробуждений (на нём стоит весь UI). Актор DataStore,
 *    кэш-вызовы и все delay()-зависимые запуски VM (вуаль 4 с / возврат 20–30 с,
 *    баннер 8 с) едут через один надёжный планировщик. Нагрузка на main —
 *    миллисекунды (pb ~10 КБ, актор сериализует edit'ы, события редкие).
 *    Семантика для вызывающих не меняется: те же тайминги, те же ключи и JSON
 *    (01-APP-SPEC §8); Android-путь не затронут.
 *
 * Держать в синхронизации с `AppContainer.applicationScope`: он обязан
 * использовать ЭТОТ ЖЕ диспетчер, чтобы актор DataStore, кэш-вызовы и таймеры
 * VM жили в одном планировщике.
 */
@InternalCoroutinesApi
internal class KochMainQueueDispatcher private constructor() : CoroutineDispatcher(), Delay {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatch_async(dispatch_get_main_queue()) { block.run() }
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>
    ) {
        val handle = scheduleAfterDelayMillis(timeMillis) {
            with(continuation) { resumeUndispatched(Unit) }
        }
        continuation.disposeOnCancellation(handle)
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val cancelled = AtomicInt(0)
        scheduleAfterDelayMillis(timeMillis) {
            if (cancelled.value == 0) block.run()
        }
        return DisposableHandle { cancelled.value = 1 }
    }

    private fun scheduleAfterDelayMillis(timeMillis: Long, block: () -> Unit): DisposableHandle {
        val disposed = AtomicInt(0)
        val whenNs = dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000)
        dispatch_after(whenNs, dispatch_get_main_queue()) {
            if (disposed.value == 0) block()
        }
        return DisposableHandle { disposed.value = 1 }
    }

    companion object {
        val INSTANCE: KochMainQueueDispatcher by lazy { KochMainQueueDispatcher() }
    }
}

@InternalCoroutinesApi
actual fun cacheIoDispatcher(): CoroutineDispatcher = KochMainQueueDispatcher.INSTANCE
