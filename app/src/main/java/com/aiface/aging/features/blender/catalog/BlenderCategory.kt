package com.aiface.aging.features.blender.catalog

import com.aiface.aging.features.editor.model.ModelFramePack

data class BlenderCategory(
    val id: Int,
    val title: String,
    val packs: ArrayList<ModelFramePack> = arrayListOf(),
)
