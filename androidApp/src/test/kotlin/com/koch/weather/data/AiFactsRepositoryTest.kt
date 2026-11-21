package com.koch.weather.data

import com.koch.weather.data.model.FactOfDayPolicy
import com.koch.weather.data.repository.AiFactsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Item 12 фазы 5: список фактов расширен (>= 16), тексты без повторов,
 * факт выбирается по индексу сессии — при перезапуске он меняется, а внутри
 * сессии «другой факт» не повторяет текущий.
 */
class AiFactsRepositoryTest {

    private val repository = AiFactsRepository()

    @Test
    fun factsAreAtLeastSixteenAndUnique() {
        val facts = repository.getAllFacts()
        assertTrue("Нужно минимум 16 фактов, есть ${facts.size}", facts.size >= 16)
        assertEquals("Факты не должны повторяться", facts.size, facts.toSet().size)
        assertTrue(facts.none { it.isBlank() })
        facts.forEach { fact ->
            assertTrue("Слишком короткий факт: $fact", fact.length > 15)
        }
    }

    @Test
    fun factCount_matchesFactList() {
        assertEquals(repository.getAllFacts().size, repository.getFactCount())
    }

    @Test
    fun factAt_isStableForSessionIndex() {
        // Внутри сессии факт берётся по сохранённому индексу: рекомпозиция,
        // смена города и возврат из фона текст не меняют.
        val index = 7
        val fact = repository.getFactAt(index)
        assertEquals(fact, AiFactsRepository().getFactAt(index))
    }

    @Test
    fun factAt_normalizesOutOfRangeIndex() {
        val facts = repository.getAllFacts()
        assertEquals(facts.first(), repository.getFactAt(0))
        assertEquals(facts.last(), repository.getFactAt(facts.size - 1))
        assertEquals(facts.first(), repository.getFactAt(facts.size))
        assertEquals(facts.last(), repository.getFactAt(-1))
    }

    @Test
    fun newIndexPerStart_variesFact() {
        // Перезапуск больше не даёт детерминированный по дню факт: индекс на
        // старте выбирается случайно, поэтому тексты стартов различаются.
        val facts = (1..100).map {
            repository.getFactAt(FactOfDayPolicy.initialIndex(repository.getFactCount()))
        }.toSet()
        assertTrue("Факт должен меняться между запусками", facts.size > 1)
    }

    @Test
    fun nextFactIndex_neverRepeatsCurrentFact() {
        val facts = repository.getAllFacts()
        facts.indices.forEach { current ->
            repeat(25) {
                val next = FactOfDayPolicy.nextIndex(current, facts.size)
                assertNotEquals(facts[current], facts[next])
            }
        }
    }

    @Test
    fun defaultFact_isFirstHistoricalFact() {
        assertEquals("Чтобы точно узнать погоду, нужно посмотреть в окно", repository.getDefaultFact())
        assertTrue(repository.getRandomFact().isNotBlank())
    }
}
