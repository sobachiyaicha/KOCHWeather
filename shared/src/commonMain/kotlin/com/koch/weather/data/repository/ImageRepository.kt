package com.koch.weather.data.repository

import com.koch.weather.data.model.ImageCatalog
import com.koch.weather.data.model.ImageCategory
import com.koch.weather.data.model.RemoteImage

/**
 * Реальные фото Wikimedia Commons (R17). Списки стабильны, проверены HTTP 200
 * 2026-11-14. Никаких выдуманных URL и локальных плейсхолдеров-заглушек:
 * отсутствие загрузки обрабатывает Coil (placeholder — зона ui-coder).
 */
class ImageRepository {

    fun getImages(category: ImageCategory): List<RemoteImage> =
        ImageCatalog.images[category].orEmpty()

    fun getUrls(category: ImageCategory): List<String> =
        ImageCatalog.urls(category)

    fun getUrlsByCategory(): Map<ImageCategory, List<String>> =
        ImageCatalog.urlsByCategory()

    fun getPrimaryUrl(category: ImageCategory): String? =
        ImageCatalog.images[category]?.firstOrNull()?.url

    fun getAllImages(): List<RemoteImage> =
        ImageCategory.values().flatMap { getImages(it) }

    /** User-Agent, который обязателен для upload.wikimedia.org (иначе 429). */
    fun userAgent(): String = ImageCatalog.WIKIMEDIA_USER_AGENT
}
