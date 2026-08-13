package com.aiface.aging.features.blender.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemBlenderPackBinding
import com.aiface.aging.features.editor.model.ModelFramePack

class BlenderPackAdapter(
    private val onClick: (ModelFramePack) -> Unit,
) : RecyclerView.Adapter<BlenderPackAdapter.Holder>() {

    private val items = mutableListOf<ModelFramePack>()

    fun submit(list: List<ModelFramePack>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBlenderPackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(
        private val binding: ItemBlenderPackBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ModelFramePack) {
            val url = item.cover?.takeIf { it.isNotBlank() }
                ?: item.file?.takeIf { it.isNotBlank() }
            Glide.with(binding.ivCover)
                .load(url)
                .placeholder(R.drawable.placeholder_icon)
                .error(R.drawable.placeholder_icon)
                .centerCrop()
                .into(binding.ivCover)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
