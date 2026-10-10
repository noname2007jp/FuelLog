package com.fuellog.app.ocr

/** 画像比率(0..1)で保持するROI。画面サイズに依存しない。 */
enum class RoiTarget(val label: String) {
    ODOMETER_INTEGER("オド整数部"),
    ODOMETER_DECIMAL("オド小数部"),
    TRIP_INTEGER("トリップ整数部"),
    TRIP_DECIMAL("トリップ小数部"),
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
        copy(
            right = (right + rightDelta).coerceIn(left + 0.01f, 1f),
            bottom = (bottom + bottomDelta).coerceIn(top + 0.01f, 1f)
        )
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
