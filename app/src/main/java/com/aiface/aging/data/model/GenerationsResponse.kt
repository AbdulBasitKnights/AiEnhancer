package com.aiface.aging.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GenerationsResponse(
    @SerializedName("status")
    val status: Int?,
    @SerializedName("data")
    val data: GenerationsPayload?,
    @SerializedName("message")
    val message: String?,
)

@Keep
data class GenerationsPayload(
    @SerializedName("data")
    val data: List<GenerationDto>?,
    @SerializedName("meta")
    val meta: GenerationsMeta?,
)

@Keep
data class GenerationDto(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("prompt")
    val prompt: String?,
    @SerializedName("media_id")
    val mediaId: Int?,
    @SerializedName("input_image_url")
    val inputImageUrl: String?,
    @SerializedName("output_image_url")
    val outputImageUrl: String?,
    @SerializedName("device_id")
    val deviceId: String?,
    @SerializedName("user_identifier")
    val userIdentifier: String?,
    @SerializedName("created_at")
    val createdAt: String?,
)

@Keep
data class GenerationsMeta(
    @SerializedName("page")
    val page: Int?,
    @SerializedName("page_size")
    val pageSize: Int?,
    @SerializedName("total")
    val total: Int?,
    @SerializedName("has_next")
    val hasNext: Boolean?,
)
