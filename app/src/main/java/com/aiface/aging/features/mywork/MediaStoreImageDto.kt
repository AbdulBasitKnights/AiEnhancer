package com.aiface.aging.features.mywork

import android.net.Uri
import java.util.Date

data class MediaStoreImageDto(
    val id: Long,
    val displayName: String,
    val dateAdded: Date,
    val contentUri: Uri,
    val path: String
)

fun MediaStoreImageDto.toMediaStoreImage() : MediaStoreImage{
    return MediaStoreImage(id, displayName, contentUri, path,dateAdded)
}