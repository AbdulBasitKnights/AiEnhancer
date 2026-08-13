package com.aiface.aging.features.frames.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemFramePackBinding
import com.aiface.aging.databinding.ItemFramePackBottomBinding
import com.aiface.aging.features.editor.model.ModelFramePack

/**
 * Catalog (grid) or editor-bottom (horizontal strip) frame pack thumbs.
 */
class FramesPackAdapter(
    private val editorBottom: Boolean = false,
    private val onClick: (ModelFramePack) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ModelFramePack>()

    fun submit(list: List<ModelFramePack>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (editorBottom) TYPE_BOTTOM else TYPE_CATALOG

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_BOTTOM) {
            BottomHolder(ItemFramePackBottomBinding.inflate(inflater, parent, false))
        } else {
            CatalogHolder(ItemFramePackBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is CatalogHolder -> holder.bind(item)
            is BottomHolder -> holder.bind(item)
        }
    }

    private fun thumbUrl(item: ModelFramePack): String? =
        item.cover?.takeIf { it.isNotBlank() }
            ?: item.file?.takeIf { it.isNotBlank() }

    private fun loadThumb(target: android.widget.ImageView, item: ModelFramePack) {
        Glide.with(target)
            .load(thumbUrl(item))
            .placeholder(R.drawable.placeholder_icon)
            .error(R.drawable.placeholder_icon)
            .centerCrop()
            .into(target)
    }

    inner class CatalogHolder(
        private val binding: ItemFramePackBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModelFramePack) {
            loadThumb(binding.frameHeadImg, item)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    inner class BottomHolder(
        private val binding: ItemFramePackBottomBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModelFramePack) {
            loadThumb(binding.frameHeadImg, item)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private const val TYPE_CATALOG = 0
        private const val TYPE_BOTTOM = 1
    }
}
