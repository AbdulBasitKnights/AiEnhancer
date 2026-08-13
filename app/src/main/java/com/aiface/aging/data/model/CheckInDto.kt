package com.aiface.aging.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CheckInStateResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("data")
    val data: CheckInStateDto?,
    @SerializedName("message")
    val message: String?,
)

@Keep
data class CheckInStateDto(
    @SerializedName("device_id")
    val deviceId: String?,
    @SerializedName("cycle_day")
    val cycleDay: Int?,
    @SerializedName("last_claim_utc")
    val lastClaimUtc: String?,
    @SerializedName("total_credits")
    val totalCredits: Int?,
)

@Keep
data class CheckInClaimRequest(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("claim_utc")
    val claimUtc: String,
    @SerializedName("cycle_day")
    val cycleDay: Int,
    @SerializedName("reward")
    val reward: Int,
)

@Keep
data class CheckInClaimResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("data")
    val data: CheckInStateDto?,
    @SerializedName("message")
    val message: String?,
)
