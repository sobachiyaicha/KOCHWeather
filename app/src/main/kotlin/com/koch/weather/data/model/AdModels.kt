package com.koch.weather.data.model

import androidx.annotation.AnyRes
import androidx.compose.runtime.Immutable
import com.koch.weather.R
import kotlin.random.Random

/** Тип медиа в рекламном креативе: фото рисуем через `Image`, ролик — через `TextureView` + `MediaPlayer`. */
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
 * @param videoWidth ширина кадра ролика в пикселях (0 — неизвестна); размер
 *   известен заранее, чтобы выставить fit-матрицу `TextureView` **до** `play()`
 *   и первый же кадр показать с letterbox, а не растянутым на весь экран
 * @param videoHeight высота кадра ролика в пикселях (0 — неизвестна)
 */
@Immutable
data class AdCreative(
    val id: String,
    val type: AdMediaType,
    @param:AnyRes val mediaRes: Int,
    val hasBuyLabel: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0
)

/**
 * Каталог рекламы (ТЗ «реклама»): пул из шести креативов на локальных медиа.
 *
 * - фото: `ad_photo1`, `ad_photo2`, `ad_slippers` (с подписью), `ad_son` (без подписи);
 * - видео: `ad_lion` (первый показ в жизни пользователя), `ad_slippers_video`.
 *
 * Выбор креатива поп-апа случайный, но воспроизводимый в тестах: тип медиа
 * (видео/фото) решает `Random`, который передаётся параметром ([popupCreative]).
 * Один и тот же креатив не показывается два раза подряд — последний id
 * исключается из выбора.
 */
object AdCatalog {

    /** Красная подпись под тремя фото (ТЗ п. 2). */
    const val BUY_LABEL_RU = "КУПИТЬ СЕЙЧАС!"

    /** Шанс видео-креатива в поп-апе: 50 % видео / 50 % фото. */
    const val POPUP_VIDEO_CHANCE_PERCENT = 50

    /** Верхняя граница для стохастических бросков в процентах. */
    private const val PERCENT_MAX = 100

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

    // Размеры кадров роликов (tkhd в MP4): известны заранее, поэтому движок
    // вписывает кадр в экран ещё до `play()` — первого «растянутого» кадра нет.
    /** Кадр `res/raw/ad_lion.mp4`. */
    private const val LION_WIDTH = 360
    private const val LION_HEIGHT = 480

    /** Кадр `res/raw/ad_slippers.mp4`. */
    private const val SLIPPERS_VIDEO_WIDTH = 360
    private const val SLIPPERS_VIDEO_HEIGHT = 360

    val lion = AdCreative(
        id = "ad_lion",
        type = AdMediaType.VIDEO,
        mediaRes = R.raw.ad_lion,
        videoWidth = LION_WIDTH,
        videoHeight = LION_HEIGHT
    )

    val slippersVideo = AdCreative(
        id = "ad_slippers_video",
        type = AdMediaType.VIDEO,
        mediaRes = R.raw.ad_slippers,
        videoWidth = SLIPPERS_VIDEO_WIDTH,
        videoHeight = SLIPPERS_VIDEO_HEIGHT
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

    /** Видео-креативы поп-апа: `ad_lion`, `ad_slippers_video`. */
    val popupVideoCreatives: List<AdCreative> =
        creatives.filter { it.type == AdMediaType.VIDEO }

    /** Фото-креативы поп-апа: фото1, фото2, тапки, сын. */
    val popupPhotoCreatives: List<AdCreative> =
        creatives.filter { it.type == AdMediaType.PHOTO }

    /** Первый в жизни пользователя поп-ап — обязательно «лев в очках». */
    fun firstShowCreative(): AdCreative = lion

    /**
     * Креатив полноэкранного поп-апа после стирания облаков.
     *
     * С вероятностью [POPUP_VIDEO_CHANCE_PERCENT] % берётся видео-креатив,
     * иначе фото. Креатив [lastShownId] (последний показанный) исключается
     * из пула, поэтому один и тот же ролик/фото не идёт два раза подряд.
     * [random] инжектится параметром — как в [FactOfDayPolicy] — чтобы тесты
     * получали детерминированный выбор без глобального состояния.
     *
     * Пул из одного креатива: повтор неизбежен, поэтому возвращается он же,
     * а не падение/пустой результат.
     */
    fun popupCreative(
        lastShownId: String? = null,
        random: Random = Random.Default
    ): AdCreative {
        val video = random.nextInt(PERCENT_MAX) < POPUP_VIDEO_CHANCE_PERCENT
        // Пустой пул типа (каталог без роликов) — не повод падать: берём весь пул.
        val pool = (if (video) popupVideoCreatives else popupPhotoCreatives).ifEmpty { creatives }
        val candidates = pool.filterNot { it.id == lastShownId }
        val resolved = candidates.ifEmpty { pool }
        return resolved[random.nextInt(resolved.size)]
    }

    /** Ротация поп-апа: без `Random`, одинаковый вход — одинаковый креатив. */
    fun creativeFor(sequence: Int): AdCreative = creatives[sequence.mod(creatives.size)]

    /** Ротация баннера главного экрана между фото1 и фото2. */
    fun bannerCreativeFor(sequence: Int): AdCreative =
        bannerCreatives[sequence.mod(bannerCreatives.size)]

    fun byId(id: String): AdCreative? = creatives.firstOrNull { it.id == id }
}
