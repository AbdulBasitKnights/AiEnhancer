package com.aiface.aging.features.filters.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ModelStickerPack(
    var id: Int = 0,
    var file: String? = null,
    var catId : Int? = null
) : Parcelable