package com.aiface.aging.data.remote.dto.frames

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.aiface.aging.features.blender.catalog.BlenderCategory
import com.aiface.aging.features.editor.model.ModelFramePack

/** Combined categories+packs response from api/getCategoriesAndFrames */
@Keep
data class ModelFrameHomeCategoriesDto(
    var id: Int = 0,
    var title: String? = null,
    var parent: String? = "Top",
    var event: String? = null,
    @SerializedName("packList")
    var packList: ArrayList<ModelFramePackDto> = arrayListOf(),
)
@Keep
fun ModelFrameHomeCategoriesDto.toBlenderCategory(): BlenderCategory =
    BlenderCategory(
        id = id,
        title = title.orEmpty(),
        packs = ArrayList(packList.map { it.toModelFramesPack() }),
    )
