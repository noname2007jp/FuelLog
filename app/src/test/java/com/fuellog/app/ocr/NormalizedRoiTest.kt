package com.fuellog.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizedRoiTest {

    @Test
    fun movingRoiKeepsItInsideImageBounds() {
        val roi = NormalizedRoi(RoiTarget.DATE, 0.2f, 0.2f, 0.5f, 0.4f)
        val moved = roi.move(0.8f, -0.5f)
        assertEquals(0.7f, moved.left, 0.0001f)
        assertEquals(0.0f, moved.top, 0.0001f)
        assertEquals(1.0f, moved.right, 0.0001f)
        assertEquals(0.2f, moved.bottom, 0.0001f)
    }

    @Test
    fun resizingFromEachCornerKeepsMinimumSizeAndBounds() {
        val roi = NormalizedRoi(RoiTarget.DATE, 0.2f, 0.2f, 0.5f, 0.5f)
        val topLeft = roi.resizeFrom(RoiCorner.TOP_LEFT, 0.8f, 0.8f)
        assertEquals(0.49f, topLeft.left, 0.0001f)
        assertEquals(0.49f, topLeft.top, 0.0001f)
        assertEquals(0.5f, topLeft.right, 0.0001f)
        assertEquals(0.5f, topLeft.bottom, 0.0001f)

        val bottomRight = roi.resizeFrom(RoiCorner.BOTTOM_RIGHT, 1f, 1f)
        assertEquals(1f, bottomRight.right, 0.0001f)
        assertEquals(1f, bottomRight.bottom, 0.0001f)
    }
}
