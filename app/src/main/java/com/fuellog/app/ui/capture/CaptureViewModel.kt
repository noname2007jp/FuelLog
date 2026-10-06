package com.fuellog.app.ui.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fuellog.app.ocr.OcrAnalyzer

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

    var isProcessing: Boolean by mutableStateOf(false); private set
    var errorMessage: String? by mutableStateOf(null); private set
    var infoMessage: String? by mutableStateOf(null); private set

    fun onReceiptRecognized(path: String, text: String) {
        receiptPhotoPath = path
        val result = OcrAnalyzer.parseReceipt(text)
        fuelLiters = result.fuelLiters ?: fuelLiters
        costYen = result.costYen ?: costYen
        unitPrice = result.unitPrice ?: unitPrice
        infoMessage = if (result.fuelLiters != null) {
            "レシートを読み取りました"
        } else {
            "レシートの数値を読み取れませんでした。確認画面で手入力してください"
        }
        errorMessage = null
    }

    fun onOdometerRecognized(path: String, text: String) {
        meterPhotoPath = path
        val value = OcrAnalyzer.parseOdometer(text)
        odometerKm = value ?: odometerKm
        infoMessage = if (value != null) {
            "オドメーターを読み取りました"
        } else {
            "数値を読み取れませんでした。確認画面で手入力してください"
        }
        errorMessage = null
    }

    fun onTripRecognized(path: String, text: String) {
        meterPhotoPath = path
        val value = OcrAnalyzer.parseTrip(text)
        tripKm = value ?: tripKm
        infoMessage = if (value != null) {
            "トリップメーターを読み取りました"
        } else {
            "数値を読み取れませんでした。確認画面で手入力してください"
        }
        errorMessage = null
    }

    fun updateProcessing(processing: Boolean) {
        isProcessing = processing
    }

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
        isProcessing = false
        errorMessage = null
        infoMessage = null
    }
}
