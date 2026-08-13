package com.aiface.aging.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CategoryResponse(
    @SerializedName("status")
    val status: Int?,
    @SerializedName("data")
    val data: CategoryResponseData?,
    @SerializedName("message")
    val message: String?,
)

@Keep
data class CategoryResponseData(
    @SerializedName("data")
    val data: List<CategoryDto>,
    @SerializedName("meta")
    val meta: PagingMeta?,
)

@Keep
data class PagingMeta(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("total")
    val total: Int,
    @SerializedName("has_next")
    val hasNext: Boolean,
)

@Keep
data class CategoryDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("position")
    val position: Int,
    @SerializedName("artwork_count")
    val artworkCount: Int,
    @SerializedName("media_items")
    val mediaItems: List<MediaItemDto>,
)

@Keep
data class MediaItemDto(
    @SerializedName("media_type")
    val mediaType: String,
    @SerializedName("is_premium")
    val isPremium: Boolean,
    @SerializedName("active")
    val active: Boolean,
    @SerializedName("title")
    val title: String,
    @SerializedName("prompt")
    val prompt: String?,
    @SerializedName("id")
    val id: Int,
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("original_filename")
    val originalFilename: String?,
    @SerializedName("file_path")
    val filePath: String?,
    @SerializedName("gif_path")
    val gifPath: String?,
    @SerializedName("created_at")
    val createdAt: String?,
)
