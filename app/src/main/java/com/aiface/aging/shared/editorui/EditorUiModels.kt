package com.aiface.aging.shared.editorui

import androidx.recyclerview.widget.DiffUtil

data class ModelDrawableAssets(
    var id: Int? = null,
    var drawable: Int? = null,
    var imageTitle: String? = null
)

class DrawableAssetsDiffCallback : DiffUtil.ItemCallback<ModelDrawableAssets>() {

    override fun areItemsTheSame(
        oldItem: ModelDrawableAssets,
        newItem: ModelDrawableAssets
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ModelDrawableAssets,
        newItem: ModelDrawableAssets
    ): Boolean {
        return oldItem == newItem
    }
}

data class ModelRatio(
    var id: Int? = null,
    var drawable: Int? = null,
    var imageTitle: String? = null,
    var ratio: String? = null,
    var background : Int?=null
)

class RatioDiffCallback : DiffUtil.ItemCallback<ModelRatio>() {

    override fun areItemsTheSame(
        oldItem: ModelRatio,
        newItem: ModelRatio
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ModelRatio,
        newItem: ModelRatio
    ): Boolean {
        return oldItem == newItem
    }
}
