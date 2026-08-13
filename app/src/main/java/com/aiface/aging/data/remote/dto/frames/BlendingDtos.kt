package com.aiface.aging.data.remote.dto.frames

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.aiface.aging.features.editor.model.ModelFrameHeader
import com.aiface.aging.features.editor.model.ModelFramePack
import com.aiface.aging.features.blender.catalog.BlenderCategory

@Keep
data class ModelFramesHeaderDto(
    var id: Int = 0,
    var title: String? = null,
    var actionbar: String? = null,
    var event: String? = null,
    var cover: String? = null,
    var key: String? = null,
    var parent: String? = null,
    var access: String? = null,
    var orientation: String? = null,
    @SerializedName("tag_title")
    var tagTitle: String? = null,
    @SerializedName("tag_img")
    var tagImg: String? = null,
    var state: String? = null,
)

@Keep
data class ModelFramePackDto(
    var id: Int = 0,
    var title: String? = null,
    var event: String? = null,
    @SerializedName("cat_id")
    var catId: Int? = null,
    var cover: String? = null,
    var file: String? = null,
    var mask1: String? = null,
    var mask2: String? = null,
    var mask3: String? = null,
    var dimensionFrame: String? = null,
    var constraintSet1: String? = null,
    var constraintSet2: String? = null,
    var constraintSet3: String? = null,
    @SerializedName("gif_file")
    var gif_file: String? = null,
    var editor: String? = null,
    @SerializedName("tag_title")
    var tagTitle: String? = null,
    var state: String? = null,
)

fun ModelFramesHeaderDto.toModelFramesHeader(): ModelFrameHeader =
    ModelFrameHeader(id, title, parent ?: "Blending")

fun ModelFramePackDto.toModelFramesPack(): ModelFramePack =
    ModelFramePack(
        id,
        title,
        catId,
        cover,
        file,
        gif_file,
        mask1,
        mask2,
        mask3,
        dimensionFrame,
        constraintSet1,
        constraintSet2,
        constraintSet3,
        editor,
        tagTitle,
        state,
    )

fun toBlenderCategory(
    header: ModelFramesHeaderDto,
    packs: List<ModelFramePack>,
): BlenderCategory =
    BlenderCategory(
        id = header.id,
        title = header.title.orEmpty(),
        packs = ArrayList(packs),
    )
