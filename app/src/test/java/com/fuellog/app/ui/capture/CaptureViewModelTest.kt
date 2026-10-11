package com.fuellog.app.ui.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureViewModelTest {

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
