package com.aiface.aging.utils

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.aiface.aging.R

object GlobalLoader {

    private val mainHandler = Handler(Looper.getMainLooper())

    var isLoaderShowing = false

    @JvmStatic
    @JvmOverloads
    fun show(activity: Activity, message: String? = null) {
        runOnMain {
            if (activity.isFinishing || activity.isDestroyed) return@runOnMain
            isLoaderShowing = true
            val loader = activity.findViewById<View>(R.id.globalLoader)
            loader?.visibility = View.VISIBLE
            message?.let { findLoaderTextView(loader as? ViewGroup)?.text = it }
            val img = activity.findViewById<ImageView>(R.id.progressImage)
            img?.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.rotate))
        }
    }

    @JvmStatic
    fun hide(activity: Activity) {
        runOnMain {
            if (activity.isFinishing || activity.isDestroyed) return@runOnMain
            isLoaderShowing = false
            val loader = activity.findViewById<View>(R.id.globalLoader)
            loader?.visibility = View.GONE
            activity.findViewById<ImageView>(R.id.progressImage)?.clearAnimation()
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun findLoaderTextView(root: ViewGroup?): TextView? {
        if (root == null) return null
        for (index in 0 until root.childCount) {
            when (val child = root.getChildAt(index)) {
                is TextView -> return child
                is ViewGroup -> findLoaderTextView(child)?.let { return it }
            }
        }
        return null
    }
}
