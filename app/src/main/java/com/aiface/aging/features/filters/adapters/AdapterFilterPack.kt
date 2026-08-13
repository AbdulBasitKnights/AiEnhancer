package com.snaptune.ai.photoeditor.collagemaker.presentation.fragments.filters.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.aiface.aging.features.filters.model.ModelFilterPack
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemFilterPackBinding

interface FilterPacksCallback {
    fun onFilterClick(position: Int, modelFilterPack: ModelFilterPack)
}

class AdapterFilterPack constructor(
    private val listener: FilterPacksCallback,
    private val context: Context
) : ListAdapter<ModelFilterPack, RecyclerView.ViewHolder>(FilterPackDiffCallback()) {


    private var selectedItemPosition: Int = -1

    fun unselectMode() {
        selectedItemPosition = -1
        notifyDataSetChanged()
    }

    fun selectMode(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    fun refreshItem(position: Int) {
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            ItemFilterPackBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            listener,
            context
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val frameHeader = getItem(position)
        (holder as MyViewHolder).bind(frameHeader, selectedItemPosition)
    }

    class MyViewHolder(
        private val binding: ItemFilterPackBinding,
        listener: FilterPacksCallback,
        val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {
        init {

            binding.setItemClickListener {
                binding.modelFilterPack?.let { modelFilterPack ->
                    listener.onFilterClick(adapterPosition, modelFilterPack)
                }
            }
        }

        fun bind(item: ModelFilterPack, selectedItemPosition: Int) {
            highlightItemAt(
                binding = binding, position = adapterPosition, selectedItemPosition = selectedItemPosition
            )
            binding.apply {
                modelFilterPack = item
            }
        }


        private fun highlightItemAt(
            binding: ItemFilterPackBinding, position: Int, selectedItemPosition: Int
        ) {
            val paddingInDp: Int = if (selectedItemPosition == position) {
                2
            } else {
                0
            }
            val density = binding.frameHeadImg.context.resources.displayMetrics.density
            val paddingInPx = (paddingInDp * density).toInt()
            binding.frameHeadImg.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)

            binding.frameHeadImg.foreground =
                if (selectedItemPosition == position) ContextCompat.getDrawable(
                    context, R.drawable.bg_selected_frame
                ) else {
                    ContextCompat.getDrawable(
                        context, R.drawable.transparent_icon
                    )
                }
        }
    }
}

class FilterPackDiffCallback : DiffUtil.ItemCallback<ModelFilterPack>() {
    override fun areItemsTheSame(oldItem: ModelFilterPack, newItem: ModelFilterPack): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ModelFilterPack, newItem: ModelFilterPack): Boolean {
        return oldItem == newItem
    }
}