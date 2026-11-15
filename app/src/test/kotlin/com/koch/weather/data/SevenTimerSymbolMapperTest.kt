package com.koch.weather.data

import com.koch.weather.data.provider.SevenTimerSymbolMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SevenTimerSymbolMapperTest {

    @Test
    fun mapsSevenTimerWeatherToWmo_includingDayNight() {
        assertEquals(0, SevenTimerSymbolMapper.toWmo("clearday"))
        assertEquals(0, SevenTimerSymbolMapper.toWmo("clearnight"))
        assertEquals(2, SevenTimerSymbolMapper.toWmo("pcloudyday"))
        assertEquals(3, SevenTimerSymbolMapper.toWmo("mcloudynight"))
        assertEquals(3, SevenTimerSymbolMapper.toWmo("cloudyday"))
        assertEquals(45, SevenTimerSymbolMapper.toWmo("fog"))
        assertEquals(61, SevenTimerSymbolMapper.toWmo("lightrainday"))
        assertEquals(63, SevenTimerSymbolMapper.toWmo("rainnight"))
        assertEquals(65, SevenTimerSymbolMapper.toWmo("heavyrainday"))
        assertEquals(80, SevenTimerSymbolMapper.toWmo("rainshowersday"))
        assertEquals(82, SevenTimerSymbolMapper.toWmo("heavyrainshowersnight"))
        assertEquals(71, SevenTimerSymbolMapper.toWmo("lightsnowday"))
        assertEquals(73, SevenTimerSymbolMapper.toWmo("snownight"))
        assertEquals(75, SevenTimerSymbolMapper.toWmo("heavysnowday"))
        assertEquals(85, SevenTimerSymbolMapper.toWmo("lightsnowshowersday"))
        assertEquals(95, SevenTimerSymbolMapper.toWmo("tsday"))
        assertEquals(95, SevenTimerSymbolMapper.toWmo("tsrainnight"))
        assertNull(SevenTimerSymbolMapper.toWmo("unknown"))
        assertNull(SevenTimerSymbolMapper.toWmo(null))
    }

    @Test
    fun directionToDegrees_mapsCardinalAndIntercardinal() {
        assertEquals(0.0, SevenTimerSymbolMapper.directionToDegrees("N") ?: -1.0, 0.001)
        assertEquals(45.0, SevenTimerSymbolMapper.directionToDegrees("NE") ?: -1.0, 0.001)
        assertEquals(180.0, SevenTimerSymbolMapper.directionToDegrees("S") ?: -1.0, 0.001)
        assertEquals(315.0, SevenTimerSymbolMapper.directionToDegrees("nw") ?: -1.0, 0.001)
        assertNull(SevenTimerSymbolMapper.directionToDegrees("???"))
    }

    @Test
    fun beaufortToMetersPerSecond_usesStandardTable() {
        assertEquals(0.0, SevenTimerSymbolMapper.beaufortToMetersPerSecond(0) ?: -1.0, 0.001)
        assertEquals(3.6, SevenTimerSymbolMapper.beaufortToMetersPerSecond(3) ?: -1.0, 0.001)
        assertEquals(17.2, SevenTimerSymbolMapper.beaufortToMetersPerSecond(8) ?: -1.0, 0.001)
        assertNull(SevenTimerSymbolMapper.beaufortToMetersPerSecond(null))
    }
}
