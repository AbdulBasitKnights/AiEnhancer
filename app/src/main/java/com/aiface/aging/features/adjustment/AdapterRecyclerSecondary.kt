package com.aiface.aging.features.adjustment

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemRecyclerSecondaryBinding
import com.aiface.aging.shared.editorui.DrawableAssetsDiffCallback
import com.aiface.aging.shared.editorui.ModelDrawableAssets

interface SecondaryRecyclerListener {
    fun onSecondaryRecyclerClick(position: Int, modelDrawableAssets: ModelDrawableAssets)
}

open class AdapterRecyclerSecondary constructor(
    private val listener: SecondaryRecyclerListener,
    private val context: Context
) :
    ListAdapter<ModelDrawableAssets, RecyclerView.ViewHolder>(DrawableAssetsDiffCallback()) {

    private var selectedItemPosition: Int = 0

    fun unselectBottomItem() {
        selectedItemPosition = 0
    }

    fun selectBottomItem(position: Int) {
        selectedItemPosition = position
       // notifyItemChanged(position)
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int {
        return selectedItemPosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            listener,
            ItemRecyclerSecondaryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
            context
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val modelRatio = getItem(position)
        (holder as ViewHolder).bind(modelRatio, selectedItemPosition)
    }

    class ViewHolder(
        listener: SecondaryRecyclerListener,
        private val binding: ItemRecyclerSecondaryBinding,
        private val context: Context,

        ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.modelDrawableAssets?.let { item ->
                    listener.onSecondaryRecyclerClick(adapterPosition, item)
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
            binding: ItemRecyclerSecondaryBinding, position: Int, selectedItemPosition: Int
        ) {

            binding.mediaItemText.setTextColor(
                if (selectedItemPosition == position) {
                    ContextCompat.getColor(
                        context, R.color.colorHighlightBlueDark
                    )
                } else {
                    ContextCompat.getColor(
                        context, R.color.bgText
                    )
                }
            )
            binding.mediaItemIcon.setColorFilter(
                if (selectedItemPosition == position) {
                    ContextCompat.getColor(
                        context, R.color.colorHighlightBlueDark
                    )
                } else {
                    ContextCompat.getColor(
                        context, R.color.bgText
                    )
                }
            )
        }
    }
}