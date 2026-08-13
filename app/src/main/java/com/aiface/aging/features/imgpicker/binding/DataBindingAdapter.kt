package com.aiface.aging.features.imgpicker.binding

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.aiface.aging.features.imgpicker.base.BaseRecyclerViewAdapter
import com.aiface.aging.R
internal class DataBindingAdapter {

    companion object {
        @BindingAdapter("imageUri")
        @JvmStatic
        fun loadImage(imageView: ImageView, uri: Uri?) {
            if (uri != null){
                Glide.with(imageView.context)
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Skip disk cache
                    .skipMemoryCache(true)
                    .override(250)
                    .into(imageView)
            }else{
                imageView.setImageResource(R.drawable.ic_background_) // Use a placeholder image

            }

        }

        @Suppress("UNCHECKED_CAST")
        @BindingAdapter("replaceAll")
        @JvmStatic
        fun <D> replaceAll(recyclerView: RecyclerView, items: List<D>?) {
            items?.let { (recyclerView.adapter as? BaseRecyclerViewAdapter<D, *>)?.replaceAll(it) }
        }

        @Suppress("UNCHECKED_CAST")
        @BindingAdapter("replaceAll", "diffCallback")
        @JvmStatic
        fun <D> replaceAll(recyclerView: RecyclerView, items: List<D>?, diffCallback: Boolean) {
            items?.let {
                (recyclerView.adapter as? BaseRecyclerViewAdapter<D, *>)?.replaceAll(
                    it,
                    diffCallback
                )
            }
        }


        @Suppress("UNCHECKED_CAST")
        @BindingAdapter("animateTranslationY")
        @JvmStatic
        fun animateTranslationY(view: View, show: Boolean) {
            val animateValue = when (show) {
                true -> -view.y / 2
                false -> 0f
            }
            view.animate().translationY(animateValue).start()
        }

        @BindingAdapter("src")
        @JvmStatic
        fun setImageViewResource(imageView: ImageView, resId: Int?) {
            try {
                resId?.let { imageView.setImageResource(it) }
            } catch (e: Exception) {

            }

        }

        @BindingAdapter("background")
        @JvmStatic
        fun setBackgroundResource(view: View, resId: Int?) {
            try {
                resId?.let { view.setBackgroundResource(it) }
            } catch (e: Exception) {

            }
        }

    }

}
