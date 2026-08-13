package com.aiface.aging.features.look.haircolor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aiface.aging.R
import com.aiface.aging.features.look.adapter.HairColorItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class HairEditorViewModel @Inject constructor() : ViewModel() {

    var cropedImage: Bitmap? = null

    var finalBitmap: Bitmap? = null

    val colorList = listOf(
        HairColorItem(R.color.hair_color1), HairColorItem(R.color.hair_color2), HairColorItem(R.color.hair_color3),
        HairColorItem(R.color.hair_color4), HairColorItem(R.color.hair_color5), HairColorItem(R.color.hair_color6),
        HairColorItem(R.color.hair_color7), HairColorItem(R.color.hair_color8), HairColorItem(R.color.hair_color9),
        HairColorItem(R.color.hair_color10), HairColorItem(R.color.hair_color11), HairColorItem(R.color.hair_color12),
        HairColorItem(R.color.hair_color13), HairColorItem(R.color.hair_color14), HairColorItem(R.color.hair_color15),
        HairColorItem(R.color.hair_color16), HairColorItem(R.color.hair_color17), HairColorItem(R.color.hair_color18),
        HairColorItem(R.color.hair_color19), HairColorItem(R.color.hair_color20), HairColorItem(R.color.hair_color21),
        HairColorItem(R.color.hair_color22), HairColorItem(R.color.hair_color23), HairColorItem(R.color.hair_color24),
        HairColorItem(R.color.hair_color25), HairColorItem(R.color.hair_color26), HairColorItem(R.color.hair_color27),
        HairColorItem(R.color.hair_color28), HairColorItem(R.color.hair_color29), HairColorItem(R.color.hair_color30),
        HairColorItem(R.color.hair_color31), HairColorItem(R.color.hair_color32), HairColorItem(R.color.hair_color33),
        HairColorItem(R.color.hair_color34), HairColorItem(R.color.hair_color35), HairColorItem(R.color.hair_color36),
        HairColorItem(R.color.hair_color37), HairColorItem(R.color.hair_color38), HairColorItem(R.color.hair_color39),
    )

    private val _selectedColor = MutableLiveData<Int?>() // store color int
    val selectedColor: LiveData<Int?> = _selectedColor

    private val _opacity = MutableLiveData<Float>(0.0f)
    val opacity: LiveData<Float> = _opacity

    fun setSelectedColor(colorInt: Int) {
        _selectedColor.value = colorInt
    }

    fun setOpacity(opacity: Float) {
        _opacity.value = opacity
    }

    override fun onCleared() {
        cropedImage?.takeIf { !it.isRecycled }?.recycle()
        finalBitmap?.takeIf { !it.isRecycled }?.recycle()
        cropedImage = null
        finalBitmap = null
        super.onCleared()
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
        if (bitmap.isRecycled) return null

        val saveBitmap =
            try {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } catch (e: Exception) {
                null
            } ?: return null

        val resolver = context.applicationContext.contentResolver
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"

        return try {
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + File.separator + "ProCapture",
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    } else {
                        @Suppress("DEPRECATION")
                        val picturesDir =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        val outputFile = File(picturesDir, "ProCapture").apply { mkdirs() }
                        put(MediaStore.Images.Media.DATA, File(outputFile, fileName).absolutePath)
                    }
                }

            val uri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    if (!saveBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Bitmap compress failed")
                    }
                } ?: throw IOException("Unable to open output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val published =
                        ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                    resolver.update(uri, published, null, null)
                }
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            if (saveBitmap !== bitmap && !saveBitmap.isRecycled) {
                saveBitmap.recycle()
            }
        }
    }


    fun clearColor(){
        _selectedColor.value=null
    }

}