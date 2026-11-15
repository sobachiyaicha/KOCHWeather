package com.koch.weather.data.model

import kotlin.random.Random

/**
 * Выбор «факта дня» на сессию приложения.
 *
 * Детерминизм по городу и дню года убран: факт выбирается случайно при каждом
 * запуске приложения. Выбранный индекс лежит в [MainScreenUiState.aiFactIndex],
 * поэтому рекомпозиция, смена города и возврат из фона в течение сессии факт не
 * меняют — новый факт будет только после перезапуска приложения или кнопки
 * «Другой факт».
 */
object FactOfDayPolicy {

    /** Индекс факта на старте приложения: любой из [size]. */
    fun initialIndex(size: Int, random: Random = Random.Default): Int =
        if (size <= 0) 0 else random.nextInt(size)

    /** Индекс «другого факта»: гарантированно не равен [currentIndex]. */
    fun nextIndex(currentIndex: Int, size: Int, random: Random = Random.Default): Int {
        if (size <= 1) return 0
        val current = currentIndex.mod(size)
        // Сдвиг 1..size-1 по кругу: тот же факт не выпадает.
        val offset = random.nextInt(size - 1) + 1
        return (current + offset).mod(size)
    }
}
