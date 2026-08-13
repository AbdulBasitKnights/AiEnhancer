package com.aiface.aging.features.filters.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ModelFilterPack(
    var id: Int = 0,
    var rule: String? = null,
    var intensity: String? = null,
    var bitmap: Bitmap? = null
) : Parcelable