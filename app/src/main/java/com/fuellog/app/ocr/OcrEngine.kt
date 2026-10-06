package com.fuellog.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** ML Kit によるオンデバイスOCR。日本語モデルは数字も認識可能。 */
class OcrEngine(private val context: Context) {

    private val japaneseRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }

    /** レシート用: 日本語 + 数字 */
    suspend fun recognizeReceipt(uri: Uri): String = recognize(uri, useJapanese = true)

    /** メーター用: 数字中心のラテンモデル */
    suspend fun recognizeMeter(uri: Uri): String = recognize(uri, useJapanese = false)

    private suspend fun recognize(uri: Uri, useJapanese: Boolean): String =
        suspendCancellableCoroutine { cont ->
            val image = try {
                InputImage.fromFilePath(context, uri)
            } catch (e: Exception) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            val client = if (useJapanese) japaneseRecognizer else latinRecognizer
            client.process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}
