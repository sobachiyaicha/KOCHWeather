package com.koch.weather.data.model

/**
 * Запрет Unicode-эмодзи в данных. Типографика (°, ★, ☆, ›, →, ←, ◀, ▶, ×, •,
 * —, «», №) не считается эмодзи и не должна ловиться этим регэкспом.
 *
 * U+2605/U+2606 (★/☆) исключены из диапазона U+2600..U+27BF намеренно.
 */
object EmojiTextGuard {

    const val EMOJI_PATTERN =
        "[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{2604}\\x{2607}-\\x{27BF}" +
            "\\x{2B00}-\\x{2BFF}\\x{FE0F}\\x{200D}\\x{1F3FB}-\\x{1F3FF}]"

    private val regex = Regex(EMOJI_PATTERN)

    fun containsEmoji(text: String): Boolean = regex.containsMatchIn(text)

    fun findAll(text: String): List<String> = regex.findAll(text).map { it.value }.toList()
}
