package com.aiface.aging.features.see_all

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemSeeAllTemplateBinding
import com.aiface.aging.domain.model.Template
import com.aiface.aging.shared.TemplateThumbLoader
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.setSafeClickListener

class SeeAllAdapter(
    private val onTemplateClick: (Template) -> Unit,
    private val context: FragmentActivity
) : ListAdapter<Template, SeeAllAdapter.TemplateViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Template>() {
        override fun areItemsTheSame(oldItem: Template, newItem: Template): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Template, newItem: Template): Boolean =
            oldItem == newItem
    }

    inner class TemplateViewHolder(
        private val binding: ItemSeeAllTemplateBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Template) {
            binding.tvTitle.text =
                item.title.takeUnless { it.isNullOrBlank() } ?: item.prompt.orEmpty()
            TemplateThumbLoader.load(
                imageView = binding.image,
                thumbnailUrl = item.thumbnailUrl,
                mediaUrl = item.mediaUrl,
                context = context,
                gifUrl = item.gifUrl,
            )
            binding.tvPremium.visibility =
                if (item.isPro && isProVersion.value == false) View.VISIBLE else View.GONE
            binding.root.setSafeClickListener { onTemplateClick(item) }
        }

        fun clearImage() {
            TemplateThumbLoader.clear(binding.image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemSeeAllTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: TemplateViewHolder) {
        super.onViewRecycled(holder)
        holder.clearImage()
    }
}
