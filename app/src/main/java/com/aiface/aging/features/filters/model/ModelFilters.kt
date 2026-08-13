package com.aiface.aging.features.filters.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ModelFilters(
    var id: Int = 0,
    var title: String? = null,
    var parent: String = "Top",
) : Parcelable