package com.aiface.aging.ads_nextgen

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.aiface.aging.R
import com.aiface.aging.shared.hideNavigationBar

/**
 * After fullscreen ad: wipe bottom padding, hide nav again, restretch banner containers.
 * Fixes white gap under banner + progressive banner shrink.
 */
object HostUiAfterFullscreenRestorer {

    fun restore(activity: Activity) {
        AdMainThread.run {
            if (activity.isFinishing || activity.isDestroyed) return@run
            if (FullscreenAdInsetsHelper.isFullscreenAdActivity(activity)) return@run

            try {
                apply(activity)
                activity.window?.decorView?.post {
                    if (activity.isFinishing || activity.isDestroyed) return@post
                    apply(activity)
                }
            } catch (_: Exception) {
                // best-effort
            }
        }
    }

    private fun apply(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (activity is FragmentActivity) {
            activity.hideNavigationBar()
        }

        clearBottomPadding(activity.findViewById(android.R.id.content))
        activity.findViewById<View>(R.id.main)?.let { clearBottomPadding(it) }
        activity.findViewById<View>(R.id.clAd)?.let { clearBottomPadding(it) }

        BannerSizeHelper.restoreBannerContainers(activity)

        ViewCompat.requestApplyInsets(activity.window.decorView)
        activity.findViewById<ViewGroup>(android.R.id.content)?.requestLayout()
    }

    private fun clearBottomPadding(view: View?) {
        view ?: return
        if (view.paddingBottom != 0) {
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, 0)
        }
        if (view is ViewGroup) {
            for (i in 0 until minOf(view.childCount, 8)) {
                val child = view.getChildAt(i)
                if (child.paddingBottom != 0) {
                    child.setPadding(child.paddingLeft, child.paddingTop, child.paddingRight, 0)
                }
            }
        }
    }
}
