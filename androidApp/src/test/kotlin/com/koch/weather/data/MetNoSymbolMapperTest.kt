package com.koch.weather.data

import com.koch.weather.data.provider.MetNoSymbolMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetNoSymbolMapperTest {

    @Test
    fun mapsDayNightAndPolartwilightSuffixes() {
        assertEquals(0, MetNoSymbolMapper.toWmo("clearsky_day"))
        assertEquals(0, MetNoSymbolMapper.toWmo("clearsky_night"))
        assertEquals(0, MetNoSymbolMapper.toWmo("clearsky_polartwilight"))
        assertEquals(1, MetNoSymbolMapper.toWmo("fair_night"))
        assertEquals(2, MetNoSymbolMapper.toWmo("partlycloudy_day"))
        assertEquals(3, MetNoSymbolMapper.toWmo("cloudy"))
    }

    @Test
    fun mapsRainSnowAndThunder() {
        assertEquals(45, MetNoSymbolMapper.toWmo("fog"))
        assertEquals(61, MetNoSymbolMapper.toWmo("lightrain"))
        assertEquals(63, MetNoSymbolMapper.toWmo("rain"))
        assertEquals(65, MetNoSymbolMapper.toWmo("heavyrain"))
        assertEquals(80, MetNoSymbolMapper.toWmo("lightrainshowers_day"))
        assertEquals(81, MetNoSymbolMapper.toWmo("rainshowers"))
        assertEquals(82, MetNoSymbolMapper.toWmo("heavyrainshowers_night"))
        assertEquals(71, MetNoSymbolMapper.toWmo("lightsnow"))
        assertEquals(73, MetNoSymbolMapper.toWmo("snow"))
        assertEquals(75, MetNoSymbolMapper.toWmo("heavysnow"))
        assertEquals(85, MetNoSymbolMapper.toWmo("lightsnowshowers"))
        assertEquals(86, MetNoSymbolMapper.toWmo("snowshowers"))
        assertEquals(66, MetNoSymbolMapper.toWmo("sleet"))
        assertEquals(67, MetNoSymbolMapper.toWmo("heavysleet"))
        assertEquals(95, MetNoSymbolMapper.toWmo("rainandthunder"))
        assertEquals(95, MetNoSymbolMapper.toWmo("snowandthunder"))
    }

    @Test
    fun unknownOrEmpty_isNull_notInventedWeather() {
        assertNull(MetNoSymbolMapper.toWmo(null))
        assertNull(MetNoSymbolMapper.toWmo(""))
        assertNull(MetNoSymbolMapper.toWmo("unknown_symbol"))
    }
}
