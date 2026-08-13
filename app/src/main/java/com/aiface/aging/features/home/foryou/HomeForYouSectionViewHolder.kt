package com.aiface.aging.features.home.foryou

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemHomeForYouSectionBinding
import com.aiface.aging.features.tools.ToolsFeature
import com.aiface.aging.shared.setSafeClickListener

/**
 * Section row for Home "For You" tools.
 * Layout: [item_home_for_you_section.xml] — edit that file to customize title/arrow/list.
 */
class HomeForYouSectionViewHolder(
    private val binding: ItemHomeForYouSectionBinding,
    private val onSeeAllClick: () -> Unit,
    private val onToolClick: (ToolsFeature) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(tools: List<ToolsFeature>) {
        binding.rvForYouTools.adapter = HomeForYouToolAdapter(tools, onToolClick)
        binding.btnForYouSeeAll.setSafeClickListener { onSeeAllClick() }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onSeeAllClick: () -> Unit,
            onToolClick: (ToolsFeature) -> Unit,
        ): HomeForYouSectionViewHolder {
            val binding =
                ItemHomeForYouSectionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            return HomeForYouSectionViewHolder(binding, onSeeAllClick, onToolClick)
        }
    }
}
