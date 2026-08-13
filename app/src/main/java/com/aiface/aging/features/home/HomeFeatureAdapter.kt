package com.aiface.aging.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemFeatureBinding
import com.aiface.aging.shared.setSafeClickListener


class HomeFeatureAdapter(
    private var list: List<HomeFeature>,
    private val onItemClick: (HomeFeature) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return FeatureViewHolder(
            ItemFeatureBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        (holder as FeatureViewHolder).bind(item)
        holder.itemView.setSafeClickListener {
            onItemClick(item)
        }
    }

    inner class FeatureViewHolder(private val binding: ItemFeatureBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeFeature) {
            binding?.let { binding ->
                binding.img.setImageDrawable(item.img)
                binding.tvTitle.text = item.title
                }

            }

    }

    fun updateList(newList: List<HomeFeature>) {
        val diffResult = DiffUtil.calculateDiff(HomeFeatureDiffCallback(list, newList))
        list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    fun currentList() = list

    inner class HomeFeatureDiffCallback(
        private val oldList: List<HomeFeature>,
        private val newList: List<HomeFeature>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].title == newList[newItemPosition].title
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

}


