package com.koch.weather.data

import com.koch.weather.R
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.backgroundRes
import com.koch.weather.data.model.defaultBackgroundRes
import com.koch.weather.data.model.previewRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * TZ: «Голубое небо» — фото с собакой (и фон главной, и карточка галереи),
 * «Кровавый закат» — без обоев, только градиент темы (`imageRes == null`
 * в `KochSkyBackground`). Маппинг ресурсов — единственный источник правды,
 * поэтому проверяем его, а не только UI.
 */
class ThemeBackgroundsTest {

    @Test
    fun blueSky_showsDogPhotoOnMainAndInGalleryCard() {
        assertEquals(R.drawable.theme_default as Int?, AppThemeMode.BLUE_SKY.backgroundRes)
        assertEquals(R.drawable.theme_default as Int?, AppThemeMode.BLUE_SKY.defaultBackgroundRes)
        assertEquals(R.drawable.theme_default_preview as Int?, AppThemeMode.BLUE_SKY.previewRes)
    }

    @Test
    fun bloodySunset_hasNoWallpaper_soUiFallsBackToThemeGradient() {
        assertNull(AppThemeMode.BLOODY_SUNSET.backgroundRes)
        assertNull(AppThemeMode.BLOODY_SUNSET.defaultBackgroundRes)
    }

    @Test
    fun otherThemes_keepTheirOwnFullBackgroundAndPreview() {
        AppThemeMode.values()
            .filter { it != AppThemeMode.BLUE_SKY && it != AppThemeMode.BLOODY_SUNSET }
            .forEach { theme ->
                assertEquals("$theme: фон главной", theme.backgroundRes, theme.defaultBackgroundRes)
                assertNotNull("$theme: нет ни превью, ни фона", theme.previewRes)
            }
    }
}
