package com.fuellog.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImporterTest {
    @Test
    fun importsCurrentFuelLogCsvColumns() {
        val result = CsvImporter.parse(
            "日付,オドメーター(km),トリップA(km),トリップB(km),給油量(L),計算燃費(km/L),車載燃費(km/L),金額(円),単価(円/L)\n" +
                "2026-10-01,123456.7,123456.7,321.4,30.25,10.6,15.6,5000,165.29"
        )
        assertEquals(1, result.records.size)
        val record = result.records.single()
        assertEquals(123456.7, record.odometerKm!!, 0.0001)
        assertEquals(123456.7, record.tripAKm!!, 0.0001)
        assertEquals(321.4, record.tripKm!!, 0.0001)
        assertEquals(15.6, record.vehicleFuelEconomyKmPerLiter!!, 0.0001)
    }

    @Test
    fun importsPreviousCsvFormatAndSkipsInvalidRows() {
        val result = CsvImporter.parse(
            "日付,オドメーター(km),区間距離(km),給油量(L),燃費(km/L),金額(円),単価(円/L)\n" +
                "2026-10-01,123456,321.4,30.25,10.6,5000,165.29\n" +
                "bad-date,123457,200,20,10,3000,150"
        )
        assertEquals(1, result.records.size)
        assertEquals(321.4, result.records.single().tripKm!!, 0.0001)
        assertEquals(1, result.skippedRows)
        assertTrue(result.errors.isNotEmpty())
    }
}
