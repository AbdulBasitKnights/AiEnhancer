package com.aiface.aging.features.blender

import android.graphics.Bitmap
import android.net.Uri
import com.aiface.aging.utils.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlenderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Persist blended canvas to cache for Preview; gallery save on Preview. */
    fun saveBlend(bitmap: Bitmap): String? =
        ImageUtils.saveBitmapToCache(context, bitmap)
}

sealed class PhotoBlenderUiState {
    data object NeedBase : PhotoBlenderUiState()
    data object NeedCharacter : PhotoBlenderUiState()
    data object Masking : PhotoBlenderUiState()
    data class Ready(
        val baseUri: Uri,
        val characterCutout: Bitmap,
    ) : PhotoBlenderUiState()

    data object Saving : PhotoBlenderUiState()
    data class Saved(val path: String) : PhotoBlenderUiState()
    data class Error(val message: String) : PhotoBlenderUiState()
}
