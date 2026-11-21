package com.koch.weather.data

import com.koch.weather.data.model.FactOfDayPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Факт дня больше не детерминирован по городу и дню года: при каждом запуске
 * приложения выбирается новый индекс, а в рамках сессии он живёт в состоянии
 * (`MainScreenUiState.aiFactIndex`) и при рекомпозиции не меняется.
 */
class FactOfDayPolicyTest {

    private val size = 20

    @Test
    fun initialIndex_isWithinFactsRange() {
        repeat(200) {
            val index = FactOfDayPolicy.initialIndex(size)
            assertTrue("Индекс вне диапазона: $index", index in 0 until size)
        }
    }

    @Test
    fun initialIndex_variesBetweenStarts() {
        // 100 «перезапусков»: всегда один и тот же факт — это уже не новое
        // поведение (детерминизм по дню убран).
        val indices = (1..100).map { FactOfDayPolicy.initialIndex(size) }.toSet()
        assertTrue("Факт должен меняться между запусками, получено: $indices", indices.size > 1)
    }

    @Test
    fun initialIndex_handlesEmptyAndSingleFactLists() {
        assertEquals(0, FactOfDayPolicy.initialIndex(0))
        assertEquals(0, FactOfDayPolicy.initialIndex(1))
    }

    @Test
    fun nextIndex_neverRepeatsCurrent() {
        (0 until size).forEach { current ->
            repeat(50) {
                assertNotEquals(current, FactOfDayPolicy.nextIndex(current, size))
            }
        }
    }

    @Test
    fun nextIndex_isWithinRangeAndNormalizesCurrent() {
        repeat(200) {
            val index = FactOfDayPolicy.nextIndex(currentIndex = 57, size = size)
            assertTrue("Индекс вне диапазона: $index", index in 0 until size)
            // 57 mod 20 == 17: «другой факт» не должен вернуть тот же индекс.
            assertNotEquals(17, index)
        }
        assertEquals(0, FactOfDayPolicy.nextIndex(0, 1))
        assertEquals(0, FactOfDayPolicy.nextIndex(-3, 1))
    }

    @Test
    fun nextIndex_dependsOnlyOnRandomSeed() {
        val first = FactOfDayPolicy.nextIndex(currentIndex = 5, size = size, random = Random(42))
        val second = FactOfDayPolicy.nextIndex(currentIndex = 5, size = size, random = Random(42))
        assertEquals(first, second)
    }
}
