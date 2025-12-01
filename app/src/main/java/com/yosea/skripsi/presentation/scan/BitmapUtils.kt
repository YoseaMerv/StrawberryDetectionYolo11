package com.yosea.skripsi.presentation.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import java.io.InputStream

object BitmapUtils {

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Cek Rotasi EXIF
            val inputStreamForExif = context.contentResolver.openInputStream(uri)
            val exif = inputStreamForExif?.let { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            inputStreamForExif?.close()

            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            // Putar Bitmap jika perlu
            return if (rotation != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotation)
                Bitmap.createBitmap(
                    originalBitmap, 0, 0,
                    originalBitmap.width, originalBitmap.height,
                    matrix, true
                )
            } else {
                originalBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}