package com.aiface.aging.features.mywork

import android.net.Uri
import androidx.annotation.Keep
import java.util.Date
@Keep
data class MediaStoreImage(
    val id: Long,
    val displayName: String,
    val contentUri: Uri,
    val path: String,
    val dateAdded : Date?=null
)
@Keep
data class MediaStoreVideo(
    val id: Int,
    val name: String,
    val video_url: String?
)
