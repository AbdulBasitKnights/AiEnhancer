package com.aiface.aging.features.filters.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ModelStickerHeader(
    var id: Int = 0,
    var title: String? = null,
    var tagTitle: String? = null,
    var actionBar : String? = null
) : Parcelable