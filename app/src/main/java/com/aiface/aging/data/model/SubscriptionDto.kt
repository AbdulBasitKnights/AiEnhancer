package com.aiface.aging.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
@Keep
data class CurrentSubscriptionResponse(
    @SerializedName("status")
    val status: Int?,
    @SerializedName("data")
    val data: SubscriptionDataDto?,
    @SerializedName("message")
    val message: String?,
)
@Keep
data class SubscriptionDataDto(
    @SerializedName("plan_id")
    val planId: String?,
    @SerializedName("plan_name")
    val planName: String?,
    @SerializedName("duration_days")
    val durationDays: Int?,
    @SerializedName("video_credits_remaining")
    val videoCreditsRemaining: Int,
    @SerializedName("image_credits_remaining")
    val imageCreditsRemaining: Int,
    @SerializedName("started_at")
    val startedAt: String?,
    @SerializedName("expires_at")
    val expiresAt: String?,
    @SerializedName("status")
    val status: String?,
)
@Keep
data class PurchaseSubscriptionRequest(
    @SerializedName("plan_uuid")
    val planUuid: String,
)
