package com.aiface.aging.features.imgpicker.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemGalleryCameraBinding
import com.aiface.aging.databinding.ItemGalleryMediaBinding
import com.aiface.aging.features.imgpicker.builder.TedImagePickerBaseBuilder
import com.aiface.aging.features.imgpicker.model.Media
import com.aiface.aging.features.imgpicker.util.ToastUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class MediaAdapterNoAd(
    private val activity: FragmentActivity,
    private val builder: TedImagePickerBaseBuilder<*>?
) : RecyclerView.Adapter<MediaAdapterNoAd.BaseViewHolder<ViewDataBinding, Media>>() {

    private val items = mutableListOf<Media>()
    val selectedUriList: MutableList<Uri> = mutableListOf()
    private val executorService: ExecutorService = Executors.newFixedThreadPool(4)

    var onMediaAddListener: (() -> Unit)? = null
    var onItemClickListener: OnItemClickListener<Media>? = null

    interface OnItemClickListener<D> {
        fun onItemClick(data: D, itemPosition: Int, layoutPosition: Int)
        fun onHeaderClick() {
            // no-op
        }
    }

    override fun getItemViewType(position: Int): Int {
       // return if (position == 0) ViewType.HEADER.ordinal else ViewType.ITEM.ordinal
        return   ViewType.ITEM.ordinal
    }

    override fun getItemCount(): Int {
     //   return items.size + 1 // +1 for camera header
        return items.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<ViewDataBinding, Media> {
        return when (ViewType.values()[viewType]) {
            ViewType.HEADER -> CameraViewHolder(parent, R.layout.item_gallery_camera)
            ViewType.ITEM -> ImageViewHolder(parent, R.layout.item_gallery_media)
        }.apply {
            onItemClickListener?.let { listener ->
                itemView.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
//                        if (adapterPosition == 0) {
//                            listener.onHeaderClick()
//                        } else {
                            listener.onItemClick(
                                getActualItem(adapterPosition),
                                adapterPosition,//adapterPosition - 1,
                                adapterPosition
                            )
                      //  }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ViewDataBinding, Media>, position: Int) {
        if (getItemViewType(position) == ViewType.ITEM.ordinal) {
            holder.bind(getActualItem(position))
        }
    }

    private fun getActualItem(position: Int): Media {
    //    return items[position - 1] // because position 0 is header
        return items[position] // because position 0 is header
    }

    fun replaceAll(newItems: List<Media>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun toggleMediaSelect(uri: Uri) {
        if (selectedUriList.contains(uri)) {
            removeMedia(uri)
        } else {
            if (builder?.maxCount == 1) {
                removeMedia(uri)
                selectedUriList.clear()
                selectedUriList.add(uri)
                onMediaAddListener?.invoke()
                refreshSelectedView()
                notifyItemChanged(0)
            } else {
                addMedia(uri)
            }
        }
    }

    private fun addMedia(uri: Uri) {
        if (selectedUriList.size == builder?.maxCount) {
            ToastUtil.showToast(
                activity,
                builder.maxCountMessage ?: activity.getString(builder.maxCountMessageResId)
            )
        } else {
            selectedUriList.add(uri)
            onMediaAddListener?.invoke()
            refreshSelectedView()
        }
    }

    private fun removeMedia(uri: Uri) {
        val position = getViewPosition(uri)
        selectedUriList.remove(uri)
        notifyItemChanged(position)
        refreshSelectedView()
    }

    private fun getViewPosition(uri: Uri): Int {
        val actualPosition = items.indexOfFirst { it.uri == uri }
      //  return actualPosition + 1 // +1 for header
        return actualPosition
    }

    private fun refreshSelectedView() {
        selectedUriList.forEach {
            notifyItemChanged(getViewPosition(it))
        }
    }

    abstract inner class BaseViewHolder<out B : ViewDataBinding, D>(
        parent: ViewGroup, @LayoutRes layoutRes: Int
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
    ) {
        protected val binding: B = DataBindingUtil.bind(itemView)!!
        open fun bind(data: D) {}
        open fun recycled() {}
    }

    inner class ImageViewHolder(parent: ViewGroup, layoutRes: Int) :
        BaseViewHolder<ItemGalleryMediaBinding, Media>(parent, layoutRes) {

        init {
            binding.run {
                selectType = builder?.selectType

                showZoom = false
            }
        }

        override fun bind(data: Media) {
            binding.run {
                media = data
                isSelected = selectedUriList.contains(data.uri)
                if (isSelected){
                    binding.selctor.visibility = View.VISIBLE
                }else{
                    binding.selctor.visibility = View.GONE
                }
                if (isSelected) {
                  //  selectedNumber = selectedUriList.indexOf(data.uri) + 1
                    selectedNumber = selectedUriList.indexOf(data.uri)
                }
                showZoom = builder?.showZoomIndicator == true && data is Media.Image
                showDuration = builder?.showVideoDuration == true && data is Media.Video
                if (data is Media.Video) {
                    duration = data.durationText
                }
            }
        }

        override fun recycled() {
            if (!activity.isDestroyed) {
                Glide.with(activity).clear(binding.ivImage)
            }
        }


    }

    inner class CameraViewHolder(parent: ViewGroup, layoutRes: Int) :
        BaseViewHolder<ItemGalleryCameraBinding, Media>(parent, layoutRes) {
        init {
            builder?.let { binding.ivImage.setImageResource(it.cameraTileImageResId) }
        }
    }

    enum class ViewType {
        HEADER, ITEM
    }
}


