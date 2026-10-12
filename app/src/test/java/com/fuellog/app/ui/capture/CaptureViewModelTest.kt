package com.fuellog.app.ui.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureViewModelTest {



    @Test
    fun printedReceiptDateOverridesExifDate() {
        val viewModel = CaptureViewModel()
        viewModel.applyExifDate(java.time.LocalDate.of(2025, 3, 4))

        viewModel.onReceiptRecognized(
            "/tmp/receipt.jpg",
            "給油量 30.25\n単価 168.5\n合計 5000\n日付 2024/05/01"
        )

        assertEquals(java.time.LocalDate.of(2024, 5, 1), viewModel.recognizedDate)
        assertEquals(com.fuellog.app.util.DateOrigin.RECEIPT, viewModel.dateOrigin)
    }

    @Test
    fun dashboardOcrSetsOdometerTripAAndTripBSeparately() {
        val viewModel = CaptureViewModel()
        viewModel.onDashboardRecognized(
            "/tmp/dashboard-a.jpg",
            """ODO
123456
TRIP A
123456.7
TRIP B
3456.4""",
            CaptureTarget.ODOMETER
        )
        viewModel.onDashboardRecognized(
            "/tmp/dashboard-b.jpg",
            """TRIP B
3456.4
AVG
15.6 km/L""",
            CaptureTarget.TRIP
        )
        assertEquals(123456.4, viewModel.odometerKm!!, 0.0001)
        assertEquals(123456.7, viewModel.tripAKm!!, 0.0001)
        assertEquals(3456.4, viewModel.tripKm!!, 0.0001)
        assertEquals(15.6, viewModel.vehicleFuelEconomyKmPerLiter!!, 0.0001)
    }

    @Test
    fun failedMeterOcrDoesNotKeepPreviousReading() {
        val viewModel = CaptureViewModel()

        viewModel.onOdometerRecognized("/tmp/meter1.jpg", "48532.7")
        assertEquals(48532.7, viewModel.odometerKm!!, 0.0001)

        viewModel.onOdometerRecognized("/tmp/meter2.jpg", "数字を認識できません")
        assertNull(viewModel.odometerKm)

        viewModel.onTripRecognized("/tmp/trip1.jpg", "123.4")
        assertEquals(123.4, viewModel.tripKm!!, 0.0001)

        viewModel.onTripRecognized("/tmp/trip2.jpg", "数字を認識できません")
        assertNull(viewModel.tripKm)
    }

    @Test
    fun newReceiptDoesNotKeepValuesFromPreviousImageWhenOcrFails() {
        val viewModel = CaptureViewModel()

        viewModel.onReceiptRecognized(
            "/tmp/first.jpg",
            "給油量 30.25\n単価 168.5\n合計 5000"
        )
        assertEquals(30.25, viewModel.fuelLiters!!, 0.0001)
        assertEquals(5000, viewModel.costYen)
        assertEquals(168.5, viewModel.unitPrice!!, 0.0001)

        viewModel.onReceiptRecognized("/tmp/second.jpg", "読み取れる数値はありません")

        assertNull(viewModel.fuelLiters)
        assertNull(viewModel.costYen)
        assertNull(viewModel.unitPrice)
    }
}
