package com.aiface.aging.features.blender.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.databinding.DripingItemLayoutBinding

class BlendShapeStyleAdapter(private val onBgStyleItemClick: ((position: Int, ArrayList<String>) -> Unit)) :
    RecyclerView.Adapter<BlendShapeStyleAdapter.BgStyleViewHolder>() {

    private var bgStyleList: ArrayList<String> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BgStyleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DripingItemLayoutBinding.inflate(inflater, parent, false)
        return BgStyleViewHolder(binding)
    }


    override fun onBindViewHolder(holder: BgStyleViewHolder, position: Int) {

        Glide.with(holder.itemView).load(bgStyleList[position]).into(holder.binding.image)
        holder.binding.image.setColorFilter(
            ContextCompat.getColor(
                holder.itemView.context, R.color.purple
            ), android.graphics.PorterDuff.Mode.SRC_IN
        )
        holder.itemView.setOnClickListener {
            onBgStyleItemClick(position, bgStyleList)
        }

    }

    fun updateList(listOfEffectStyles: ArrayList<String>) {
        this.bgStyleList = listOfEffectStyles
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return bgStyleList.size
    }



    inner class BgStyleViewHolder(val binding: DripingItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)
}