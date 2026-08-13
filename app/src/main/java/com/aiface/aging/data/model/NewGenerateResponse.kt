package com.aiface.aging.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
@Keep
@Parcelize
data class NewGenerateResponse(
    @SerializedName("status")
    val status: Int?,
    @SerializedName("data")
    val data: NewGenerateData?,
    @SerializedName("message")
    val message: String?,
) : Parcelable
@Keep
@Parcelize
data class NewGenerateData(
    @SerializedName("job_id")
    val jobId: String?,
    @SerializedName("generation_type")
    val generationType: String?,
    @SerializedName("status")
    val status: String?,
    /** URL of the generated output image. */
    @SerializedName("output_url")
    val outputUrl: String?,
    @SerializedName("credit_deducted")
    val creditDeducted: Boolean?,
    @SerializedName("video_credits_remaining")
    val videoCreditsRemaining: Int?,
    @SerializedName("image_credits_remaining")
    val imageCreditsRemaining: Int?,
    @SerializedName("counter")
    val counter: Int?,
    @SerializedName("vendor_message")
    val vendorMessage: String?,
    @SerializedName("prompt")
    val prompt: String?,
) : Parcelable
