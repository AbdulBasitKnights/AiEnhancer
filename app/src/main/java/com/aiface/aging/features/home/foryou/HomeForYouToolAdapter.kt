package com.aiface.aging.features.home.foryou

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemHomeForYouToolBinding
import com.aiface.aging.features.tools.ToolsFeature
import com.aiface.aging.shared.setSafeClickListener

/**
 * Horizontal adapter for Home "For You" tool cards.
 * Layout: [item_home_for_you_tool.xml] — edit that file to customize card UI.
 */
class HomeForYouToolAdapter(
    private val tools: List<ToolsFeature>,
    private val onToolClick: (ToolsFeature) -> Unit,
) : RecyclerView.Adapter<HomeForYouToolAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding =
            ItemHomeForYouToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding, onToolClick)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(tools[position])
    }

    override fun getItemCount(): Int = tools.size

    class Holder(
        private val binding: ItemHomeForYouToolBinding,
        private val onToolClick: (ToolsFeature) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ToolsFeature) {
            binding.ivForYouTool.setImageDrawable(item.img)
            binding.tvForYouToolTitle.text = item.title
            binding.root.setSafeClickListener { onToolClick(item) }
        }
    }
}
