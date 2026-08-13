package com.aiface.aging.ads_nextgen

import android.R
import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Edge-to-edge fix for GMA Next-Gen fullscreen [AdActivity]
 * (interstitial / rewarded / app open).
 *
 * Applies status-bar + cutout + nav insets as padding so close/skip
 * stay clear of system bars on Android 15+.
 */
object FullscreenAdInsetsHelper {

    private const val NEXT_GEN_AD_ACTIVITY =
        "com.google.android.libraries.ads.mobile.sdk.common.AdActivity"
    private const val LEGACY_AD_ACTIVITY =
        "com.google.android.gms.ads.AdActivity"

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (isFullscreenAdActivity(activity)) prepareWindow(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    if (isFullscreenAdActivity(activity)) applyInsets(activity)
                }

                override fun onActivityResumed(activity: Activity) {
                    if (isFullscreenAdActivity(activity)) applyInsets(activity)
                }

                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }

    fun isFullscreenAdActivity(activity: Activity): Boolean {
        val name = activity.javaClass.name
        return name == NEXT_GEN_AD_ACTIVITY ||
            name == LEGACY_AD_ACTIVITY ||
            (name.contains("ads", ignoreCase = true) && name.endsWith("AdActivity"))
    }

    private fun prepareWindow(activity: Activity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun applyInsets(activity: Activity) {
        prepareWindow(activity)

        val content = activity.findViewById<ViewGroup>(R.id.content) ?: return

        val applyPadding: (View, WindowInsetsCompat) -> Unit = { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        }

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val host = view as ViewGroup
            val child = host.getChildAt(0)
            if (child != null) {
                applyPadding(child, insets)
                host.setPadding(0, 0, 0, 0)
            } else {
                applyPadding(host, insets)
            }
            insets
        }

        // Status bar stays (top padding). Hide nav like the rest of the app.
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.show(WindowInsetsCompat.Type.statusBars())
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        fun request() {
            ViewCompat.requestApplyInsets(content)
            content.getChildAt(0)?.let { child ->
                val rootInsets = ViewCompat.getRootWindowInsets(content) ?: return@let
                applyPadding(child, rootInsets)
            }
        }

        content.post {
            request()
            // Ad content often attaches after first frame.
            content.postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) request()
            }, 150)
            content.postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) request()
            }, 400)
        }
    }
}
