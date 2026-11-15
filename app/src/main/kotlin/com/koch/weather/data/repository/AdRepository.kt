package com.koch.weather.data.repository

import com.koch.weather.data.model.AdCatalog
import com.koch.weather.data.model.AdCreative

/**
 * Реклама (ТЗ «реклама»): пул из шести медиа-креативов и детерминированная ротация.
 *
 * Баннер главного экрана ротируется только между фото1/фото2 ([bannerCreativeFor]),
 * полноэкранный поп-ап — по всему пулу ([creativeFor]). Первый в жизни показ
 * закреплён за «львом» ([firstShowCreative]) и запоминается в DataStore
 * (`first_ad_shown`), поэтому решается на уровне ViewModel.
 */
class AdRepository {

    fun getCreatives(): List<AdCreative> = AdCatalog.creatives

    fun count(): Int = AdCatalog.creatives.size

    fun firstShowCreative(): AdCreative = AdCatalog.firstShowCreative()

    fun creativeFor(sequence: Int): AdCreative = AdCatalog.creativeFor(sequence)

    fun bannerCreativeFor(sequence: Int): AdCreative = AdCatalog.bannerCreativeFor(sequence)
}
