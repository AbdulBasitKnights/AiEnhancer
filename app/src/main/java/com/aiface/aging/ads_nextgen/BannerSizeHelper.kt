package com.aiface.aging.ads_nextgen

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowMetrics
import android.widget.FrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView

object BannerSizeHelper {

    /**
     * Anchored adaptive (sticky bottom) — stable height.
     * Inline adaptive with maxHeight=60 was shrinking after fullscreen window metrics changes.
     */
    fun adaptiveMatchParentSize(activity: Activity): AdSize {
        val adWidthDp = screenWidthDp(activity).coerceAtLeast(320)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)
    }

    /** Prefer full display width so size does not shrink after immersive/fullscreen. */
    fun screenWidthDp(activity: Activity): Int {
        val density = activity.resources.displayMetrics.density
        val widthPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics: WindowMetrics = activity.windowManager.maximumWindowMetrics
            metrics.bounds.width()
        } else {
            val out = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getRealMetrics(out)
            out.widthPixels
        }
        return (widthPx / density).toInt()
    }

    fun applyMatchParentWidth(adView: AdView) {
        adView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /** Re-apply layout after fullscreen so banner does not keep a collapsed size. */
    fun restoreBannerContainers(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        restoreBannerViews(content)
    }

    private fun restoreBannerViews(group: ViewGroup) {
        for (i in 0 until group.childCount) {
            when (val child = group.getChildAt(i)) {
                is AdView -> {
                    applyMatchParentWidth(child)
                    child.requestLayout()
                }
                is FrameLayout -> {
                    if (child.id == com.aiface.aging.R.id.bannerAdView ||
                        child.id == com.aiface.aging.R.id.clAd
                    ) {
                        child.minimumHeight = 0
                        if (child.paddingBottom != 0) {
                            child.setPadding(
                                child.paddingLeft,
                                child.paddingTop,
                                child.paddingRight,
                                0
                            )
                        }
                        child.requestLayout()
                    }
                    restoreBannerViews(child)
                }
                is ViewGroup -> restoreBannerViews(child)
            }
        }
    }
}
