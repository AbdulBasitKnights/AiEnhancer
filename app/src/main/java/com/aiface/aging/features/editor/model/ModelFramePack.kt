package com.aiface.aging.features.editor.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ModelFramePack(
    var id: Int = 0,
    var title: String? = null,
    var cat_id: Int? = null,
    var cover: String? = null,//for Ai ImageToImage and Video Thumbnail
    var file: String? = null,
    var gif_file: String? = null,
    var mask1: String? = null,//for Ai ImageToImage and Video ImageCount
    var mask2: String? = null,//for Ai ImageToImage and Video CategoryName
    var mask3: String? = null,
    var dimensionFrame: String? = null,
    var constraintSet1: String? = null,
    var constraintSet2: String? = null,
    var constraintSet3: String? = null,
    var editor: String? = null,
    var tag_title: String? = null,
    var state: String? = null
) : Parcelable