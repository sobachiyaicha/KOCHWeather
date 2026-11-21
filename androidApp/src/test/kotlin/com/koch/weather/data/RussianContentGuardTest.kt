package com.koch.weather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Граница «Россия, рубли» (item 11 фазы 5): в `app/src/main` не должно остаться
 * ни прежней валюты, ни прежнего города в дефолтах. Тест читает исходники так же,
 * как [EmojiFreeTextTest], — это дешёвый и честный страж вместо проверки на глаз.
 *
 * Проверка регистронезависимая: раньше маркер «Минск» с заглавной буквы не ловил
 * `"минск"`/`"minsk"` во внутреннем маппинге, и тест был ложно-зелёным.
 */
class RussianContentGuardTest {

    private val forbidden = listOf("BYN", "Минск", "Беларус", "minsk")

    /**
     * Один критерий и для реального скана файлов, и для юнит-примеров ниже:
     * регистр не важен, поэтому нижний регистр больше не проскакивает.
     */
    private fun offendersIn(text: String, markers: List<String> = forbidden): List<String> =
        markers.filter { marker -> text.contains(marker, ignoreCase = true) }

    @Test
    fun mainSources_haveNoForeignCurrencyOrRemovedCity() {
        // Phase 1: исходники/ресурсы разъехались по shared/{commonMain,androidMain}
        // и androidApp; прежний корень app/src/main оставлен для истории.
        val roots = listOf(
            File("src/main"),
            File("app/src/main"),
            File("../shared/src/commonMain"),
            File("../shared/src/androidMain")
        ).filter { it.isDirectory }
        val files = roots.flatMap { root -> root.walkTopDown().toList() }
            .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
            .toList()
        assertTrue("Не найдены исходники для проверки", files.isNotEmpty())

        val offenders = mutableListOf<String>()
        files.forEach { file ->
            val text = file.readText(Charsets.UTF_8)
            offendersIn(text).forEach { marker -> offenders += "${file.path}: $marker" }
        }
        assertTrue("Найдены запрещённые подстроки: $offenders", offenders.isEmpty())
    }

    @Test
    fun guard_catchesLowercaseSamples() {
        // Пример-образец, а не реальный файл: гард обязан падать на нижнем регистре.
        assertEquals(listOf("Минск"), offendersIn("Поездка в Минск"))
        assertEquals(listOf("Минск"), offendersIn("поездка в минск"))
        assertEquals(listOf("minsk"), offendersIn("trip to MINSK"))
        assertEquals(listOf("BYN"), offendersIn("Билет за 179 byn"))
        assertEquals(listOf("Беларус"), offendersIn("беларусский маршрут"))

        assertTrue(offendersIn("Санкт-Петербург, Москва, Сочи, Дубай").isEmpty())
        assertTrue(offendersIn("Нет данных о пробках").isEmpty())
    }
}
