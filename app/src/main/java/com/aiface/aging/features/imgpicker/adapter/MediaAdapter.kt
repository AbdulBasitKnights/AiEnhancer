package com.aiface.aging.features.imgpicker.adapter

import android.app.Activity
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.facebook.shimmer.ShimmerFrameLayout
import com.aiface.aging.databinding.ItemGalleryCameraBinding
import com.aiface.aging.databinding.ItemGalleryMediaBinding
import com.aiface.aging.databinding.ItemNativeAdGridLayoutBinding
import com.aiface.aging.features.imgpicker.base.BaseSimpleHeaderAdapter
import com.aiface.aging.features.imgpicker.base.BaseViewHolder
import com.aiface.aging.features.imgpicker.builder.TedImagePickerBaseBuilder
import com.aiface.aging.features.imgpicker.model.Media
import com.aiface.aging.features.imgpicker.util.ToastUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class MediaAdapter(
    private val activity: Activity,
    private val builder: TedImagePickerBaseBuilder<*>?,
) : BaseSimpleHeaderAdapter<Media>(if (builder?.showCameraTile == true) 1 else 0) {

    internal val selectedUriList: MutableList<Uri> = mutableListOf()
    var onMediaAddListener: (() -> Unit)? = null

    private val executorService: ExecutorService = Executors.newFixedThreadPool(4)

    override fun getHeaderViewHolder(parent: ViewGroup) = CameraViewHolder(parent)
    override fun getItemViewHolder(parent: ViewGroup) = ImageViewHolder(parent)
    override fun getAdViewHolder(parent: ViewGroup) = AdViewHolder(parent)

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
            val message =
                builder.maxCountMessage ?: activity.getString(builder.maxCountMessageResId)
            ToastUtil.showToast(activity, message)
        } else {
            selectedUriList.add(uri)
            onMediaAddListener?.invoke()
            refreshSelectedView()
        }
    }

    private fun getViewPosition(it: Uri): Int =
        items.indexOfFirst { media -> media.uri == it } + headerCount

    private fun removeMedia(uri: Uri) {
        val position = getViewPosition(uri)
        selectedUriList.remove(uri)
        notifyItemChanged(position)
        refreshSelectedView()
    }

    private fun refreshSelectedView() {
        selectedUriList.forEach {
            val position: Int = getViewPosition(it)
            notifyItemChanged(position)
        }
    }

    inner class ImageViewHolder(parent: ViewGroup) :
        BaseViewHolder<ItemGalleryMediaBinding, Media>(parent, R.layout.item_gallery_media) {

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

                showZoom = builder?.showZoomIndicator == true && media is Media.Image
                showDuration = builder?.showVideoDuration == true && media is Media.Video
                if (data is Media.Video) {
                    binding.duration = data.durationText
                }
            }
        }

        override fun recycled() {
            if (activity.isDestroyed) {
                return
            }
            Glide.with(activity).clear(binding.ivImage)
        }


    }

    inner class CameraViewHolder(parent: ViewGroup) : HeaderViewHolder<ItemGalleryCameraBinding>(
        parent, R.layout.item_gallery_camera
    ) {
        init {
            builder?.let { binding.ivImage.setImageResource(it.cameraTileImageResId) }
        }
    }

    inner class AdViewHolder(parent: ViewGroup) : BaseViewHolder<ItemNativeAdGridLayoutBinding, Media>(
        parent, R.layout.item_native_ad_grid_layout
    ) {
        override fun bind(data: Media) {
            val shimmer = itemView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
            shimmer?.visibility = View.VISIBLE
            try {
                shimmer?.startShimmer()
            } catch (_: Exception) {
            }
            itemView.findViewById<View>(R.id.nativeAdView)?.visibility = View.GONE
        }

        override fun recycled() {
            if (activity.isDestroyed) {
                return
            }
            val shimmer = itemView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
            try {
                shimmer?.stopShimmer()
            } catch (_: Exception) {
            }
        }
    }
}
