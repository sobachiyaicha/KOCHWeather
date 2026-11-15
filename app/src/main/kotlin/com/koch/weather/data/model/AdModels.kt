package com.koch.weather.data.model

import androidx.annotation.AnyRes
import androidx.compose.runtime.Immutable
import com.koch.weather.R

/** Тип медиа в рекламном креативе: фото рисуем через `Image`, ролик — через `VideoView`. */
enum class AdMediaType {
    PHOTO,
    VIDEO
}

/**
 * Рекламный креатив-медиа (ТЗ «реклама»).
 *
 * Пул состоит из шести креативов, и медиа «вылетает» без надписей: в полноэкранном
 * поп-апе нет ни заголовков, ни текста, ни CTA — только само медиа и крестик.
 * Единственный текст — красная подпись «КУПИТЬ СЕЙЧАС!» под тремя фото
 * ([hasBuyLabel]); у остальных креативов надписей нет вообще.
 *
 * @param id стабильный идентификатор креатива (он же ключ анимаций)
 * @param type фото или видео
 * @param mediaRes `R.drawable.ad_*` для фото, `R.raw.ad_*` для видео
 * @param hasBuyLabel красная подпись «КУПИТЬ СЕЙЧАС!» под медиа
 */
@Immutable
data class AdCreative(
    val id: String,
    val type: AdMediaType,
    @param:AnyRes val mediaRes: Int,
    val hasBuyLabel: Boolean = false
)

/**
 * Каталог рекламы (ТЗ «реклама»): пул из шести креативов на локальных медиа.
 *
 * - фото: `ad_photo1`, `ad_photo2`, `ad_slippers` (с подписью), `ad_son` (без подписи);
 * - видео: `ad_lion` (первый показ в жизни пользователя), `ad_slippers_video`.
 *
 * Ротация детерминированная: один и тот же вход даёт один и тот же креатив,
 * никакого `Random` — результат воспроизводим между запусками.
 */
object AdCatalog {

    /** Красная подпись под тремя фото (ТЗ п. 2). */
    const val BUY_LABEL_RU = "КУПИТЬ СЕЙЧАС!"

    /** Крупная белая надпись на зелёной плашке баннера (ТЗ п. 3). */
    const val BANNER_DISCOUNT_RU = "СКИДКА 50%"

    /** Мелкая надпись под ней на плашке баннера. */
    const val BANNER_NOTE_RU = "только сегодня"

    val photo1 = AdCreative(
        id = "ad_photo1",
        type = AdMediaType.PHOTO,
        mediaRes = R.drawable.ad_photo1,
        hasBuyLabel = true
    )

    val photo2 = AdCreative(
        id = "ad_photo2",
        type = AdMediaType.PHOTO,
        mediaRes = R.drawable.ad_photo2,
        hasBuyLabel = true
    )

    val slippers = AdCreative(
        id = "ad_slippers",
        type = AdMediaType.PHOTO,
        mediaRes = R.drawable.ad_slippers,
        hasBuyLabel = true
    )

    val son = AdCreative(
        id = "ad_son",
        type = AdMediaType.PHOTO,
        mediaRes = R.drawable.ad_son
    )

    val lion = AdCreative(
        id = "ad_lion",
        type = AdMediaType.VIDEO,
        mediaRes = R.raw.ad_lion
    )

    val slippersVideo = AdCreative(
        id = "ad_slippers_video",
        type = AdMediaType.VIDEO,
        mediaRes = R.raw.ad_slippers
    )

    /** Пул полноэкранного поп-апа: ровно шесть креативов. */
    val creatives: List<AdCreative> = listOf(
        photo1,
        photo2,
        slippers,
        son,
        lion,
        slippersVideo
    )

    /** Баннер главного экрана — только фото1/фото2 (ТЗ п. 3). */
    val bannerCreatives: List<AdCreative> = listOf(photo1, photo2)

    /** Первый в жизни пользователя поп-ап — обязательно «лев в очках». */
    fun firstShowCreative(): AdCreative = lion

    /** Ротация поп-апа: без `Random`, одинаковый вход — одинаковый креатив. */
    fun creativeFor(sequence: Int): AdCreative = creatives[sequence.mod(creatives.size)]

    /** Ротация баннера главного экрана между фото1 и фото2. */
    fun bannerCreativeFor(sequence: Int): AdCreative =
        bannerCreatives[sequence.mod(bannerCreatives.size)]

    fun byId(id: String): AdCreative? = creatives.firstOrNull { it.id == id }
}
