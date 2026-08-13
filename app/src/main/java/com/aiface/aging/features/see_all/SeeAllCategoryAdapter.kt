package com.aiface.aging.features.see_all

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemFaceSwapCategoryBinding
import com.aiface.aging.domain.model.Category
import com.aiface.aging.shared.setSafeClickListener

fun interface SeeAllCategoryCallback {
    fun onCategoryClick(position: Int, category: Category)
}

/**
 * Horizontal category chips — same selected/unselected look as Face Swap.
 */
class SeeAllCategoryAdapter(
    private val listener: SeeAllCategoryCallback,
) : ListAdapter<Category, SeeAllCategoryAdapter.Holder>(Diff) {

    private var selectedPosition = 0

    fun selectMode(position: Int) {
        if (position !in 0 until itemCount) return
        selectedPosition = position
        notifyDataSetChanged()
    }

    fun selectedPosition(): Int = selectedPosition

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            ItemFaceSwapCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class Holder(
        private val binding: ItemFaceSwapCategoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setSafeClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setSafeClickListener
                if (selectedPosition == position) return@setSafeClickListener
                selectMode(position)
                listener.onCategoryClick(position, getItem(position))
            }
        }

        fun bind(item: Category, selected: Boolean) {
            val context = binding.root.context
            binding.tvCategory.text = item.name
            binding.tvCategory.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.white else R.color.grey,
                ),
            )
            binding.tvCategory.background = ContextCompat.getDrawable(
                context,
                if (selected) R.drawable.rounded_filled_dark_blue
                else R.drawable.rounded_border_white,
            )
        }
    }

    private object Diff : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean =
            oldItem == newItem
    }
}
