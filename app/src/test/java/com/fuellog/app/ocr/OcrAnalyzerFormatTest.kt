package com.fuellog.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrAnalyzerFormatTest {

    @Test
    fun parsesMeterValuesUsingExpectedDisplayFormats() {
        val result = OcrAnalyzer.parseDashboard(
            """
            ODO
            012345
            A 0000.0 km
            B 0123.4 km
            AVG 00.0 km/L
            """.trimIndent()
        )

        assertEquals(12345.0, result.odometerKm!!, 0.0001)
        assertEquals(0.0, result.tripAKm!!, 0.0001)
        assertEquals(123.4, result.tripBKm!!, 0.0001)
        assertEquals(0.0, result.vehicleFuelEconomyKmPerLiter!!, 0.0001)
    }

    @Test
    fun normalizesFullWidthDigitsAndDecimalPoint() {
        val result = OcrAnalyzer.parseDashboard(
            """
            ＯＤＯ ０１２３４５
            Ａ ００１２．３
            Ｂ ００４５．６
            平均燃費 １２．３ ｋｍ／Ｌ
            """.trimIndent()
        )

        assertEquals(12345.0, result.odometerKm!!, 0.0001)
        assertEquals(12.3, result.tripAKm!!, 0.0001)
        assertEquals(45.6, result.tripBKm!!, 0.0001)
        assertEquals(12.3, result.vehicleFuelEconomyKmPerLiter!!, 0.0001)
    }

    @Test
    fun doesNotInventValuesWhenExpectedLabelsAndNumbersAreMissing() {
        val result = OcrAnalyzer.parseDashboard("dashboard unreadable")

        assertNull(result.odometerKm)
        assertNull(result.tripAKm)
        assertNull(result.tripBKm)
        assertNull(result.vehicleFuelEconomyKmPerLiter)
    }
}
