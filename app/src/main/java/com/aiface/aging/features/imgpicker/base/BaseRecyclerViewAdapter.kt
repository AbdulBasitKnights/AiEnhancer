package com.aiface.aging.features.imgpicker.base

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

internal abstract class BaseRecyclerViewAdapter<D, VH : BaseViewHolder<ViewDataBinding, D>>(private var headerCount: Int = 0) :
    RecyclerView.Adapter<VH>() {

    protected val items = mutableListOf<D>()
    var onItemClickListener: OnItemClickListener<D>? = null

    interface OnItemClickListener<D> {
        fun onItemClick(data: D, itemPosition: Int, layoutPosition: Int)
        fun onHeaderClick() {
            // no-op
        }
    }

    open fun replaceAll(items: List<D>, useDiffCallback: Boolean = false) {
        this.items.run {
            clear()
            addAll(items)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = getViewType(position).ordinal

    private fun getViewType(position: Int): ViewType {
        return when {
            position < headerCount -> ViewType.HEADER
            position % 5 == 0 -> ViewType.AD // Example condition for AD
            else -> ViewType.ITEM
        }
    }

    abstract fun getViewHolder(parent: ViewGroup, viewType: ViewType): VH

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return getViewHolder(parent, ViewType.getViewType(viewType)).apply {
            onItemClickListener?.let { listener ->
                itemView.setOnClickListener {
                    if (adapterPosition >= headerCount) {
                        getItem(adapterPosition)?.let { it1 ->
                            listener.onItemClick(
                                it1,
                                getItemPosition(adapterPosition),
                                adapterPosition
                            )
                        }
                    } else if (adapterPosition < headerCount) {
                        listener.onHeaderClick()
                    }
                }
            }
        }
    }

    private fun getItemPosition(adapterPosition: Int) = adapterPosition - headerCount

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (getViewType(position)) {
            ViewType.HEADER -> {
                // Header binding logic if needed
            }
            ViewType.ITEM -> getItem(position)?.let { holder.bind(it) }
            ViewType.AD -> getItem(position)?.let { holder.bind(it) } // Add AD binding logic here
        }
    }

    override fun onViewRecycled(holder: VH) {
        holder.recycled()
        super.onViewRecycled(holder)
    }

    fun getItemPosition(data: D) = items.indexOf(data).let { if (it < 0) it else it + headerCount }

    override fun getItemCount(): Int = items.size + headerCount

  //  open fun getItem(position: Int): D = items[getItemPosition(position)]

    open fun getItem(position: Int): D? {
        val index = getItemPosition(position)
        return if (index >= 0 && index < items.size) {
            items[index]
        } else {
            null
        }
    }

    enum class ViewType {
        HEADER, ITEM, AD;

        companion object {
            fun getViewType(value: Int) = values()[value]
        }
    }
}

