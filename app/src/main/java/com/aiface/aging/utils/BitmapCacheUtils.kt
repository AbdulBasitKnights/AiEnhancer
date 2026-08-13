package com.aiface.aging.utils

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

fun saveBitmapToTempCache(context: Context, bitmap: Bitmap): String? {
    return try {
        val outputFile = File.createTempFile("editor_", ".png", context.cacheDir)
        if (outputFile.exists()) outputFile.delete()
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        outputFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
