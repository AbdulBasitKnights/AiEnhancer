package com.aiface.aging.features.filters.model

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ModelFiltersPackDto(
    var id: Int = 0,
    var title: String? = null,
    var event: String? = null,
    @SerializedName("cat_id")
    var catId: Int? = null,
    var cover: String? = null,
    var file: String? = null,
    var rule: String? = null,
    var intensity: String? = null,
    var editor: String? = null,
    @SerializedName("tag_title")
    var tagTitle: String? = null,
    @SerializedName("tag_img")
    var tagImg: String? = null,
    var state: String? = null,
    var bitmap: Bitmap? = null
)

fun ModelFiltersPackDto.toModelFiltersPack() : ModelFilterPack {
    return ModelFilterPack(id, rule, intensity,bitmap)
}
