package com.aiface.aging.features.text.text

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemTextMainBinding
import com.aiface.aging.shared.editorui.DrawableAssetsDiffCallback
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.utils.AppUtils.setCustomMargins

open class AdapterTextMain constructor(
    private val textItemListener: TextItemListener,
    private val context: Context
) :
    ListAdapter<ModelDrawableAssets, RecyclerView.ViewHolder>(DrawableAssetsDiffCallback()) {

    private var selectedItemPosition: Int = 2

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
            textItemListener,
            ItemTextMainBinding.inflate(
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
        textItemListener: TextItemListener,
        private val binding: ItemTextMainBinding,
        private val context: Context,

        ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.modelDrawableAssets?.let { item ->
                    textItemListener.onTextItemClick(adapterPosition, item)
                }
            }
        }

        fun bind(item: ModelDrawableAssets, selectedItemPosition: Int) {
            if (position!=0) binding.ll.setCustomMargins(90,0,0,0)
            highlightItemAt(
                binding = binding, position = position, selectedItemPosition = selectedItemPosition
            )
            binding.apply {
                modelDrawableAssets = item
            }
        }


        private fun highlightItemAt(
            binding: ItemTextMainBinding, position: Int, selectedItemPosition: Int
        ) {

            binding.mediaItemText.setTextColor(
                if (selectedItemPosition == position) {
                    ContextCompat.getColor(
                        context, R.color.colorHighlightBlueDark
                    )
                } else {
                    ContextCompat.getColor(
                        context, R.color.text_secondary
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
                        context, R.color.text_secondary
                    )
                }
            )

        }
    }
}