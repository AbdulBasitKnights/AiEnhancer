package com.aiface.aging.features.imgpicker.adapter

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.bumptech.glide.Glide
import com.aiface.aging.databinding.ItemGalleryCameraBinding
import com.aiface.aging.databinding.ItemGalleryMediaBinding
import com.aiface.aging.databinding.ItemNativeAdGridLayoutBinding
import com.aiface.aging.features.imgpicker.builder.TedImagePickerBaseBuilder
import com.aiface.aging.features.imgpicker.model.Media
import com.aiface.aging.features.imgpicker.util.ToastUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class MediaAdapterNew(
    private val activity: FragmentActivity,
    private val builder: TedImagePickerBaseBuilder<*>?
) : RecyclerView.Adapter<MediaAdapterNew.BaseViewHolder<ViewDataBinding, Media>>() {

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
        return when {
           // position == 0 -> ViewType.HEADER.ordinal
            position % 9 == 0 && position < itemCount - 1 -> ViewType.AD.ordinal // Ensuring last item isn't an ad
            else -> ViewType.ITEM.ordinal
        }
    }

    override fun getItemCount(): Int {
        val adCount = (items.size / 9)
        var totalItems = items.size + 1 + adCount

        // Ensure the last item isn't an ad
        if ((items.size + adCount) % 9 == 0) {
            totalItems -= 1
        }

        return totalItems
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<ViewDataBinding, Media> {
        return when (ViewType.values()[viewType]) {
            ViewType.HEADER -> CameraViewHolder(parent, R.layout.item_gallery_camera)
            ViewType.ITEM -> ImageViewHolder(parent, R.layout.item_gallery_media)
            ViewType.AD -> AdViewHolder(parent, R.layout.item_native_ad_grid_layout)
        }.apply {
            if (this !is AdViewHolder) { // Prevent click listener for AdViewHolder
                onItemClickListener?.let { listener ->
                    itemView.setOnClickListener {
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            if (adapterPosition == 0) {
                                listener.onHeaderClick()
                            } else {
                                listener.onItemClick(
                                    getActualItem(adapterPosition),
                                    adapterPosition - 1,
                                    adapterPosition
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ViewDataBinding, Media>, position: Int) {
        when (getItemViewType(position)) {
            ViewType.HEADER.ordinal -> {}
            ViewType.AD.ordinal -> (holder as AdViewHolder).bind(getActualItem(position)) // Bind ad content here
            ViewType.ITEM.ordinal -> holder.bind(getActualItem(position))
        }
    }

    private fun getActualItem(position: Int): Media {
        // Adjust position to account for ads every 10 items
        val adjustedPosition = position - (position / 9)
        return items[adjustedPosition - 1]
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
        return actualPosition + 1 + (actualPosition / 9)
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
                if (isSelected) {
                    selectedNumber = selectedUriList.indexOf(data.uri) + 1
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

    inner class AdViewHolder(parent: ViewGroup, layoutRes: Int) :
        BaseViewHolder<ItemNativeAdGridLayoutBinding, Media>(parent, layoutRes) {


        var isAdLoaded: Boolean = false
        override fun bind(data: Media) {
            val shimmer = itemView.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(
                com.aiface.aging.R.id.shimmer_container_native,
            )
            shimmer?.visibility = android.view.View.VISIBLE
            try {
                shimmer?.startShimmer()
            } catch (_: Exception) {
            }
            itemView.findViewById<android.view.View>(com.aiface.aging.R.id.nativeAdView)
                ?.visibility = android.view.View.GONE
        }

    }

    enum class ViewType {
        HEADER, ITEM, AD
    }

}

