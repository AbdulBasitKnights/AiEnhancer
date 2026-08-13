package com.aiface.aging.features.imgpicker.adapter

import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemAlbumBinding
import com.aiface.aging.features.imgpicker.base.BaseRecyclerViewAdapter
import com.aiface.aging.features.imgpicker.base.BaseViewHolder
import com.aiface.aging.features.imgpicker.builder.TedImagePickerBaseBuilder
import com.aiface.aging.features.imgpicker.model.Album
import com.aiface.aging.features.imgpicker.util.TextFormatUtil

internal class AlbumAdapter(private val builder: TedImagePickerBaseBuilder<*>?) :
    BaseRecyclerViewAdapter<Album, AlbumAdapter.AlbumViewHolder>() {

    private var selectedPosition = 0

    override fun getViewHolder(parent: ViewGroup, viewType: ViewType) = AlbumViewHolder(parent)

    fun setSelectedAlbum(album: Album) {
        val index = items.indexOf(album)
        if (index >= 0 && selectedPosition != index) {
            val lastSelectedPosition = selectedPosition
            selectedPosition = index
            notifyItemChanged(lastSelectedPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    inner class AlbumViewHolder(parent: ViewGroup) :
        BaseViewHolder<ItemAlbumBinding, Album>(parent, R.layout.item_album) {
        override fun bind(data: Album) {
            binding.album = data
            binding.isSelected = adapterPosition == selectedPosition
            binding.mediaCountText =
                builder?.let {
                    TextFormatUtil.getMediaCountText(
                        it.imageCountFormat,
                        data.mediaCount
                    )
                }
        }

        override fun recycled() {
            try {
                Glide.with(itemView).clear(binding.ivImage)
            } catch (e: Exception) {

            }
        }
    }
}