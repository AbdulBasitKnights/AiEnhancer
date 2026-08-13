package com.aiface.aging.shared.editorui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemRatioBinding
import com.aiface.aging.R


open class AdapterRatio constructor(
    private val ratioListener: RatioListener,
    private val context: Context
) :
    ListAdapter<ModelRatio, RecyclerView.ViewHolder>(RatioDiffCallback()) {

    private var selectedItemPosition: Int = 0

    fun unselectBottomItem() {
        selectedItemPosition = 0
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
            ratioListener,
            ItemRatioBinding.inflate(
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
        ratioListener: RatioListener,
        private val binding: ItemRatioBinding,
        private val context: Context,

        ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.modelRatio?.let { item ->
                    ratioListener.onRatioClick(adapterPosition, item)
                }
            }
        }

        fun bind(item: ModelRatio, selectedItemPosition: Int) {
            highlightItemAt(
                binding = binding, position = position, selectedItemPosition = selectedItemPosition
            )
            binding.apply {
                modelRatio = item
            }
            //   if (position!=0)binding.ll.setCustomMargins(60,0,0,0)
        }


        private fun highlightItemAt(
            binding: ItemRatioBinding,
            position: Int,
            selectedItemPosition: Int
        ) {
            val highlightColor = ContextCompat.getColor(context, R.color.colorHighlightBlueDark)
            val defaultTextColor = ContextCompat.getColor(context, R.color.bgText)
            val transparentColor =
                ContextCompat.getColor(context, R.color.transparent)
            val grey5Color = ContextCompat.getColor(context, R.color.grey5)

            binding.mediaItemText.setTextColor(if (selectedItemPosition == position) highlightColor else defaultTextColor)
            binding.mediaItemContainer.setColorFilter(if (selectedItemPosition == position) highlightColor else transparentColor)

            val iconColor = if (position == 0) {
                if (selectedItemPosition == 0) highlightColor else transparentColor
            } else {
                if (selectedItemPosition == position) grey5Color else transparentColor
            }
            binding.mediaItemIcon.setColorFilter(iconColor)
        }
    }
}
