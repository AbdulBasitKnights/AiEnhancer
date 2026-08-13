package com.aiface.aging.data.model.faceswap

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
data class FaceSwapRegisterRequest(
    @SerializedName("device_id")
    val deviceId: String,
)

@Keep
data class FaceSwapBaseResponse<T>(
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("message")
    val message: String? = null,
)

@Keep
data class FaceSwapRegisterData(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("device_id")
    val deviceId: String? = null,
)

@Keep
data class FaceSwapItemsData<T>(
    @SerializedName("items")
    val items: List<T>? = null,
)

@Keep
data class FaceSwapCategoryDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("slug")
    val slug: String? = null,
    @SerializedName("sort_order")
    val sortOrder: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
)

@Keep
@Parcelize
data class FaceSwapTemplateDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("category_id")
    val categoryId: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("media_type")
    val mediaType: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("video_url")
    val videoUrl: String? = null,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerializedName("preview_url")
    val previewUrl: String? = null,
    @SerializedName("cache_status")
    val cacheStatus: String? = null,
    @SerializedName("cache_url")
    val cacheUrl: String? = null,
    @SerializedName("sort_order")
    val sortOrder: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    @SerializedName("image_count")
    val imageCount: Int? = null,
    @SerializedName("required_images")
    val requiredImages: Int? = null,
    @SerializedName("face_count")
    val faceCount: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
) : Parcelable {

    fun resolveRequiredImageCount(): Int {
        val count = imageCount ?: requiredImages ?: faceCount ?: 1
        return count.coerceAtLeast(1)
    }

    fun displayPreviewUrl(): String? =
        previewUrl?.takeIf { it.isNotBlank() }
            ?: imageUrl?.takeIf { it.isNotBlank() }
            ?: thumbnailUrl?.takeIf { it.isNotBlank() }
}

@Keep
data class FaceSwapGenerateData(
    @SerializedName("generation_id")
    val generationId: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("duration_ms")
    val durationMs: Long? = null,
)

@Keep
data class FaceSwapStatusData(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("source_image_url")
    val sourceImageUrl: String? = null,
    @SerializedName("target_image_url")
    val targetImageUrl: String? = null,
    @SerializedName("output_image_url")
    val outputImageUrl: String? = null,
    @SerializedName("template_id")
    val templateId: String? = null,
    @SerializedName("error_message")
    val errorMessage: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("started_at")
    val startedAt: String? = null,
    @SerializedName("completed_at")
    val completedAt: String? = null,
    @SerializedName("duration_ms")
    val durationMs: Long? = null,
)

fun String?.isTerminalFailed(): Boolean {
    val status = this?.lowercase()?.trim().orEmpty()
    return status == "failed" || status == "error" || status == "cancelled" || status == "canceled"
}

fun String?.isCompleted(): Boolean {
    val status = this?.lowercase()?.trim().orEmpty()
    return status == "completed" || status == "success" || status == "succeeded"
}

fun String?.isInProgress(): Boolean {
    val status = this?.lowercase()?.trim().orEmpty()
    return status == "pending" ||
        status == "queued" ||
        status == "processing" ||
        status == "running" ||
        status == "in_progress"
}
