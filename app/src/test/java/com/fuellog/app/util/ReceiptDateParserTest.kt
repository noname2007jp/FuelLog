package com.fuellog.app.util

import com.fuellog.app.ocr.OcrAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReceiptDateParserTest {

    private val today = LocalDate.of(2026, 10, 11)

    @Test
    fun parsesSupportedWesternFormats() {
        assertEquals(LocalDate.of(2024, 5, 1), ReceiptDateParser.parse("2024/05/01", today))
        assertEquals(LocalDate.of(2024, 5, 1), ReceiptDateParser.parse("2024-5-1", today))
        assertEquals(LocalDate.of(2024, 5, 1), ReceiptDateParser.parse("2024.05.01", today))
        assertEquals(LocalDate.of(2024, 5, 1), ReceiptDateParser.parse("２０２４年５月１日", today))
    }

    @Test
    fun parsesJapaneseEraFormatsAndValidatesEraBoundaries() {
        assertEquals(LocalDate.of(2024, 5, 1), ReceiptDateParser.parse("令和6年5月1日", today))
        assertEquals(LocalDate.of(2024, 5, 1), ReceiptDateParser.parse("R6.5.1", today))
        assertEquals(LocalDate.of(2019, 4, 30), ReceiptDateParser.parse("平成31年4月30日", today))
        assertNull(ReceiptDateParser.parse("平成31年5月1日", today))
    }

    @Test
    fun appliesTwoDigitYearAndDateRangeRules() {
        assertEquals(LocalDate.of(2024, 5, 1), ReceiptDateParser.parse("24/05/01", today))
        assertEquals(LocalDate.of(1980, 5, 1), ReceiptDateParser.parse("80/05/01", today))
        assertNull(ReceiptDateParser.parse("70/05/01", today))
        assertNull(ReceiptDateParser.parse("2027/05/01", today))
        assertNull(ReceiptDateParser.parse("2024/02/30", today))
    }

    @Test
    fun classifiesDashboardLabelsAndMergesOdometerDecimalOnlyWhenThousandsMatch() {
        val result = OcrAnalyzer.parseDashboard(
            """ODO
123456
TRIP A
123456.7
TRIP B
321.4
AVG
15.6 km/L"""
        )
        assertEquals(123456.7, result.odometerKm!!, 0.0001)
        assertEquals(123456.7, result.tripAKm!!, 0.0001)
        assertEquals(321.4, result.tripBKm!!, 0.0001)
        assertEquals(15.6, result.vehicleFuelEconomyKmPerLiter!!, 0.0001)

        assertEquals(123456.0, OcrAnalyzer.mergeOdometerWithTripADecimal(123456.0, 123.4), 0.0001)
        assertEquals(123456.7, OcrAnalyzer.mergeOdometerWithTripADecimal(123456.0, 123456.7), 0.0001)
    }

    @Test
    fun combinesOdometerIntegerAndDecimalRois() {
        assertEquals(48532.7, OcrAnalyzer.combineOdometer("48,532", "7")!!, 0.0001)
        assertNull(OcrAnalyzer.combineOdometer("", "7"))
        assertNull(OcrAnalyzer.combineOdometer("48532", ""))
    }
}
