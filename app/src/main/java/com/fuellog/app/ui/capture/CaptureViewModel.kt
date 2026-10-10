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
    ODOMETER("🚗 オドメーター", "オドメーター(総走行距離)の数字を撮影してください"),
    TRIP("🔁 トリップ", "トリップメーター(区間距離)の数字を撮影してください")
}

class CaptureViewModel : ViewModel() {
    var receiptPhotoPath: String? by mutableStateOf(null); private set
    var meterPhotoPath: String? by mutableStateOf(null); private set
    var fuelLiters: Double? by mutableStateOf(null); private set
    var costYen: Int? by mutableStateOf(null); private set
    var unitPrice: Double? by mutableStateOf(null); private set
    var odometerKm: Double? by mutableStateOf(null); private set
    var tripKm: Double? by mutableStateOf(null); private set
    var recognizedDate: LocalDate by mutableStateOf(LocalDate.now()); private set
    var dateOrigin: DateOrigin by mutableStateOf(DateOrigin.TODAY); private set

    var isProcessing: Boolean by mutableStateOf(false); private set
    var errorMessage: String? by mutableStateOf(null); private set
    var infoMessage: String? by mutableStateOf(null); private set

    fun onReceiptRecognized(path: String, text: String) {
        receiptPhotoPath = path
        val result = OcrAnalyzer.parseReceipt(text)
        fuelLiters = result.fuelLiters ?: fuelLiters
        costYen = result.costYen ?: costYen
        unitPrice = result.unitPrice ?: unitPrice
        result.receiptDate?.let {
            recognizedDate = it
            dateOrigin = DateOrigin.RECEIPT
        }
        infoMessage = if (result.fuelLiters != null || result.receiptDate != null) {
            "レシートを読み取りました"
        } else {
            "レシートの数値・日付を読み取れませんでした。確認画面で修正してください"
        }
        errorMessage = null
    }

    /** Exif日時が有効な場合のみ、レシート日付が未検出のときに採用する。 */
    fun applyExifDate(date: LocalDate?) {
        if (dateOrigin != DateOrigin.RECEIPT && date != null &&
            date.year >= 1971 && !date.isAfter(LocalDate.now())) {
            recognizedDate = date
            dateOrigin = DateOrigin.EXIF
        }
    }

    fun onOdometerRecognized(path: String, text: String) {
        meterPhotoPath = path
        val value = OcrAnalyzer.parseOdometer(text)
        odometerKm = value ?: odometerKm
        infoMessage = if (value != null) "オドメーターを読み取りました"
            else "数値を読み取れませんでした。確認画面で手入力してください"
        errorMessage = null
    }

    fun onTripRecognized(path: String, text: String) {
        meterPhotoPath = path
        val value = OcrAnalyzer.parseTrip(text)
        tripKm = value ?: tripKm
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
        tripKm = null
        recognizedDate = LocalDate.now()
        dateOrigin = DateOrigin.TODAY
        isProcessing = false
        errorMessage = null
        infoMessage = null
    }
}