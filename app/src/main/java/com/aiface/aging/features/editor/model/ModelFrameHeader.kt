package com.aiface.aging.features.editor.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class ModelFrameHeader(
        var id: Int = 0,
        var title: String? = null,
        var parent: String = "Top",
) : Parcelable

