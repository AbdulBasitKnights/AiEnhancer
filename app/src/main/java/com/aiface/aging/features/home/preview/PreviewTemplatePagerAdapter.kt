package com.aiface.aging.features.home.preview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemPreviewTemplatePageBinding
import com.aiface.aging.domain.model.Template
import com.aiface.aging.shared.TemplateThumbLoader
import com.aiface.aging.shared.setSafeClickListener

class PreviewTemplatePagerAdapter(
    private val onTryFeature: (Template) -> Unit,
) : RecyclerView.Adapter<PreviewTemplatePagerAdapter.Holder>() {

    private val items = mutableListOf<Template>()

    fun submit(list: List<Template>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Template? = items.getOrNull(position)

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemPreviewTemplatePageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(
        private val binding: ItemPreviewTemplatePageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Template) {
            TemplateThumbLoader.load(
                imageView = binding.ivTemplateThumb,
                thumbnailUrl = item.thumbnailUrl,
                mediaUrl = item.mediaUrl,
                gifUrl = item.gifUrl,
            )

            binding.tvTemplateTitle.text = item.title?.takeIf { it.isNotBlank() }
                ?: item.prompt.orEmpty()
            val description = item.prompt?.takeIf { it.isNotBlank() && it != item.title }
                ?: item.negativePrompt.orEmpty()
            binding.tvTemplateDescription.text = description
            binding.tvTemplateDescription.visibility =
                if (description.isBlank()) android.view.View.INVISIBLE else android.view.View.INVISIBLE

            binding.btnTryFeature.setSafeClickListener { onTryFeature(item) }
        }
    }
}
