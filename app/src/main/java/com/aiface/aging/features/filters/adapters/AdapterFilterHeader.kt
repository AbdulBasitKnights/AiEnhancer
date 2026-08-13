package com.aiface.aging.features.filters.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemFilterHeaderBinding
import com.aiface.aging.features.filters.model.ModelFilters


interface FiltersCallback {
    fun onFilterCtgClick(position: Int, modelFilters: ModelFilters)
}

class AdapterFilterHeader constructor(
    private val context: Context, val listener: FiltersCallback,
) : ListAdapter<ModelFilters, RecyclerView.ViewHolder>(FiltersDiffCallback()) {

    private var selectedItemPosition: Int = 0

    fun unselectMode() {
        selectedItemPosition = 0
        notifyDataSetChanged()
    }

    fun selectMode(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int {
        return selectedItemPosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            ItemFilterHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ), listener, context
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val frameHeader = getItem(position)
        (holder as MyViewHolder).bind(frameHeader, position, selectedItemPosition)
    }

    class MyViewHolder(
        private val binding: ItemFilterHeaderBinding,
        listener: FiltersCallback,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.modelFilters?.let { modelFilterHeader ->
                    listener.onFilterCtgClick(adapterPosition, modelFilterHeader)
                }
            }
        }

        fun bind(item: ModelFilters, position: Int, selectedItemPosition: Int) {
            highlightItemAt(
                binding = binding, position = position, selectedItemPosition = selectedItemPosition
            )
            binding.apply {
                modelFilters = item
            }
        }

        private fun highlightItemAt(
            binding: ItemFilterHeaderBinding, position: Int, selectedItemPosition: Int
        ) {

            binding.frameHeadTxt.setTextColor(
                if (selectedItemPosition == position) ContextCompat.getColor(
                    context, R.color.colorHighlightBlueDark
                ) else {
                    ContextCompat.getColor(
                        context, R.color.bgText
                    )
                }
            )
        }
    }
}

private class FiltersDiffCallback : DiffUtil.ItemCallback<ModelFilters>() {

    override fun areItemsTheSame(
        oldItem: ModelFilters,
        newItem: ModelFilters
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ModelFilters,
        newItem: ModelFilters
    ): Boolean {
        return oldItem == newItem
    }
}