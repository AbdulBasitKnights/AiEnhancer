package com.aiface.aging.features.imgpicker.builder.type

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
enum class MediaType : Parcelable {
    IMAGE,
    VIDEO,
    IMAGE_AND_VIDEO,
    ;
}
