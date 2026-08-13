package com.aiface.aging.features.blender

import android.content.Context
import com.aiface.aging.R
import com.aiface.aging.features.blender.model.BlendEditorOptionsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlenderEditorRepo @Inject constructor() {

    fun getAllBlendEditorOptions(): ArrayList<BlendEditorOptionsModel> {
        return arrayListOf(
            BlendEditorOptionsModel(R.drawable.replace_icon, "Replace"),
            BlendEditorOptionsModel(R.drawable.frameicon, "Background"),
            BlendEditorOptionsModel(R.drawable.flip, "Flip"),
            BlendEditorOptionsModel(R.drawable.shapes, "Shapes"),
            BlendEditorOptionsModel(R.drawable.drop, "Blend")
        )
    }

    suspend fun getAllBlendShapeStyles(context: Context): ArrayList<String> {
        val shapeList = ArrayList<String>()
        withContext(Dispatchers.IO) {
            try {
                val files = context.assets.list("shapes") ?: return@withContext
                files.forEach { file ->
                    shapeList.add("file:///android_asset/shapes${File.separator}$file")
                }
            } catch (_: Exception) {
            }
        }
        return shapeList
    }
}
