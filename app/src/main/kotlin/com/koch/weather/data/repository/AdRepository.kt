package com.koch.weather.data.repository

import com.koch.weather.data.model.AdCatalog
import com.koch.weather.data.model.AdCreative

/**
 * Реклама (item 9 фазы 5): 4 креатива и детерминированная ротация.
 *
 * Один и тот же креатив не должен висеть всегда: номер зависит от дня года и
 * числа открытий рекламного листа — `creatives[(dayOfYear + openCount) % size]`.
 * Без Random: после перезапуска в тот же день показывается тот же креатив.
 */
class AdRepository {

    fun getCreatives(): List<AdCreative> = AdCatalog.creatives

    fun creativeFor(dayOfYear: Int, openCount: Int = 0): AdCreative =
        AdCatalog.creativeFor(dayOfYear, openCount)

    fun count(): Int = AdCatalog.creatives.size
}
