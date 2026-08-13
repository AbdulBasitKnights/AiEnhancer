package com.aiface.aging.features.collage


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.utils.AppUtils


class AdapterCollageHeader(
    private val context: Context,
    private val list: List<String>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedItemPosition: Int = 0
    private var listener: CollageTypeClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_collage_header, parent, false)
        return FrameViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setSelectedPosition(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        (holder as FrameViewHolder)
        holder.category.text = item

        holder.itemView.setOnClickListener {
            listener?.onCollageTypeClick(position, item)
            selectedItemPosition = holder.adapterPosition
            notifyDataSetChanged()
        }
        setCustomColor(holder.category, position)

    }

    inner class FrameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val category: TextView = this.itemView.findViewById(R.id.frame_head_txt)
    }

    private fun setCustomColor(textView: TextView, position: Int) {
        textView.setTextColor(
            if (selectedItemPosition == position) ContextCompat.getColor(
                context, R.color.white
            ) else {
                ContextCompat.getColor(
                    context, R.color.white
                )
            }
        )

        textView.background =
            if (selectedItemPosition == position) ContextCompat.getDrawable(
                context, R.drawable.bg_selected_card
            ) else {
                ContextCompat.getDrawable(
                    context, R.drawable.bg_collage_header_unselected
                )
            }
    }


    fun setCtgListener(listener: CollageTypeClickListener) {
        this.listener = listener
    }

}

interface CollageTypeClickListener {
    fun onCollageTypeClick(position: Int, item: String)
}

