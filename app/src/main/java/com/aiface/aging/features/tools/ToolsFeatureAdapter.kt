package com.aiface.aging.features.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemToolsBinding
import com.aiface.aging.shared.setSafeClickListener

class ToolsFeatureAdapter(
    private var list: List<ToolsFeature>,
    private val onItemClick: (ToolsFeature) -> Unit,
) : RecyclerView.Adapter<ToolsFeatureAdapter.FeatureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        return FeatureViewHolder(
            ItemToolsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
        holder.itemView.setSafeClickListener { onItemClick(item) }
    }

    inner class FeatureViewHolder(
        private val binding: ItemToolsBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ToolsFeature) {
            binding.img.setImageDrawable(item.img)
            binding.tvTitle.text = item.title
        }
    }

    fun updateList(newList: List<ToolsFeature>) {
        val diffResult = DiffUtil.calculateDiff(ToolsFeatureDiffCallback(list, newList))
        list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    private class ToolsFeatureDiffCallback(
        private val oldList: List<ToolsFeature>,
        private val newList: List<ToolsFeature>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}
