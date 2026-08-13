package com.aiface.aging.shared.editorui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemBottomRecyclerBinding


interface BottomFeaturesCallback {
    fun onBottomItemClick(position: Int, modelDrawableAssets: ModelDrawableAssets)
}

open class AdapterBottomRecycler constructor(
    private val bottomFeaturesCallback: BottomFeaturesCallback,
    private val context: Context,
    private val isEditor: Boolean = false,
    showByDefault: Boolean = true
) :
    ListAdapter<ModelDrawableAssets, RecyclerView.ViewHolder>(DrawableAssetsDiffCallback()) {

    //private var selectedItemPosition: Int = 0
    private var selectedItemPosition: Int = if (showByDefault) 0 else -1

    fun unselectBottomItem() {
        val previousSelectedPosition = selectedItemPosition
        selectedItemPosition = -1
        if (previousSelectedPosition != -1) {
            notifyItemChanged(previousSelectedPosition)
        }
    }

    fun selectBottomItem(position: Int) {
        val previousSelectedPosition = selectedItemPosition
        selectedItemPosition = position
        if (previousSelectedPosition != -1) {
            notifyItemChanged(previousSelectedPosition)
        }
        notifyItemChanged(position)
    }


    fun getSelectedPosition(): Int {
        return selectedItemPosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            bottomFeaturesCallback,
            ItemBottomRecyclerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
            context
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val apiAds = getItem(position)
        (holder as ViewHolder).bind(apiAds, selectedItemPosition, isEditor)
    }

    class ViewHolder(
        bottomFeaturesCallback: BottomFeaturesCallback,
        private val binding: ItemBottomRecyclerBinding,
        private val context: Context,

        ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.modelDrawableAssets?.let { item ->
                    bottomFeaturesCallback.onBottomItemClick(adapterPosition, item)
                }
            }
        }

        fun bind(item: ModelDrawableAssets, selectedItemPosition: Int, isEditor: Boolean) {
            //  if (position!=0)binding.ll.setCustomMargins(60,0,0,0)
            highlightItemAt(
                binding = binding,
                position = position,
                selectedItemPosition = selectedItemPosition,
                isEditor
            )
            binding.apply {
                modelDrawableAssets = item
            }
        }


        private fun highlightItemAt(
            binding: ItemBottomRecyclerBinding,
            position: Int,
            selectedItemPosition: Int,
            isEditor: Boolean
        ) {
            if (isEditor) {
                binding.mediaItemText.setTextColor(
                    if (selectedItemPosition == position) {
                        ContextCompat.getColor(
                            context, R.color.colorHighlightBlueDark
                        )
                    } else {
                        ContextCompat.getColor(
                            context, R.color.white
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
                            context, R.color.white
                        )
                    }
                )

            } else {
                binding.mediaItemText.setTextColor(
                    if (selectedItemPosition == position) {

                        ContextCompat.getColor(context, R.color.colorHighlightBlueDark)

                    } else {
                        ContextCompat.getColor(
                            context, R.color.white
                        )
                    }
                )
                binding.mediaItemIcon.setColorFilter(
                    if (selectedItemPosition == position) {
                        ContextCompat.getColor(context, R.color.colorHighlightBlueDark)
                    } else {
                        ContextCompat.getColor(
                            context, R.color.white
                        )
                    }
                )
            }

        }
    }
}
