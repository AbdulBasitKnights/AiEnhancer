package com.aiface.aging.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class RegisterRequest(
    @SerializedName("device_id")
    val deviceId: String,
)

@Keep
data class RegisterResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("data")
    val data: RegisterDataDto?,
    @SerializedName("message")
    val message: String?,
)

@Keep
data class RegisterDataDto(
    @SerializedName("token")
    val token: String,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("app_name")
    val appName: String,
    @SerializedName("user_id")
    val userId: String,
    /** ISO-8601 expiry timestamp when provided by backend. */
    @SerializedName("expires_at")
    val expiresAt: String? = null,
    /** Lifetime in seconds when provided by backend. */
    @SerializedName("expires_in")
    val expiresIn: Long? = null,
)
