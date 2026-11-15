package com.koch.weather.data

import com.koch.weather.data.repository.AiFactsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Item 12 фазы 5: факт дня детерминирован, «другой факт» не повторяет текущий,
 * список фактов расширен (>= 16), тексты без повторов.
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
    fun factForDay_isDeterministic() {
        val first = repository.getFactForDay("Москва", 123)
        val second = repository.getFactForDay("Москва", 123)
        assertEquals(first, second)
        assertEquals(repository.getFactForDay(123), repository.getFactForDay(null, 123))
    }

    @Test
    fun twoColdStartsWithSameCityAndDay_giveSameFact() {
        // D5: перезапуск в тот же день не должен менять детерминированный факт.
        val day = 318
        val firstStart = AiFactsRepository().getFactForDay("Санкт-Петербург", day)
        val secondStart = AiFactsRepository().getFactForDay("Санкт-Петербург", day)
        assertEquals(firstStart, secondStart)
    }

    @Test
    fun refreshCallsDoNotChangeFactOfDay() {
        // Другой факт выдаёт только refreshAiFact(); Random не влияет на сид дня.
        val before = repository.getFactForDay("Москва", 77)
        repeat(50) { repository.getNextFact(before) }
        assertEquals(before, repository.getFactForDay("Москва", 77))
    }

    @Test
    fun consecutiveDays_giveDifferentFacts() {
        (1..40).forEach { day ->
            assertNotEquals(
                "Соседние дни не должны повторять факт (день $day)",
                repository.getFactForDay("Москва", day),
                repository.getFactForDay("Москва", day + 1)
            )
        }
    }

    @Test
    fun differentCities_varyFacts() {
        val cities = listOf("Санкт-Петербург", "Москва", "Сочи", "Казань", "Дубай")
        val facts = cities.map { repository.getFactForDay(it, 10) }.toSet()
        assertTrue("Разные города могут давать разные факты", facts.size >= 2)
    }

    @Test
    fun nextFact_neverRepeatsCurrent() {
        repository.getAllFacts().forEach { current ->
            repeat(25) {
                assertNotEquals(current, repository.getNextFact(current))
            }
        }
        assertTrue(repository.getAllFacts().contains(repository.getNextFact("неизвестный факт")))
    }

    @Test
    fun defaultFact_isFirstHistoricalFact() {
        assertEquals("Чтобы точно узнать погоду, нужно посмотреть в окно", repository.getDefaultFact())
        assertTrue(repository.getRandomFact().isNotBlank())
    }
}
