package com.aiface.aging.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class NewCategoryResponse(
    @SerializedName("status")
    val status: Int?,
    @SerializedName("data")
    val data: NewCategoryData?,
    @SerializedName("message")
    val message: String?,
)

@Keep
data class NewCategoryData(
    @SerializedName("data")
    val data: List<NewCategoryDto>? = null,
    @SerializedName("meta")
    val meta: NewPagingMeta? = null,
)

@Keep
data class NewPagingMeta(
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
data class NewCategoryDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("app_name")
    val appName: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("position")
    val position: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("template_count")
    val templateCount: Int,
    @SerializedName("templates")
    val templates: List<NewTemplateDto>? = null,
)

@Keep
data class NewTemplateDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("category_id")
    val categoryId: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("generation_type")
    val generationType: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("priority")
    val priority: Int,
    @SerializedName("is_pro")
    val isPro: Boolean = false,
    /** Some backends send [is_premium] instead of [is_pro]. */
    @SerializedName("is_premium")
    val isPremium: Boolean = false,
    @SerializedName("image_count")
    val imageCount: Int?,
    @SerializedName("vendor_template_id")
    val vendorTemplateId: String?,
    @SerializedName("media_url")
    val mediaUrl: String?,
    @SerializedName("gif_url")
    val gifUrl: String?,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String?,
    @SerializedName("prompt")
    val prompt: String?,
    @SerializedName("negative_prompt")
    val negativePrompt: String?,
)
