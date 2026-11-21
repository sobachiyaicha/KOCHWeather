package com.koch.weather.data

import com.koch.weather.data.model.ImageCatalog
import com.koch.weather.data.model.ImageCategory
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.repository.ImageRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCatalogTest {

    private val repository = ImageRepository()

    @Test
    fun everyCategoryHasStableWikimediaUrls() {
        ImageCategory.values().forEach { category ->
            val images = repository.getImages(category)
            assertTrue("Нет фото для $category", images.isNotEmpty())
            images.forEach { image ->
                assertTrue(image.url.startsWith("https://upload.wikimedia.org/wikipedia/commons/thumb/"))
                assertTrue(image.url.endsWith(".jpg") || image.url.endsWith(".png"))
                assertTrue("Ожидалось компактное превью 500px: ${image.url}", image.url.contains("/500px-"))
                assertFalse("utm-параметры не нужны: ${image.url}", image.url.contains("?"))
                assertTrue(image.sourcePageUrl.startsWith("https://commons.wikimedia.org/wiki/File:"))
                assertEquals("Wikimedia Commons", image.source)
            }
        }
    }

    @Test
    fun newCategories_haveAtLeastOneImage() {
        val required = listOf(
            ImageCategory.IPHONE_FRONT,
            ImageCategory.FOOD_FRIES,
            ImageCategory.FOOD_NUGGETS,
            ImageCategory.FOOD_PIROSHKI,
            ImageCategory.FOOD_HOTDOG,
            ImageCategory.FOOD_COFFEE,
            ImageCategory.SKY_BLUE,
            ImageCategory.SKY_SUNSET,
            ImageCategory.SKY_NORTHERN
        )
        required.forEach { category ->
            assertTrue(
                "Нет фото для $category",
                repository.getImages(category).isNotEmpty()
            )
        }
    }

    @Test
    fun urlsByCategory_matchesCatalogAndHasNoDuplicates() {
        val map = repository.getUrlsByCategory()
        assertEquals(ImageCategory.values().toSet(), map.keys)
        val allUrls = map.values.flatten()
        assertEquals(allUrls.size, allUrls.toSet().size)
        assertEquals(repository.getUrls(ImageCategory.FOOD), map[ImageCategory.FOOD])
    }

    @Test
    fun idsAndUrlsAreUnique() {
        val all = repository.getAllImages()
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { it.url }.toSet().size)
    }

    @Test
    fun skyBackgroundUrls_areExposedInUiState() {
        val state = MainScreenUiState(imageUrls = repository.getUrlsByCategory())
        assertEquals(repository.getUrls(ImageCategory.SKY_BLUE).first(), state.skyBlueImageUrl)
        assertEquals(repository.getUrls(ImageCategory.SKY_SUNSET).first(), state.skySunsetImageUrl)
        assertEquals(repository.getUrls(ImageCategory.SKY_NORTHERN).first(), state.skyNorthernImageUrl)
    }

    @Test
    fun userAgentIsSetForWikimedia() {
        assertTrue(ImageCatalog.WIKIMEDIA_USER_AGENT.contains("KOCHWeather"))
        assertEquals(ImageCatalog.WIKIMEDIA_USER_AGENT, repository.userAgent())
    }
}
