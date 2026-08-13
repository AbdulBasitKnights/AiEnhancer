package com.aiface.aging.features.collage.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ModelFrameHomeCategories(
    var id: Int = 0,
    var title: String? = null,
//    var parent: String = "Top",
//    @TypeConverters(Converters::class)
//    var packList: ArrayList<ModelFramePack> = arrayListOf(),
//    var showSeeAll: Boolean = true,
//    var event: String? = null
) : Parcelable
