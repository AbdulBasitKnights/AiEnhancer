package com.aiface.aging.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class GenerateResponse(
    @SerializedName("data")
    val data: GenerateData?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: Int?,
) : Parcelable

@Keep
@Parcelize
data class GenerateData(
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("device_id")
    val deviceId: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("input_image_url")
    val inputImageUrl: String?,
    @SerializedName("media_id")
    val mediaId: String?,
    @SerializedName("output_image_url")
    val outputImageUrl: String?,
    @SerializedName("prompt")
    val prompt: String?,
    @SerializedName("user_identifier")
    val userIdentifier: String?,
) : Parcelable
