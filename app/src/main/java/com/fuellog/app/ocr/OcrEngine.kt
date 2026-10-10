package com.fuellog.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** ML Kit によるオンデバイスOCR。 */
class OcrEngine(private val context: Context) {

    private val japaneseRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }

    /** レシート全体を読み取る。 */
    suspend fun recognizeReceipt(uri: Uri): String = recognize(uri, useJapanese = true)

    /** メーター画像全体を読み取る。 */
    suspend fun recognizeMeter(uri: Uri): String = recognize(uri, useJapanese = false)

    /**
     * 正規化ROI（画像比率0..1）で指定した範囲だけをOCRする。
     * ImageUtils.normalizeForDisplayAndOcr() の出力画像を渡すこと。
     */
    suspend fun recognizeRegion(file: java.io.File, roi: NormalizedRoi, useJapanese: Boolean = false): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "画像を読み込めません" }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("画像を読み込めません")
        val left = (roi.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (roi.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (roi.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (roi.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        if (cropped !== bitmap) bitmap.recycle()
        return try {
            recognizeBitmap(cropped, useJapanese)
        } finally {
            cropped.recycle()
        }
    }

    /** オドメーター整数部と小数部を別ROIで読み取り、1桁小数として連結する。 */
    suspend fun recognizeOdometer(file: java.io.File, integerRoi: NormalizedRoi, decimalRoi: NormalizedRoi): Double? {
        require(integerRoi.target == RoiTarget.ODOMETER_INTEGER)
        require(decimalRoi.target == RoiTarget.ODOMETER_DECIMAL)
        val integerText = recognizeRegion(file, integerRoi)
        val decimalText = recognizeRegion(file, decimalRoi)
        return OcrAnalyzer.combineOdometer(integerText, decimalText)
    }

    private suspend fun recognize(uri: Uri, useJapanese: Boolean): String =
        suspendCancellableCoroutine { cont ->
            val image = try {
                InputImage.fromFilePath(context, uri)
            } catch (e: Exception) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            process(image, useJapanese, cont)
        }

    private suspend fun recognizeBitmap(bitmap: Bitmap, useJapanese: Boolean): String =
        suspendCancellableCoroutine { cont ->
            val image = try {
                InputImage.fromBitmap(bitmap, 0)
            } catch (e: Exception) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            process(image, useJapanese, cont)
        }

    private fun process(
        image: InputImage,
        useJapanese: Boolean,
        cont: CancellableContinuation<String>
    ) {
        val client = if (useJapanese) japaneseRecognizer else latinRecognizer
        client.process(image)
            .addOnSuccessListener { if (cont.context.isActive) cont.resume(it.text) }
            .addOnFailureListener { if (cont.context.isActive) cont.resumeWithException(it) }
    }
}
