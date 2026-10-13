package com.otomemo.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

object ImageStore {

    private const val DIR = "service_images"
    private const val MAX_DIM = 1280

    private fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    fun load(context: Context, name: String): Bitmap? =
        BitmapFactory.decodeFile(file(context, name).absolutePath)

    fun loadScaled(context: Context, name: String, req: Int): Bitmap? {
        val path = file(context, name).absolutePath
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= req && bounds.outHeight / (sample * 2) >= req) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            path, BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }

    fun delete(context: Context, name: String) {
        file(context, name).delete()
    }

    fun saveFromUri(context: Context, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
            boundsStream.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > MAX_DIM || bounds.outHeight / sample > MAX_DIM) {
                sample *= 2
            }
            val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = decodeStream.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null

            val name = "img_${System.currentTimeMillis()}.jpg"
            file(context, name).outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()
            name
        } catch (e: Exception) {
            android.util.Log.e("ImageStore", "saveFromUri gagal", e)
            null
        }
    }
}
