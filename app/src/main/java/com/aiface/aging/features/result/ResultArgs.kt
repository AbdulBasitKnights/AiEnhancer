package com.aiface.aging.features.result

import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import com.aiface.aging.data.model.NewGenerateResponse
import com.aiface.aging.features.share.ExtrasShareImageActivity

enum class ResultSource {
    AI,
    HAIR_COLOR,
    FACE_MAKEUP,
    PHOTO_EDITOR,
    COLLAGE,
    BG_REMOVER,
    PHOTO_BLENDER,
    BODY_EDITOR,
    MY_WORK,
}

object ResultArgs {
    const val SOURCE = "result_source"
    const val NEW_GENERATE_RESPONSE = "new_generate_response"
    const val OUTPUT_IMAGE_URL = "output_image_url"
    const val PROMPT = "prompt"
    const val LOCAL_IMAGE_URI = "local_image_uri"
    const val LOCAL_IMAGE_PATH = "local_image_path"
    const val FROM_MY_WORK = "from_my_work"
    const val MEDIA_ID = "media_id"
    const val DISPLAY_NAME = "display_name"
    const val ALREADY_SAVED = "already_saved"
    const val SOURCE_FEATURE = "source_feature"

    fun ai(response: NewGenerateResponse, sourceFeature: String? = null): Bundle =
        bundleOf(
            SOURCE to ResultSource.AI.name,
            NEW_GENERATE_RESPONSE to response,
        ).also { bundle ->
            if (!sourceFeature.isNullOrBlank()) {
                bundle.putString(SOURCE_FEATURE, sourceFeature)
            }
        }

    fun look(sourceFeature: String): Bundle =
        bundleOf(
            SOURCE to
                when (sourceFeature) {
                    "face_makeup" -> ResultSource.FACE_MAKEUP.name
                    else -> ResultSource.HAIR_COLOR.name
                },
            SOURCE_FEATURE to sourceFeature,
        )

    fun localSaved(
        source: ResultSource,
        path: String? = null,
        uri: Uri? = null,
        fromMyWork: Boolean = false,
        mediaId: Long? = null,
        displayName: String? = null,
    ): Bundle =
        bundleOf(
            SOURCE to source.name,
            LOCAL_IMAGE_PATH to path,
            LOCAL_IMAGE_URI to uri?.toString(),
            FROM_MY_WORK to fromMyWork,
            MEDIA_ID to mediaId,
            DISPLAY_NAME to displayName,
            ALREADY_SAVED to true,
        )

    /** Editor Next → Preview: local file not yet in gallery. */
    fun localPending(
        source: ResultSource,
        path: String? = null,
        uri: Uri? = null,
    ): Bundle =
        bundleOf(
            SOURCE to source.name,
            LOCAL_IMAGE_PATH to path,
            LOCAL_IMAGE_URI to uri?.toString(),
            FROM_MY_WORK to false,
            ALREADY_SAVED to false,
        )

    fun fromShareExtras(
        extras: ExtrasShareImageActivity,
        source: ResultSource,
    ): Bundle =
        localSaved(
            source = source,
            path = extras.path,
            uri = extras.uri,
            fromMyWork = extras.fromMyWork,
            mediaId = extras.id,
            displayName = extras.displayName,
        )

    fun readSource(bundle: Bundle?): ResultSource {
        val raw = bundle?.getString(SOURCE).orEmpty()
        return runCatching { ResultSource.valueOf(raw) }.getOrDefault(ResultSource.AI)
    }
}
