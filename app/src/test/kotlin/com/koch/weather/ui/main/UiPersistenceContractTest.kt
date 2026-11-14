package com.koch.weather.ui.main

import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.CityLocation
import com.koch.weather.data.model.DefaultCities
import com.koch.weather.data.model.MainScreenUiState
import com.koch.weather.data.model.PremiumOffer
import com.koch.weather.data.model.SelectedCityRestorePolicy
import com.koch.weather.data.model.ThemeCatalog
import com.koch.weather.data.model.ThemePreferenceCodec
import com.koch.weather.data.model.ThemeRestorePolicy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        assertEquals("179 ₽", offer.priceLabel)
        assertEquals("899 ₽", offer.oldPriceLabel)
        assertEquals("за год", offer.periodLabel)
        assertEquals("Скидка 80% только сегодня", offer.discountLabel)
        assertEquals(80, offer.discountPercent)
        assertEquals(offer, MainScreenUiState().premiumOffer)

        val custom = PremiumOffer(priceRub = 225, oldPriceRub = 900, periodRu = "месяц")
        assertEquals("225 ₽", custom.priceLabel)
        assertEquals("900 ₽", custom.oldPriceLabel)
        assertEquals("за месяц", custom.periodLabel)
    }

    @Test
    fun selectedCityCurrentLocation_isFalseByDefault() {
        val state = MainScreenUiState()
        assertFalse(state.isSelectedCityCurrentLocation)
        val searchCityState = state.copy(
            selectedCity = CityLocation("Казань", 55.7963, 49.1088, "Россия", "Татарстан")
        )
        assertFalse(searchCityState.isSelectedCityCurrentLocation)
        assertFalse(searchCityState.selectedCity!!.isCurrentLocation)
    }

    @Test
    fun cityLocation_jsonRestoresFlag_andDefaultsToFalseForOldCache() {
        val json = Json { ignoreUnknownKeys = true }
        val geo = CityLocation(
            name = "Санкт-Петербург",
            latitude = 59.9386,
            longitude = 30.3141,
            country = "Россия",
            region = "Санкт-Петербург",
            isCurrentLocation = true
        )
        val decoded = json.decodeFromString<CityLocation>(json.encodeToString(geo))
        assertTrue(decoded.isCurrentLocation)
        assertEquals(geo, decoded)

        // Старый кэш без нового поля: флаг восстанавливается дефолтом false.
        val legacy = """{"name":"Казань","latitude":55.7963,"longitude":49.1088}"""
        val legacyCity = json.decodeFromString<CityLocation>(legacy)
        assertFalse(legacyCity.isCurrentLocation)
        assertEquals("Россия", legacyCity.country)
    }

    // ------------------------------------------------------------------
    // D-2 feature-qa: выбранный город переживает перезапуск.
    // VM в JVM-тесте не создать (Application/DataStore), поэтому проверяем
    // ровно ту startup-политику, которую VM применяет к сохранённому городу.
    // ------------------------------------------------------------------

    private val kazan = CityLocation(
        name = "Казань",
        latitude = 55.7963,
        longitude = 49.1088,
        country = "Россия",
        region = "Татарстан"
    )

    @Test
    fun selectedCity_survivesVmRecreation() {
        val cities = DefaultCities.cities + kazan

        // Первый экземпляр VM: пользователь выбрал Казань и выбор сохранён.
        val savedSelection = kazan

        // Новый экземпляр VM: startup-политика восстанавливает тот же город.
        val restored = SelectedCityRestorePolicy.resolve(
            savedSelected = savedSelection,
            legacyLastCity = null,
            cities = cities
        )

        assertEquals("Казань", restored?.name)
        assertEquals(kazan.latitude, restored!!.latitude, 0.0001)
        assertEquals(kazan.longitude, restored.longitude, 0.0001)
    }

    @Test
    fun selectedCity_jsonRoundTrip_restoresSameCoordinatesAfterRestart() {
        val json = Json { ignoreUnknownKeys = true }
        val cities = DefaultCities.cities + kazan
        val stored = json.decodeFromString<CityLocation>(json.encodeToString(kazan))

        val restored = SelectedCityRestorePolicy.resolve(
            savedSelected = stored,
            legacyLastCity = null,
            cities = cities
        )

        assertEquals("Казань", restored?.name)
        assertEquals(kazan.latitude, restored!!.latitude, 0.0001)
        assertFalse(restored.isCurrentLocation)
    }

    @Test
    fun selectedCity_winsOverLastForecastCity() {
        val cities = DefaultCities.cities
        val sochi = cities.first { it.name == "Сочи" }
        val moscow = cities.first { it.name == "Москва" }

        val restored = SelectedCityRestorePolicy.resolve(
            savedSelected = sochi,
            legacyLastCity = moscow,
            cities = cities
        )

        assertEquals("Сочи", restored?.name)
    }

    @Test
    fun selectedCity_missingFromList_fallsBackToGeoCity() {
        val cities = DefaultCities.cities
        val removedCity = CityLocation("Минск", 53.9006, 27.5590, "Беларусь")

        val restored = SelectedCityRestorePolicy.resolve(
            savedSelected = removedCity,
            // Даже «последний прогноз» не должен подменять гео-город при
            // сохранённом выборе, которого больше нет в списке.
            legacyLastCity = cities.first { it.name == "Москва" },
            cities = cities
        )

        assertEquals(DefaultCities.GEO_CITY_NAME, restored?.name)
        assertTrue(restored!!.isCurrentLocation)
    }

    @Test
    fun noSavedSelection_keepsLegacyLastForecastBehavior() {
        val cities = DefaultCities.cities
        val moscow = cities.first { it.name == "Москва" }

        val restored = SelectedCityRestorePolicy.resolve(
            savedSelected = null,
            legacyLastCity = moscow,
            cities = cities
        )

        assertEquals("Москва", restored?.name)
    }

    @Test
    fun noSavedSelectionAndNoLastForecast_fallsBackToGeoCity() {
        val restored = SelectedCityRestorePolicy.resolve(
            savedSelected = null,
            legacyLastCity = null,
            cities = DefaultCities.cities
        )

        assertEquals(DefaultCities.GEO_CITY_NAME, restored?.name)
    }
}
