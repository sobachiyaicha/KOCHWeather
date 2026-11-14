package com.koch.weather.data

import com.koch.weather.data.model.ImageCatalog
import com.koch.weather.data.model.ImageCategory
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
            assertTrue("Нет фото для $category", images.size >= 2)
            images.forEach { image ->
                assertTrue(image.url.startsWith("https://upload.wikimedia.org/wikipedia/commons/thumb/"))
                assertTrue(image.url.endsWith(".jpg") || image.url.endsWith(".png"))
                assertFalse("utm-параметры не нужны: ${image.url}", image.url.contains("?"))
                assertTrue(image.sourcePageUrl.startsWith("https://commons.wikimedia.org/wiki/File:"))
                assertEquals("Wikimedia Commons", image.source)
            }
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
    fun userAgentIsSetForWikimedia() {
        assertTrue(ImageCatalog.WIKIMEDIA_USER_AGENT.contains("KOCHWeather"))
        assertEquals(ImageCatalog.WIKIMEDIA_USER_AGENT, repository.userAgent())
    }
}
