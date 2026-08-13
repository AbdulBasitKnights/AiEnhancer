package com.aiface.aging.features.look.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R

data class HairColorItem(
    val colorRes: Int,
)

class HairColorAdapter(
    private val items: List<HairColorItem>,
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<HairColorAdapter.HairColorViewHolder>() {

    inner class HairColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img_hair_color)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HairColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hair_color, parent, false)
        return HairColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: HairColorViewHolder, position: Int) {
        val item = items[position]
        val color = ContextCompat.getColor(holder.itemView.context, item.colorRes)
        holder.img.setImageDrawable(null)
        holder.img.setBackgroundColor(color)
        holder.img.setOnClickListener { onClick(item.colorRes) }
    }

    override fun getItemCount() = items.size
}
