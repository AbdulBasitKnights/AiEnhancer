package com.aiface.aging.features.collage

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemCollageBgBinding
import com.aiface.aging.shared.editorui.DrawableAssetsDiffCallback
import com.aiface.aging.shared.editorui.ModelDrawableAssets


interface BGsCallback {
    fun onBackgroundClick(position: Int, modelDrawableAssets: ModelDrawableAssets)
}

open class AdapterCollageBGs constructor(
    private val listener: BGsCallback,
    private val context: Context
) :
    ListAdapter<ModelDrawableAssets, RecyclerView.ViewHolder>(DrawableAssetsDiffCallback()) {

    private var selectedItemPosition: Int = -1

    fun unselectBottomItem() {
        selectedItemPosition = -1
        notifyDataSetChanged()
    }

    fun selectBottomItem(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int {
        return selectedItemPosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            listener,
            ItemCollageBgBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
            context
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val apiAds = getItem(position)
        (holder as ViewHolder).bind(apiAds, selectedItemPosition)
    }

    class ViewHolder(
        listener: BGsCallback,
        private val binding: ItemCollageBgBinding,
        private val context: Context,

        ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.modelDrawableAssets?.let { item ->
                    listener.onBackgroundClick(adapterPosition, item)
                }
            }
        }

        fun bind(item: ModelDrawableAssets, selectedItemPosition: Int) {
            highlightItemAt(
                binding = binding, position = position, selectedItemPosition = selectedItemPosition
            )
            binding.apply {
                modelDrawableAssets = item
            }
        }


        private fun highlightItemAt(
            binding: ItemCollageBgBinding, position: Int, selectedItemPosition: Int
        ) {
            val paddingInDp: Int = if (selectedItemPosition == position) {
                3
            } else {
                0
            }
            val density = binding.ivCollageBg.context.resources.displayMetrics.density
            val paddingInPx = (paddingInDp * density).toInt()
            binding.ivCollageBg.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
        }
    }
}