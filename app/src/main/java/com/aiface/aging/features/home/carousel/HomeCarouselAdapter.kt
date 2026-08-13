package com.aiface.aging.features.home.carousel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemHomeCarouselCardBinding
import com.aiface.aging.features.home.HomeCarouselItem
import com.aiface.aging.shared.setSafeClickListener

class HomeCarouselAdapter(
    private val items: List<HomeCarouselItem>,
    private val onItemClick: (HomeCarouselItem) -> Unit,
) : RecyclerView.Adapter<HomeCarouselAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHomeCarouselCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(
        private val binding: ItemHomeCarouselCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeCarouselItem) {
            binding.ivCarouselThumb.setImageResource(item.imageRes)
            binding.tvCarouselTitle.text = item.title
            binding.root.setSafeClickListener { onItemClick(item) }
        }
    }
}
