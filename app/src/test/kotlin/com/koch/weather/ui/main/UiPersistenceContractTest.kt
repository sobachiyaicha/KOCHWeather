package com.koch.weather.ui.main

import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.PremiumOffer
import com.koch.weather.data.model.ThemeCatalog
import com.koch.weather.data.model.ThemePreferenceCodec
import com.koch.weather.data.model.ThemeRestorePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPersistenceContractTest {

    @Test
    fun themePreferenceCodec_roundTripsAndIgnoresGarbage() {
        assertEquals("COPPER_SUNSET", ThemePreferenceCodec.encode(AppThemeMode.COPPER_SUNSET))
        assertEquals(
            AppThemeMode.COPPER_SUNSET,
            ThemePreferenceCodec.decode("COPPER_SUNSET")
        )
        assertNull(ThemePreferenceCodec.decode(null))
        assertNull(ThemePreferenceCodec.decode("NOT_A_THEME"))
    }

    @Test
    fun themeRestorePolicy_keepsSavedThemeAfterRestart() {
        val unlocked = ThemeCatalog.DEFAULT_UNLOCKED
        assertEquals(
            AppThemeMode.COPPER_SUNSET,
            ThemeRestorePolicy.resolve("COPPER_SUNSET", unlocked)
        )
        assertNull(ThemeRestorePolicy.resolve("CYBERPUNK", unlocked))
        assertNull(ThemeRestorePolicy.resolve("NOT_A_THEME", unlocked))
        assertNull(ThemeRestorePolicy.resolve(null, unlocked))
    }

    @Test
    fun cloudVeilVisibility_hasSingleHonestSemantics() {
        val initial = MainScreenUiState()
        assertFalse(initial.isCloudVeilVisible)

        val locked = initial.copy(isCloudLocked = true, isCloudsCleared = false)
        assertTrue(locked.isCloudVeilVisible)

        val cleared = initial.copy(isCloudLocked = true, isCloudsCleared = true)
        assertFalse("После очистки вуаль не возвращается", cleared.isCloudVeilVisible)

        val restoredAfterRestart = initial.copy(
            isCloudLocked = false,
            isCloudsCleared = true
        )
        assertFalse(restoredAfterRestart.isCloudVeilVisible)
    }

    @Test
    fun premiumOffer_isSingleSourceForPriceAndPeriod() {
        val offer = PremiumOffer()
        assertEquals("5.99 BYN", offer.priceLabel)
        assertEquals("29.99 BYN", offer.oldPriceLabel)
        assertEquals("за год", offer.periodLabel)
        assertEquals("Скидка 80% только сегодня", offer.discountLabel)
        assertEquals(offer, MainScreenUiState().premiumOffer)

        val custom = PremiumOffer(priceByn = 7.5, oldPriceByn = 30.0, periodRu = "месяц")
        assertEquals("7.50 BYN", custom.priceLabel)
        assertEquals("30.00 BYN", custom.oldPriceLabel)
        assertEquals("за месяц", custom.periodLabel)
    }
}
