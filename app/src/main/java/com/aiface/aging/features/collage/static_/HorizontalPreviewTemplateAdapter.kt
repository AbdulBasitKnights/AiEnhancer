package com.aiface.aging.features.collage.static_

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.features.collage.model.TemplateItem
import com.aiface.aging.utils.PhotoUtils


/**
 * Created by vanhu_000 on 3/28/2016.
 */
class HorizontalPreviewTemplateAdapter(
    private val mTemplateItems: ArrayList<TemplateItem?>,
    private val mListener: OnPreviewTemplateClickListener?
) : RecyclerView.Adapter<HorizontalPreviewTemplateAdapter.PreviewTemplateViewHolder>() {
    class PreviewTemplateViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var mImageView: ImageView
        var layout : CardView

        init {
            mImageView = itemView.findViewById<ImageView>(R.id.imageView) as ImageView
            layout=itemView.findViewById(R.id.constraintLayout)
        }
    }

    interface OnPreviewTemplateClickListener {
        fun onPreviewTemplateClick(item: TemplateItem?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewTemplateViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_template_hor, parent, false)
        return PreviewTemplateViewHolder(v)
    }

    override fun onBindViewHolder(holder: PreviewTemplateViewHolder, position: Int) {
        PhotoUtils.loadImageWithGlide(
            holder.mImageView.context,
            holder.mImageView,
            mTemplateItems[holder.adapterPosition]?.preview
        )
//        val paddingInDp: Int = if (mTemplateItems[holder.adapterPosition]?.isSelected == true) {
//            2
//        } else {
//            0
//        }
//        val density = holder.mImageView.context.resources.displayMetrics.density
//        val paddingInPx = (paddingInDp * density).toInt()
//        holder.mImageView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)

        holder.mImageView.setOnClickListener { mListener?.onPreviewTemplateClick(mTemplateItems[holder.adapterPosition]) }
        if (mTemplateItems[holder.adapterPosition]?.isSelected==true){
            holder.layout.foreground=ContextCompat.getDrawable(holder.layout.context,R.drawable.bg_collage_bottom_selected)
        }else  holder.layout.foreground=ContextCompat.getDrawable(holder.layout.context,R.drawable.bg_collage_bottom_unselected)
    }

    override fun getItemCount(): Int {
        return mTemplateItems.size
    }
}