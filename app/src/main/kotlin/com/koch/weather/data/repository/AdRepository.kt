package com.koch.weather.data.repository

import com.koch.weather.data.model.AdCatalog
import com.koch.weather.data.model.AdCreative
import kotlin.random.Random

/**
 * Реклама (ТЗ «реклама»): пул из шести медиа-креативов.
 *
 * Баннер главного экрана ротируется только между фото1/фото2 ([bannerCreativeFor]),
 * полноэкранный поп-ап выбирается случайно ([popupCreative]): 50 % видео / 50 % фото,
 * без повтора последнего креатива подряд. Первый в жизни показ закреплён за
 * «львом» ([firstShowCreative]) и запоминается в DataStore (`first_ad_shown`),
 * поэтому решается на уровне ViewModel.
 */
class AdRepository {

    fun getCreatives(): List<AdCreative> = AdCatalog.creatives

    fun count(): Int = AdCatalog.creatives.size

    fun firstShowCreative(): AdCreative = AdCatalog.firstShowCreative()

    fun creativeFor(sequence: Int): AdCreative = AdCatalog.creativeFor(sequence)

    fun popupCreative(
        lastShownId: String? = null,
        random: Random = Random.Default
    ): AdCreative = AdCatalog.popupCreative(lastShownId = lastShownId, random = random)

    fun bannerCreativeFor(sequence: Int): AdCreative = AdCatalog.bannerCreativeFor(sequence)
}
