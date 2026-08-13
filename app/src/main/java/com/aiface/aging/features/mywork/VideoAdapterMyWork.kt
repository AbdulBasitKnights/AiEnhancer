package com.aiface.aging.features.mywork


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemVideoMyworkBinding


class VideoAdapterMyWork(
    private var list: List<MediaStoreVideo>,
    private var listener: VideoAdapterMyWorkListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyWorkViewHolder(
            ItemVideoMyworkBinding.inflate(
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

    inner class MyWorkViewHolder(private val binding: ItemVideoMyworkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setItemClickListener {
                binding.mediaStoreVideo?.let { mediaStoreImage ->
                    listener.onVideoClick(mediaStoreImage)
                }
            }
        }
        fun bind(item: MediaStoreVideo) {
            binding.apply {
                mediaStoreVideo = item
            }
        }
    }

    fun updateList(newList: List<MediaStoreVideo>) {
        val diffResult = DiffUtil.calculateDiff(MediaStoreImageDiffCallback(list, newList))
        list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    fun currentList() = list

    inner class MediaStoreImageDiffCallback(
        private val oldList: List<MediaStoreVideo>,
        private val newList: List<MediaStoreVideo>
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

interface VideoAdapterMyWorkListener {
    fun onVideoClick(image: MediaStoreVideo)
}


