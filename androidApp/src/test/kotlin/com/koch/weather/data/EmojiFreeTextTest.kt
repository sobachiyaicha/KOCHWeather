package com.koch.weather.data

import com.koch.weather.data.model.DefaultUmbrellas
import com.koch.weather.data.model.EmojiTextGuard
import com.koch.weather.data.model.ThemeCatalog
import com.koch.weather.data.repository.AiFactsRepository
import com.koch.weather.data.repository.FlightRepository
import com.koch.weather.data.repository.FoodRepository
import com.koch.weather.data.repository.ImageRepository
import com.koch.weather.data.repository.TaxiRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Маркер для code-reviewer: в пакете data и MainScreenViewModel.kt не должно
 * быть Unicode-эмодзи. Типографика (°, ★, ☆, ›, →) разрешена и не ловится.
 */
class EmojiFreeTextTest {

    @Test
    fun runtimeDataStrings_haveNoEmoji() {
        val texts = mutableListOf<String>()
        val food = FoodRepository()
        food.getRestaurants().forEach { restaurant ->
            texts += restaurant.name
            texts += restaurant.category
        }
        listOf("rest_vkusno", "rest_dodo", "rest_surf").forEach { id ->
            food.getMenuItems(id).forEach { item ->
                texts += item.name
                texts += item.description
            }
        }
        TaxiRepository().getDestinations().forEach {
            texts += it.title
            texts += it.subtitle
        }
        FlightRepository().getFlightOffers().forEach {
            texts += it.fromCity
            texts += it.toCity
            texts += it.airline
            texts += it.weatherAtDestination
        }
        texts += AiFactsRepository().getAllFacts()
        DefaultUmbrellas.items.forEach {
            texts += it.name
            texts += it.description
            texts += it.priceRu
        }
        ThemeCatalog.build(ThemeCatalog.DEFAULT_UNLOCKED).forEach { texts += it.titleRu }
        ImageRepository().getAllImages().forEach {
            texts += it.descriptionRu
            texts += it.url
            texts += it.sourcePageUrl
        }

        val offenders = texts.filter { EmojiTextGuard.containsEmoji(it) }
        assertTrue("Найдены эмодзи в данных: $offenders", offenders.isEmpty())
    }

    @Test
    fun sourceFilesOfDataLayer_haveNoEmoji() {
        // Phase 1: исходники data-слоя разъехались по shared/{commonMain,androidMain};
        // прежние корни (src/main/kotlin, app/src/main/kotlin) оставлены для истории.
        val roots = listOf(
            File("src/main/kotlin"),
            File("app/src/main/kotlin"),
            File("../shared/src/commonMain/kotlin"),
            File("../shared/src/androidMain/kotlin")
        ).filter { it.isDirectory }
        val targets = roots.flatMap { root ->
            listOf(
                File(root, "com/koch/weather/data"),
                File(root, "com/koch/weather/ui/main/MainScreenViewModel.kt")
            )
        }
        val files = targets.filter { it.exists() }.flatMap { target ->
            if (target.isDirectory) {
                target.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
            } else {
                listOf(target)
            }
        }
        assertTrue("Не найдены исходники для проверки", files.isNotEmpty())

        val offenders = files.mapNotNull { file ->
            val text = file.readText(Charsets.UTF_8)
            if (EmojiTextGuard.containsEmoji(text)) file.path else null
        }
        assertTrue("Эмодзи найдены в файлах: $offenders", offenders.isEmpty())
    }

    @Test
    fun typographyIsAllowed() {
        val typography = "° ★ ☆ › → ← ◀ ▶ × • — «» №"
        assertFalse(EmojiTextGuard.containsEmoji(typography))
    }

    @Test
    fun emojiIsDetected() {
        val smile = String(Character.toChars(0x1F600))
        val coffee = String(Character.toChars(0x2615))
        assertTrue(EmojiTextGuard.containsEmoji("текст $smile"))
        assertTrue(EmojiTextGuard.containsEmoji("текст $coffee"))
    }
}
