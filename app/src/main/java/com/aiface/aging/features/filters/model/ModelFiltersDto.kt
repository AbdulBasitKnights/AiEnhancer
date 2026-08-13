package com.aiface.aging.features.filters.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class ModelFiltersDto(
    var id: Int = 0,
    var title: String? = null,
    var event: String? = null,
    var parent: String = "Top",
    var access: String? = null,
    @SerializedName("tag_title")
    var tagTitle: String? = null,
    @SerializedName("tag_img")
    var tagImg: String? = null,
    var state: String? = null,
    var option: String = "Top"
)

fun ModelFiltersDto.toModelFilters(): ModelFilters {
    return ModelFilters(id, title, parent)
}
