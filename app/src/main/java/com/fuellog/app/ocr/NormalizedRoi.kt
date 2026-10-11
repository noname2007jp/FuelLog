package com.fuellog.app.ocr

/** ROI枠のリサイズ用ハンドル位置。 */
enum class RoiCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** 画像比率(0..1)で保持するROI。画面サイズに依存しない。 */
enum class RoiTarget(val label: String, val editable: Boolean = true) {
    // Kept for reading legacy saved settings, but no longer shown: the dashboard is scanned as one area.
    ODOMETER_INTEGER("オド整数部", editable = false),
    ODOMETER_DECIMAL("オド小数部", editable = false),
    TRIP_INTEGER("トリップ整数部", editable = false),
    TRIP_DECIMAL("トリップ小数部", editable = false),
    DASHBOARD("メーター全体", editable = false),
    RECEIPT_FRAME("レシート全体"),
    METER_A_FRAME("メーターA"),
    METER_B_FRAME("メーターB"),
    FUEL_AMOUNT("給油量"),
    UNIT_PRICE("単価"),
    TOTAL("合計"),
    DATE("日付"),
    WHOLE("全体")
}

data class NormalizedRoi(
    val target: RoiTarget,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(left in 0f..1f && top in 0f..1f)
        require(right in 0f..1f && bottom in 0f..1f)
        require(right > left && bottom > top)
    }

    fun move(dx: Float, dy: Float): NormalizedRoi {
        val w = right - left
        val h = bottom - top
        val x = (left + dx).coerceIn(0f, 1f - w)
        val y = (top + dy).coerceIn(0f, 1f - h)
        return copy(left = x, top = y, right = x + w, bottom = y + h)
    }

    fun resize(rightDelta: Float, bottomDelta: Float): NormalizedRoi =
        resizeFrom(RoiCorner.BOTTOM_RIGHT, rightDelta, bottomDelta)

    fun resizeFrom(corner: RoiCorner, dx: Float, dy: Float): NormalizedRoi = when (corner) {
        RoiCorner.TOP_LEFT -> copy(
            left = (left + dx).coerceIn(0f, right - 0.01f),
            top = (top + dy).coerceIn(0f, bottom - 0.01f)
        )
        RoiCorner.TOP_RIGHT -> copy(
            right = (right + dx).coerceIn(left + 0.01f, 1f),
            top = (top + dy).coerceIn(0f, bottom - 0.01f)
        )
        RoiCorner.BOTTOM_LEFT -> copy(
            left = (left + dx).coerceIn(0f, right - 0.01f),
            bottom = (bottom + dy).coerceIn(top + 0.01f, 1f)
        )
        RoiCorner.BOTTOM_RIGHT -> copy(
            right = (right + dx).coerceIn(left + 0.01f, 1f),
            bottom = (bottom + dy).coerceIn(top + 0.01f, 1f)
        )
    }
}

/** オドメーターの2領域をOCRして整数部と小数部を連結するための定義。 */
data class OdometerRoiPair(
    val integerRoi: NormalizedRoi,
    val decimalRoi: NormalizedRoi
) {
    init {
        require(integerRoi.target == RoiTarget.ODOMETER_INTEGER)
        require(decimalRoi.target == RoiTarget.ODOMETER_DECIMAL)
    }
}
