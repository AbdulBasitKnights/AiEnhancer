package com.aiface.aging.features.blender.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemFaceSwapCategoryBinding
import com.aiface.aging.shared.setSafeClickListener

/**
 * Horizontal category chips — same selected/unselected look as Face Swap.
 */
class BlenderHeaderAdapter(
    private val darkSurface: Boolean = false,
    private val onClick: (Int, BlenderCategory) -> Unit,
) : RecyclerView.Adapter<BlenderHeaderAdapter.Holder>() {

    private val items = mutableListOf<BlenderCategory>()
    private var selected = 0

    fun submit(list: List<BlenderCategory>, selectedIndex: Int = 0) {
        items.clear()
        items.addAll(list)
        selected = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        notifyDataSetChanged()
    }

    fun select(position: Int) {
        if (position !in items.indices || position == selected) return
        val old = selected
        selected = position
        notifyItemChanged(old)
        notifyItemChanged(selected)
    }

    fun selectedPosition(): Int = selected

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemFaceSwapCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position], position == selected)
    }

    inner class Holder(
        private val binding: ItemFaceSwapCategoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BlenderCategory, isSelected: Boolean) {
            val context = binding.root.context
            binding.tvCategory.text = item.title
            binding.tvCategory.setTextColor(
                ContextCompat.getColor(
                    context,
                    when {
                        isSelected -> R.color.white
                        darkSurface -> R.color.black
                        else -> R.color.black
                    },
                ),
            )
            if (darkSurface && !isSelected) {
                binding.tvCategory.alpha = 0.7f
            } else {
                binding.tvCategory.alpha = 1f
            }
            binding.tvCategory.background = ContextCompat.getDrawable(
                context,
                if (isSelected) {
                    R.drawable.rounded_filled_dark_blue
                } else {
                    R.drawable.rounded_border_white
                },
            )
            binding.root.setSafeClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    select(pos)
                    onClick(pos, items[pos])
                }
            }
        }
    }
}
