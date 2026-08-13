package com.aiface.aging.features.faceswap.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.data.model.faceswap.FaceSwapTemplateDto
import com.aiface.aging.databinding.ItemFaceSwapTemplateBinding

fun interface FaceSwapTemplateCallback {
    fun onTemplateClick(template: FaceSwapTemplateDto)
}

class FaceSwapTemplateAdapter(
    private val listener: FaceSwapTemplateCallback,
) : ListAdapter<FaceSwapTemplateDto, FaceSwapTemplateAdapter.TemplateViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        return TemplateViewHolder(
            ItemFaceSwapTemplateBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TemplateViewHolder(
        private val binding: ItemFaceSwapTemplateBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                listener.onTemplateClick(getItem(position))
            }
        }

        fun bind(item: FaceSwapTemplateDto) {
            val thumb = item.thumbnailUrl?.takeIf { it.isNotBlank() }
                ?: item.previewUrl?.takeIf { it.isNotBlank() }
                ?: item.imageUrl
            Glide.with(binding.ivTemplate)
                .load(thumb)
                .placeholder(R.drawable.placeholder_icon)
                .centerCrop()
                .into(binding.ivTemplate)
            binding.tvTitle.text = item.name.orEmpty()
        }
    }

    private object Diff : DiffUtil.ItemCallback<FaceSwapTemplateDto>() {
        override fun areItemsTheSame(
            oldItem: FaceSwapTemplateDto,
            newItem: FaceSwapTemplateDto,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: FaceSwapTemplateDto,
            newItem: FaceSwapTemplateDto,
        ): Boolean = oldItem == newItem
    }
}
