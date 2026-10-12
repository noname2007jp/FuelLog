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
    ODOMETER("🚗 メーターA", "ODOとトリップAの表示を含めて撮影してください"),
    TRIP("🔁 メーターB", "トリップBと車載燃費の表示を含めて撮影してください")
}

class CaptureViewModel : ViewModel() {
    var receiptPhotoPath: String? by mutableStateOf(null); private set
    var meterPhotoPath: String? by mutableStateOf(null); private set
    /** 確認画面で参照する一時画像。給油記録には保存しない。 */
    var meterAPhotoPath: String? by mutableStateOf(null); private set
    var meterBPhotoPath: String? by mutableStateOf(null); private set
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

    /**
     * メーターA写真からODOとTrip Aだけを採用する。
     * 写真自体は給油記録に紐付けず、読み取り結果だけを保持する。
     */
    fun onDashboardRecognized(path: String, text: String, target: CaptureTarget) {
        val result = OcrAnalyzer.parseDashboard(text)
        when (target) {
            CaptureTarget.ODOMETER -> {
                meterAPhotoPath = path
                odometerKm = result.odometerKm
                tripAKm = result.tripAKm
                val count = listOf(odometerKm, tripAKm).count { it != null }
                infoMessage = if (count > 0) {
                    "メーターAを読み取りました（ODO・トリップA：${count}項目）。必要に応じて確認・修正してください"
                } else {
                    "ODO・トリップAを判定できませんでした。確認画面で手入力してください"
                }
            }
            CaptureTarget.TRIP -> {
                meterBPhotoPath = path
                tripKm = result.tripBKm ?: if (result.tripAKm == null && result.tripBKm == null) {
                    OcrAnalyzer.parseTrip(text)
                } else null
                vehicleFuelEconomyKmPerLiter = result.vehicleFuelEconomyKmPerLiter
                val count = listOf(tripKm, vehicleFuelEconomyKmPerLiter).count { it != null }
                infoMessage = if (count > 0) {
                    "メーターBを読み取りました（トリップB・車載燃費：${count}項目）。必要に応じて確認・修正してください"
                } else {
                    "トリップB・車載燃費を判定できませんでした。確認画面で手入力してください"
                }
            }
            CaptureTarget.RECEIPT -> Unit
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
        // メーター写真は確認・修正が終わった時点で削除し、記録として残さない。
        listOfNotNull(meterAPhotoPath, meterBPhotoPath).distinct().forEach { path ->
            runCatching { java.io.File(path).delete() }
        }
        meterAPhotoPath = null
        meterBPhotoPath = null
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