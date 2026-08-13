package com.aiface.aging.features.home.carousel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemHomeForYouSectionBinding
import com.aiface.aging.features.home.HomeCarouselItem
import com.aiface.aging.features.home.HomeItem
import com.aiface.aging.shared.setSafeClickListener

class HomeCarouselSectionViewHolder(
    private val binding: ItemHomeForYouSectionBinding,
    private val onSeeAllClick: () -> Unit,
    private val onItemClick: (HomeCarouselItem) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(section: HomeItem.CarouselSection) {
        binding.tvForYouTitle.setText(section.titleRes)
        binding.rvForYouTools.adapter = HomeCarouselAdapter(section.items, onItemClick)
        binding.btnForYouSeeAll.setSafeClickListener { onSeeAllClick() }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onSeeAllClick: () -> Unit,
            onItemClick: (HomeCarouselItem) -> Unit,
        ): HomeCarouselSectionViewHolder {
            val binding = ItemHomeForYouSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return HomeCarouselSectionViewHolder(binding, onSeeAllClick, onItemClick)
        }
    }
}
