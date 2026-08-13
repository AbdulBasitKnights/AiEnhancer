package com.aiface.aging.utils

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide

/**
 * Central memory helpers — trim image caches and release view drawables on recycle.
 */
object BitmapMemoryUtils {

    private const val TAG = "BitmapMemoryUtils"

    /** Stop GIF/animatable drawables and detach from [ImageView]. */
    fun clearImageView(imageView: ImageView?) {
        if (imageView == null) return
        try {
            (imageView.drawable as? Animatable)?.stop()
            imageView.setImageDrawable(null)
        } catch (t: Throwable) {
            Log.w(TAG, "clearImageView failed", t)
        }
    }

    /** Trim in-memory image caches (Coil template loader + Glide). Call on low memory / screen destroy. */
    fun trimImageCaches(context: Context) {
        val appContext = context.applicationContext
        try {
            com.aiface.aging.shared.TemplateThumbLoader.trimMemory(appContext)
        } catch (t: Throwable) {
            Log.w(TAG, "TemplateThumbLoader trim failed", t)
        }
        try {
            Glide.get(appContext).clearMemory()
        } catch (t: Throwable) {
            Log.w(TAG, "Glide clearMemory failed", t)
        }
    }

    /** Disk + memory trim — use from [Application.onTrimMemory] on background thread for disk. */
    fun trimImageCachesAsync(context: Context, includeDisk: Boolean = false) {
        trimImageCaches(context)
        if (!includeDisk) return
        val appContext = context.applicationContext
        Thread {
            try {
                Glide.get(appContext).clearDiskCache()
            } catch (t: Throwable) {
                Log.w(TAG, "Glide clearDiskCache failed", t)
            }
        }.start()
    }
}
