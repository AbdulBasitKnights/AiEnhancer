package com.aiface.aging.shared.binding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.aiface.aging.R
import com.aiface.aging.shared.hide
import com.aiface.aging.shared.show


@BindingAdapter("bindVisibility")
fun bindVisibility(view: View, isGone: Boolean) {
    view.visibility = if (isGone) {
        View.VISIBLE
    } else {
        View.GONE
    }
}



@BindingAdapter("loadImageDrawable")
fun loadImageDrawable(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
       // val requestOptions = RequestOptions.placeholderOf(R.drawable.loa)
        Glide.with(view.context)
            .asDrawable()
            .load(imageUrl).override(800)
           // .apply(requestOptions)
            .into(view)
    }
}

@BindingAdapter("loadImageMyWork")
fun loadImageMyWork(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        //val requestOptions = RequestOptions.placeholderOf(R.drawable.ic_frames_loading)
        Glide.with(view.context)
            .asDrawable()
            .load(imageUrl).override(800)
          //  .apply(requestOptions)
            .into(view)
    }
}
@BindingAdapter("loadVideoThumbMyWork")
fun loadVideoThumbMyWork(view: ImageView, videoUrl: String?) {
    if (videoUrl.isNullOrEmpty()) return
    val requestOptions = RequestOptions()
      //  .placeholder(R.drawable.ic_frames_loading)
       // .error(R.drawable.ic_frames_loading)
    Glide.with(view.context)
        .asBitmap() // important
        .load(videoUrl)
        .frame(1_000_000) // 1 second (microseconds)
        .apply(requestOptions)
        .into(view)
}


@BindingAdapter("loadFrameBottomThumb")
fun loadFrameBottomThumb(view: ImageView, imageUrl: String?) {
    try {
        if (!imageUrl.isNullOrEmpty()) {
          //  val requestOptions = RequestOptions.placeholderOf(R.drawable.ic_frames_loading)
            Glide.with(view.context)
                .asDrawable()
                .load(imageUrl).override(800)
                .thumbnail(0.1f)
              //  .apply(requestOptions)
                .into(view)
        }
    } catch (e: Exception) {

    }
}
@BindingAdapter("loadFrameBottomThumbNew")
fun loadFrameBottomThumbNew(view: ImageView, imageUrl: String?) {
    try {

            if (!imageUrl.isNullOrEmpty()) {
                if(imageUrl.endsWith("gif",false)){
                  //  val requestOptions = RequestOptions.placeholderOf(R.drawable.ic_frames_loading)
                    Glide.with(view.context)
                        .asGif()
                        .load(imageUrl)  // URL for the image/video
                        .override(800)
                        .thumbnail(0.1f)  // Load a small thumbnail for faster preview
                     //   .apply(requestOptions)
                        .into(view)  // ImageView to load into
                }
                else{
                //    val requestOptions = RequestOptions.placeholderOf(R.drawable.ic_frames_loading)
                    Glide.with(view.context)
                        .asDrawable()
                        .load(imageUrl).override(800)
                        .thumbnail(0.1f)
                     //  .apply(requestOptions)
                        .into(view)
                }

            }

    } catch (e: Exception) {

    }
}

//@BindingAdapter("loadFrameBottomThumb")
//fun loadFrameBottomThumbNew(view: ImageView, imageUrl: String?) {
//    try {
//        if (!imageUrl.isNullOrEmpty()) {
//            val cropOptions: RequestOptions= RequestOptions().centerCrop().placeholder(R.drawable.ic_frames_loading)
//            Glide.with(view.context)
//                .load(imageUrl)
//                .apply(cropOptions as BaseRequestOptions<*>)
//                .override(800)
//                .transition(DrawableTransitionOptions.withCrossFade(500))
//                .into(view)
//        }
//    } catch (e: Exception) {
//
//    }
//}

@BindingAdapter("loadGifFromPath")
fun loadGifFromPath(view: ImageView, imageUrl: String?) {
    try {
        if (!imageUrl.isNullOrEmpty()) {
          //  val requestOptions = RequestOptions.placeholderOf(R.drawable.ic_video_loading)
            Glide.with(view.context)
                .asGif()
                .load(imageUrl).override(800)
                .thumbnail(0.1f)
              //  .apply(requestOptions)
                .into(view)
            view.show()
        } else {
            view.hide()
        }
    } catch (e: Exception) {

    }
}

@BindingAdapter("loadDrawableAsResource")
fun loadDrawableAsResource(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
      //  val requestOptions = RequestOptions.placeholderOf(R.drawable.ic_frames_loading)
        Glide.with(view.context)
            .asDrawable()
            .load(imageUrl).override(800)
           // .apply(requestOptions)
            .into(view)
    }
}

@BindingAdapter("loadFrame")
fun loadFrame(view: ImageView, imageUrl: String?) {
    val url = imageUrl?.takeIf { it.isNotBlank() } ?: return
    Glide.with(view.context)
        .asDrawable()
        .load(url).override(800)
        .thumbnail(0.1f)
        .placeholder(R.drawable.placeholder_icon)
        .error(R.drawable.placeholder_icon)
        .into(view)
}

@BindingAdapter("loadImageUri")
fun loadImageUri(view: ImageView, imageUri: Uri?) {
    if (imageUri != null) {
        Glide.with(view.context)
            .asDrawable()
            .load(imageUri)
            .into(view)
    }
}

@BindingAdapter("checkIfAssetDownloaded")
fun checkIfAssetDownloaded(view: ImageView, url: String) {
    Glide.with(view.context)
        .load(url)
        .apply(RequestOptions().onlyRetrieveFromCache(true))
        .addListener(object : RequestListener<Drawable?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable?>,
                isFirstResource: Boolean
            ): Boolean {
                view.visibility = View.VISIBLE
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable?>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                view.visibility = View.GONE
                return false
            }

        }).preload()
}

@BindingAdapter("loadReducedDrawable")
fun loadReducedDrawable(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
   //     val requestOptions = RequestOptions.placeholderOf(R.drawable.ic_frames_loading)
        Glide.with(view.context)
            .asDrawable()
            .override(300)
         //   .apply(requestOptions)
            .load(imageUrl)
            .into(view)
    }
}

@BindingAdapter("loadIntAsDrawable")
fun loadIntAsDrawable(view: ImageView, drawable: Int) {
    try {
        Glide.with(view.context)
            .asDrawable()
            .load(drawable).override(800)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    view.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
    } catch (e: Exception) {

    }
}

@BindingAdapter("loadDrawable")
fun loadDrawable(view: ImageView, drawable: Int) {
    try {
        Glide.with(view.context)
            .asDrawable()
            .load(drawable).override(800)
            .into(view)
    } catch (e: Exception) {

    }
}

@BindingAdapter("loadBitmapToImage")
fun loadBitmapToImage(view: ImageView, bitmap: Bitmap?) {
    if (bitmap != null) {
      //  val requestOptions = RequestOptions.placeholderOf(R.drawable.ic_frames_loading)
        Glide.with(view.context)
            .asBitmap()
            .centerCrop()
            .override(800)
           // .apply(requestOptions)
            .load(bitmap)
            .into(view)
    }
}

fun isGlideInitialized(context: Context): Boolean {
    // Check if Glide is initialized
    return try {
        Glide.with(context)
        true
    } catch (e: Exception) {
        false
    }
}
