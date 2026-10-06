package com.fuellog.app.util

import android.content.Context
import android.net.Uri
import java.io.File

object ImageUtils {

    /** ギャラリーで選択した画像をアプリの写真フォルダへコピー */
    fun copyToPhotoDir(context: Context, uri: Uri, photoDir: File): File? {
        return try {
            val file = File(photoDir, "IMG_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file
        } catch (e: Exception) {
            null
        }
    }
}
