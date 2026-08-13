package com.aiface.aging.features.mywork


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemMyworkBinding


class AdapterMyWork(
    private var list: List<MediaStoreImage>,
    private var listener: MyWorkClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyWorkViewHolder(
            ItemMyworkBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val frameHeader = list[position]
        (holder as MyWorkViewHolder).bind(frameHeader)
    }

    inner class MyWorkViewHolder(private val binding: ItemMyworkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setItemClickListener {
                binding.mediaStoreImage?.let { mediaStoreImage ->
                    listener.onImageClick(mediaStoreImage)
                }
            }
        }
        fun bind(item: MediaStoreImage) {
            binding.apply {
                mediaStoreImage = item
            }
        }
    }

    fun updateList(newList: List<MediaStoreImage>) {
        val diffResult = DiffUtil.calculateDiff(MediaStoreImageDiffCallback(list, newList))
        list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    fun currentList() = list

    inner class MediaStoreImageDiffCallback(
        private val oldList: List<MediaStoreImage>,
        private val newList: List<MediaStoreImage>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

}

interface MyWorkClickListener {
    fun onImageClick(image: MediaStoreImage)
}


