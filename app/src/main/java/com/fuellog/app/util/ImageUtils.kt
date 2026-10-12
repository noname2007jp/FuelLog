package com.fuellog.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ImageUtils {
    /** ギャラリーで選択した画像をアプリの写真フォルダへコピー */
    fun copyToPhotoDir(context: Context, uri: Uri, photoDir: File): File? {
        return try {
            val file = File(photoDir, "IMG_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file
        } catch (_: Exception) {
            null
        }
    }

    /** Exifの撮影日を取得。取得不能な場合はnull。 */
    fun readExifDate(file: File): LocalDate? = try {
        val exif = ExifInterface(file)
        val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: return null
        LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")).toLocalDate()
    } catch (_: Exception) {
        null
    }

    /**
     * Exif向きを1回だけ反映し、長辺がmaxLongEdge以下のJPEGを生成する。
     * OCRと表示に同じ出力ファイルを使うことで座標系を一致させる。
     */
    fun normalizeForDisplayAndOcr(source: File, photoDir: File, maxLongEdge: Int = 1600): File? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val sample = maxOf(1, kotlin.math.ceil(
                maxOf(bounds.outWidth, bounds.outHeight).toDouble() / maxLongEdge
            ).toInt())
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = BitmapFactory.decodeFile(source.absolutePath, options) ?: return null
            val exif = ExifInterface(source)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            }
            val oriented = if (orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED) decoded
            else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }

            val scale = minOf(1f, maxLongEdge.toFloat() / maxOf(oriented.width, oriented.height))
            val finalBitmap = if (scale < 1f) Bitmap.createScaledBitmap(
                oriented, (oriented.width * scale).toInt().coerceAtLeast(1),
                (oriented.height * scale).toInt().coerceAtLeast(1), true
            ) else oriented
            val output = File(photoDir, "OCR_${System.currentTimeMillis()}.jpg")
            output.outputStream().use { stream ->
                if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)) return null
            }
            if (finalBitmap !== oriented) finalBitmap.recycle()
            if (oriented !== decoded) oriented.recycle()
            else decoded.recycle()
            output
        } catch (_: Exception) {
            null
        }
    }
}
