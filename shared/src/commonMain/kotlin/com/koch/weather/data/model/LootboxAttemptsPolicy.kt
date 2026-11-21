package com.koch.weather.data.model

/**
 * Лимит бесплатных попыток лутбокса (ТЗ: 5 на пользователя, персистентно).
 *
 * Вынесено из [com.koch.weather.data.repository.LootboxRepository], чтобы
 * границу «есть попытка / нет попытки», бонус за додеп и бонус за рекламу
 * можно было проверить JVM-тестом без DataStore и Android Context.
 * Премиум-пользователю попытки не ограничены — флаг уже есть в кэше
 * (`WeatherCache.isPremium`).
 *
 * Бонусные попытки хранятся как отрицательное значение `attemptsUsed`:
 * контракт `attemptsLeft = freeAttemptsTotal - attemptsUsed` тогда сам даёт
 * остаток больше пяти, а отдельное поле состояния не нужно.
 */
object LootboxAttemptsPolicy {

    val FREE_ATTEMPTS: Int = LootboxState.FREE_ATTEMPTS_TOTAL

    /** Сколько бонусных попыток максимум можно накопить рекламой (не копим десятки). */
    const val AD_ATTEMPTS_MAX: Int = 5

    /** Можно ли крутить лутбокс: премиум — всегда, остальным пока есть попытки. */
    fun canOpen(attemptsUsed: Int, isPremium: Boolean = false): Boolean =
        isPremium || attemptsUsed < FREE_ATTEMPTS

    /**
     * `attemptsUsed` после дропа: попытка списывается, а приз-додеп сразу
     * возвращает её назад. Премиум счётчик не трогает.
     */
    fun afterDrop(
        attemptsUsed: Int,
        isPremium: Boolean = false,
        grantsExtraAttempt: Boolean = false
    ): Int {
        if (isPremium) return attemptsUsed
        val spent = attemptsUsed + 1
        return if (grantsExtraAttempt) grantExtra(spent) else spent
    }

    /** +1 попытка (додеп): `attemptsUsed` уменьшается, `attemptsLeft` растёт. */
    fun grantExtra(attemptsUsed: Int, isPremium: Boolean = false): Int =
        if (isPremium) attemptsUsed else attemptsUsed - 1

    /** Начислять ли попытку за рекламу: не больше [AD_ATTEMPTS_MAX] бонусных. */
    fun canGrantAdAttempt(attemptsUsed: Int): Boolean = attemptsUsed > -AD_ATTEMPTS_MAX

    /** +1 попытка за просмотр рекламы (ограничение — [canGrantAdAttempt]). */
    fun grantAdAttempt(attemptsUsed: Int): Int = grantExtra(attemptsUsed)

    /** Остаток попыток; премиум не ограничен, поэтому остаток не уменьшается. */
    fun left(attemptsUsed: Int, isPremium: Boolean = false): Int =
        if (isPremium) {
            FREE_ATTEMPTS
        } else {
            (FREE_ATTEMPTS - attemptsUsed).coerceAtLeast(0)
        }
}
