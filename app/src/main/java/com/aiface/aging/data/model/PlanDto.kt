package com.aiface.aging.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
@Keep
data class PlansResponse(
    @SerializedName("status")
    val status: Int?,
    @SerializedName("data")
    val data: List<PlanDto>,
    @SerializedName("message")
    val message: String?,
)
@Keep
data class PlanDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("app_name")
    val appName: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("duration_days")
    val durationDays: Int,
    @SerializedName("video_generation_credits")
    val videoGenerationCredits: Int,
    @SerializedName("image_generation_credits")
    val imageGenerationCredits: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
)
