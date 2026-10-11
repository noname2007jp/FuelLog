package com.fuellog.app.ui.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fuellog.app.ocr.OcrAnalyzer
import com.fuellog.app.util.DateOrigin
import java.time.LocalDate

enum class CaptureTarget(val label: String, val guide: String) {
    RECEIPT("🧾 レシート", "レシート全体を枠に合わせてください"),
    ODOMETER("🚗 メーター", "ODO・トリップA/B・車載燃費の表示を含めて撮影してください"),
    TRIP("🔁 再読み取り", "メーター全体を撮影し、ODO・トリップA/B・車載燃費を再判定します")
}

class CaptureViewModel : ViewModel() {
    var receiptPhotoPath: String? by mutableStateOf(null); private set
    var meterPhotoPath: String? by mutableStateOf(null); private set
    var fuelLiters: Double? by mutableStateOf(null); private set
    var costYen: Int? by mutableStateOf(null); private set
    var unitPrice: Double? by mutableStateOf(null); private set
    var odometerKm: Double? by mutableStateOf(null); private set
    var tripAKm: Double? by mutableStateOf(null); private set
    var tripKm: Double? by mutableStateOf(null); private set
    var vehicleFuelEconomyKmPerLiter: Double? by mutableStateOf(null); private set
    var recognizedDate: LocalDate by mutableStateOf(LocalDate.now()); private set
    var dateOrigin: DateOrigin by mutableStateOf(DateOrigin.TODAY); private set

    var isProcessing: Boolean by mutableStateOf(false); private set
    var errorMessage: String? by mutableStateOf(null); private set
    var infoMessage: String? by mutableStateOf(null); private set

    fun onReceiptRecognized(path: String, text: String) {
        receiptPhotoPath = path
        val result = OcrAnalyzer.parseReceipt(text)
        // 新しいレシートの結果で置き換える。読み取れなかった項目に前の画像の値を残さない。
        fuelLiters = result.fuelLiters
        costYen = result.costYen
        unitPrice = result.unitPrice
        result.receiptDate?.let {
            recognizedDate = it
            dateOrigin = DateOrigin.RECEIPT
        }
        infoMessage = if (
            result.fuelLiters != null || result.costYen != null ||
            result.unitPrice != null || result.receiptDate != null
        ) {
            "レシートを読み取りました"
        } else {
            "レシートの数値・日付を読み取れませんでした。確認画面で修正してください"
        }
        errorMessage = null
    }

    /** 新しいレシート画像の処理開始時に日付ソースを初期化し、有効なExif日付を仮採用する。 */
    fun applyExifDate(date: LocalDate?) {
        recognizedDate = LocalDate.now()
        dateOrigin = DateOrigin.TODAY
        if (date != null && date.year >= 1971 && !date.isAfter(LocalDate.now())) {
            recognizedDate = date
            dateOrigin = DateOrigin.EXIF
        }
    }

    /** Read the full dashboard so ODO, Trip A/B and the vehicle economy can be identified together. */
    fun onDashboardRecognized(path: String, text: String, target: CaptureTarget) {
        meterPhotoPath = path
        val result = OcrAnalyzer.parseDashboard(text)
        val fallbackOdometer = if (target == CaptureTarget.ODOMETER &&
            result.tripAKm == null && result.tripBKm == null
        ) {
            OcrAnalyzer.parseOdometer(text)?.let { OcrAnalyzer.mergeOdometerWithTripADecimal(it, result.tripAKm) }
        } else null
        val fallbackTripB = if (target == CaptureTarget.TRIP &&
            result.tripAKm == null && result.tripBKm == null
        ) OcrAnalyzer.parseTrip(text) else null
        odometerKm = result.odometerKm ?: fallbackOdometer
        tripAKm = result.tripAKm
        tripKm = result.tripBKm ?: fallbackTripB
        vehicleFuelEconomyKmPerLiter = result.vehicleFuelEconomyKmPerLiter
        val count = listOf(odometerKm, tripAKm, tripKm, vehicleFuelEconomyKmPerLiter).count { it != null }
        infoMessage = if (count > 0) {
            "メーターを読み取りました（${count}項目）。必要に応じて確認・修正してください"
        } else {
            "メーターの値を判定できませんでした。確認画面で手入力してください"
        }
        errorMessage = null
    }

    fun onOdometerRecognized(path: String, text: String) {
        meterPhotoPath = path
        val value = OcrAnalyzer.parseOdometer(text)
        odometerKm = value
        infoMessage = if (value != null) "オドメーターを読み取りました"
            else "数値を読み取れませんでした。確認画面で手入力してください"
        errorMessage = null
    }

    fun onTripRecognized(path: String, text: String) {
        meterPhotoPath = path
        val value = OcrAnalyzer.parseTrip(text)
        tripKm = value
        infoMessage = if (value != null) "トリップメーターを読み取りました"
            else "数値を読み取れませんでした。確認画面で手入力してください"
        errorMessage = null
    }

    fun setDate(date: LocalDate) {
        recognizedDate = date
        dateOrigin = DateOrigin.MANUAL
    }

    fun setInfo(message: String?) { infoMessage = message }

    fun updateProcessing(processing: Boolean) { isProcessing = processing }

    fun setError(message: String?) {
        errorMessage = message
        if (message != null) infoMessage = null
    }

    fun reset() {
        receiptPhotoPath = null
        meterPhotoPath = null
        fuelLiters = null
        costYen = null
        unitPrice = null
        odometerKm = null
        tripAKm = null
        tripKm = null
        vehicleFuelEconomyKmPerLiter = null
        recognizedDate = LocalDate.now()
        dateOrigin = DateOrigin.TODAY
        isProcessing = false
        errorMessage = null
        infoMessage = null
    }
}