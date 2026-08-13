package com.aiface.aging.features.faceswap.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.data.model.faceswap.FaceSwapCategoryDto
import com.aiface.aging.databinding.ItemFaceSwapCategoryBinding
import com.aiface.aging.utils.AppUtils

fun interface FaceSwapCategoryCallback {
    fun onCategoryClick(position: Int, category: FaceSwapCategoryDto)
}

class FaceSwapCategoryAdapter(
    private val listener: FaceSwapCategoryCallback,
) : ListAdapter<FaceSwapCategoryDto, FaceSwapCategoryAdapter.CategoryViewHolder>(Diff) {

    private var selectedPosition = 0

    fun selectMode(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        return CategoryViewHolder(
            ItemFaceSwapCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), position, selectedPosition)
    }

    inner class CategoryViewHolder(
        private val binding: ItemFaceSwapCategoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                if (selectedPosition == position) return@setOnClickListener
                selectMode(position)
                listener.onCategoryClick(position, getItem(position))
            }
        }

        fun bind(item: FaceSwapCategoryDto, position: Int, selectedItemPosition: Int) {
            binding.tvCategory.text = item.name.orEmpty()
            highlightItemAt(position, selectedItemPosition)
        }

        private fun highlightItemAt(position: Int, selectedItemPosition: Int) {
            val context = binding.root.context
            // Same selected/unselected treatment as AdapterFrameHeader (non-editor).
            binding.tvCategory.setTextColor(
                if (selectedItemPosition == position) {
                    ContextCompat.getColor(context, R.color.white)
                } else {
                    if (AppUtils.isNightMode(context)) {
                        ContextCompat.getColor(context, R.color.black)
                    } else {
                        ContextCompat.getColor(context, R.color.grey)
                    }
                },
            )
            binding.tvCategory.background =
                if (selectedItemPosition == position) {
                    ContextCompat.getDrawable(context, R.drawable.rounded_filled_dark_blue)
                } else {
                    ContextCompat.getDrawable(context, R.drawable.rounded_border_white)
                }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FaceSwapCategoryDto>() {
        override fun areItemsTheSame(
            oldItem: FaceSwapCategoryDto,
            newItem: FaceSwapCategoryDto,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: FaceSwapCategoryDto,
            newItem: FaceSwapCategoryDto,
        ): Boolean = oldItem == newItem
    }
}
