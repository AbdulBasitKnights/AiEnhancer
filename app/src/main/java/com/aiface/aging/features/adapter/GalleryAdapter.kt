package com.aiface.aging.features.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.aiface.aging.R


class GalleryAdapter(
    private val context: Context,
    private val items: List<Uri>,
    private val onClick: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val playIcon: ImageView = view.findViewById(R.id.playSign)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = items[position]
        Glide.with(context)
            .load(uri)
            .centerCrop()
            .into(holder.thumbnail)


        if (uri.toString().contains("video"))
            holder.playIcon.visibility = View.VISIBLE
        else
            holder.playIcon.visibility = View.GONE

        holder.thumbnail.setOnClickListener { onClick(uri) }
    }

    override fun getItemCount() = items.size
}
