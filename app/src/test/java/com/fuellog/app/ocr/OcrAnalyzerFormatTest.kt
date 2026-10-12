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
            B 2345.4 km
            A 00.0 km/L
            """.trimIndent()
        )

        assertEquals(12345.4, result.odometerKm!!, 0.0001)
        assertEquals(0.0, result.tripAKm!!, 0.0001)
        assertEquals(2345.4, result.tripBKm!!, 0.0001)
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

        assertEquals(12345.6, result.odometerKm!!, 0.0001)
        assertEquals(12.3, result.tripAKm!!, 0.0001)
        assertEquals(45.6, result.tripBKm!!, 0.0001)
        assertEquals(12.3, result.vehicleFuelEconomyKmPerLiter!!, 0.0001)
    }

    @Test
    fun parsesTripBWhenTripAndBLabelsAreSplitAcrossLines() {
        val result = OcrAnalyzer.parseDashboard(
            """
            ODO
            123456
            TRIP
            B
            3456.6 km
            AVG
            15.2 km/L
            """.trimIndent()
        )

        assertEquals(123456.6, result.odometerKm!!, 0.0001)
        assertEquals(3456.6, result.tripBKm!!, 0.0001)
    }

    @Test
    fun findsOdometerWhenOdoLabelWasMissedByOcr() {
        val result = OcrAnalyzer.parseDashboard(
            """
            123456
            TRIP B
            3456.6 km
            """.trimIndent()
        )

        assertEquals(123456.6, result.odometerKm!!, 0.0001)
        assertEquals(3456.6, result.tripBKm!!, 0.0001)
    }

    @Test
    fun doesNotMistakeTripBForOdometerWhenOdoDigitsAreMissing() {
        val result = OcrAnalyzer.parseDashboard(
            """
            ODO
            TRIP B
            2345.6 km
            """.trimIndent()
        )

        assertNull(result.odometerKm)
        assertEquals(2345.6, result.tripBKm!!, 0.0001)
    }

    @Test
    fun dashboardParsingUsesTripBDecimalEvenWhenWholeDigitsDoNotMatchOdoTail() {
        val result = OcrAnalyzer.parseDashboard(
            """
            ODO
            012345
            TRIP B
            9876.6 km
            """.trimIndent()
        )

        assertEquals(12345.6, result.odometerKm!!, 0.0001)
        assertEquals(9876.6, result.tripBKm!!, 0.0001)
    }

    @Test
    fun alwaysUsesTripBDecimalEvenWhenTripWholeDoesNotMatchOdometerTail() {
        assertEquals(
            12345.6,
            OcrAnalyzer.mergeOdometerWithTripBDecimal(12345.0, 9876.6),
            0.0001
        )
    }

    @Test
    fun keepsIntegerOdometerWhenTripBIsMissing() {
        assertEquals(
            12345.0,
            OcrAnalyzer.mergeOdometerWithTripBDecimal(12345.0, null),
            0.0001
        )
    }

    @Test
    fun doesNotTreatAverageEconomyLabelAAsTripA() {
        val result = OcrAnalyzer.parseDashboard("ODO 012345\nA 00.0 km/L")

        assertNull(result.tripAKm)
        assertEquals(0.0, result.vehicleFuelEconomyKmPerLiter!!, 0.0001)
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
