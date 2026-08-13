package com.aiface.aging.features.home.aging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.aiface.aging.R

class AgingTemplateAdapter(
    private val items: List<AgingTemplateOption>,
    private val onSelectionChanged: (AgingTemplateOption?) -> Unit,
) : RecyclerView.Adapter<AgingTemplateAdapter.ViewHolder>() {

    private var selectedIndex = -1

    fun setSelectedTemplateId(templateId: String?) {
        selectedIndex =
            if (templateId.isNullOrBlank()) {
                -1
            } else {
                items.indexOfFirst { it.templateId == templateId }.takeIf { it >= 0 } ?: -1
            }
        notifyDataSetChanged()
    }

    fun getSelected(): AgingTemplateOption? = items.getOrNull(selectedIndex)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.agingTemplateRoot)
        val thumbnail: ImageView = itemView.findViewById(R.id.agingTemplateImage)
        val title: TextView = itemView.findViewById(R.id.agingTemplateTitle)
        val selector: ImageView = itemView.findViewById(R.id.agingTemplateSelector)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_aging_template, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isSelected = position == selectedIndex

        bindThumbnail(holder.thumbnail, item)
        holder.title.text =
            item.displayTitle?.takeIf { it.isNotBlank() }
                ?: holder.itemView.context.getString(item.titleRes)
        holder.root.setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_aging_template_selected
            } else {
                R.drawable.bg_aging_template_unselected
            },
        )
        holder.selector.setImageResource(
            if (isSelected) {
                R.drawable.ic_radio_selected
            } else {
                R.drawable.ic_radio_unselected
            },
        )

        holder.itemView.setOnClickListener {
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            selectedIndex =
                if (selectedIndex == adapterPosition) {
                    onSelectionChanged(null)
                    -1
                } else {
                    onSelectionChanged(items[adapterPosition])
                    adapterPosition
                }
            notifyDataSetChanged()
        }
    }

    private fun bindThumbnail(imageView: ImageView, item: AgingTemplateOption) {
        val thumbnailUrl = item.thumbnailUrl
        if (!thumbnailUrl.isNullOrBlank()) {
            Glide.with(imageView)
                .load(thumbnailUrl)
                .placeholder(item.thumbnailRes)
                .error(item.thumbnailRes)
                .centerCrop()
                .into(imageView)
        } else {
            Glide.with(imageView).clear(imageView)
            imageView.setImageResource(item.thumbnailRes)
        }
    }
}
