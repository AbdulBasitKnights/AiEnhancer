package com.aiface.aging.ads_nextgen

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.shimmer.ShimmerFrameLayout
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper

/**
 * Shows shimmer placeholders that mirror banner / native ad layouts.
 * All entry points swallow exceptions so ad UI never crashes the host screen.
 */
object AdShimmerHelper {

    private const val TAG = "AdShimmerHelper"

    fun showBannerShimmer(container: ViewGroup) {
        safeMain {
            if (!AdsHelper.shouldShowAds()) {
                hideShimmerOnMain(container)
                container.visibility = View.GONE
                return@safeMain
            }
            showBannerShimmerOnMain(container)
        }
    }

    fun showNativeShimmer(container: ViewGroup) {
        safeMain {
            if (!AdsHelper.shouldShowAds()) {
                hideShimmerOnMain(container)
                container.visibility = View.GONE
                return@safeMain
            }
            showNativeShimmerOnMain(container)
        }
    }

    fun hideShimmer(container: ViewGroup) {
        safeMain { hideShimmerOnMain(container) }
    }

    fun showNativeShimmerWithoutMedia(container: ViewGroup) {
        safeMain {
            if (!AdsHelper.shouldShowAds()) {
                hideShimmerWithoutMediaOnMain(container)
                container.visibility = View.GONE
                return@safeMain
            }
            showNativeShimmerWithoutMediaOnMain(container)
        }
    }

    fun hideShimmerWithoutMedia(container: ViewGroup) {
        safeMain { hideShimmerWithoutMediaOnMain(container) }
    }

    /**
     * Show XML placeholder while native request is in-flight.
     * [shimmerWrapper] = FrameLayout `@+id/shimmer` that includes layout_loading_ads_native_*.
     */
    fun showLayoutNativePlaceholder(
        adSlot: View?,
        shimmerWrapper: View?,
        nativeContainer: View? = null,
    ) {
        safeMain {
            if (!AdsHelper.shouldShowAds()) {
                hideNativeAdSlot(adSlot, shimmerWrapper, nativeContainer)
                return@safeMain
            }
            adSlot?.visibility = View.VISIBLE
            nativeContainer?.visibility = View.GONE
            shimmerWrapper?.visibility = View.VISIBLE
            val shimmer = findShimmerFrame(shimmerWrapper)
            shimmer?.visibility = View.VISIBLE
            shimmer?.startShimmerSafely()
        }
    }

    fun hideLayoutNativePlaceholder(shimmerWrapper: View?) {
        safeMain {
            val shimmer = findShimmerFrame(shimmerWrapper)
            shimmer?.stopShimmerSafely()
            shimmer?.visibility = View.GONE
            shimmerWrapper?.visibility = View.GONE
        }
    }

    /** Hide whole native slot after load/display failure. */
    fun hideNativeAdSlot(
        adSlot: View?,
        shimmerWrapper: View? = null,
        nativeContainer: View? = null,
    ) {
        safeMain {
            val shimmer = findShimmerFrame(shimmerWrapper)
            shimmer?.stopShimmerSafely()
            shimmer?.visibility = View.GONE
            shimmerWrapper?.visibility = View.GONE
            if (nativeContainer is ViewGroup) {
                try {
                    nativeContainer.removeAllViews()
                } catch (_: Throwable) {
                }
            }
            nativeContainer?.visibility = View.GONE
            adSlot?.visibility = View.GONE
        }
    }

    private fun safeMain(block: () -> Unit) {
        AdMainThread.run {
            try {
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "shimmer UI failed", t)
            }
        }
    }

    private fun findShimmerFrame(root: View?): ShimmerFrameLayout? {
        return try {
            if (root == null) return null
            if (root is ShimmerFrameLayout) return root
            if (root is ViewGroup) {
                for (i in 0 until root.childCount) {
                    findShimmerFrame(root.getChildAt(i))?.let { return it }
                }
            }
            null
        } catch (t: Throwable) {
            Log.e(TAG, "findShimmerFrame failed", t)
            null
        }
    }

    internal fun hideShimmerOnMain(container: ViewGroup) {
        try {
            val toRemove = mutableListOf<View>()
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child.tag == TAG_BANNER || child.tag == TAG_NATIVE || child is ShimmerFrameLayout) {
                    (child as? ShimmerFrameLayout)?.stopShimmerSafely()
                    toRemove.add(child)
                }
            }
            toRemove.forEach { child ->
                try {
                    container.removeView(child)
                } catch (_: Throwable) {
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "hideShimmerOnMain failed", t)
        }
    }

    private fun showBannerShimmerOnMain(container: ViewGroup) {
        hideShimmerOnMain(container)
        val shimmer = LayoutInflater.from(container.context)
            .inflate(R.layout.load_fb_banner, container, false)
        shimmer.tag = TAG_BANNER
        shimmer.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.addView(shimmer)
        shimmer.bringToFront()
        (shimmer as? ShimmerFrameLayout)?.startShimmerSafely()
        shimmer.visibility = View.VISIBLE
    }

    private fun showNativeShimmerOnMain(container: ViewGroup) {
        hideShimmerOnMain(container)
        val shimmer = LayoutInflater.from(container.context)
            .inflate(R.layout.layout_loading_ads_native_home, container, false)
        shimmer.tag = TAG_NATIVE
        shimmer.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.addView(shimmer)
        shimmer.bringToFront()
        (shimmer as? ShimmerFrameLayout)?.startShimmerSafely()
        shimmer.visibility = View.VISIBLE
    }

    private fun showNativeShimmerWithoutMediaOnMain(container: ViewGroup) {
        hideShimmerWithoutMediaOnMain(container)
        val shimmer = LayoutInflater.from(container.context)
            .inflate(R.layout.layout_loading_ads_native_small, container, false)
        shimmer.tag = TAG_NATIVE
        shimmer.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.addView(shimmer)
        shimmer.bringToFront()
        (shimmer as? ShimmerFrameLayout)?.startShimmerSafely()
        shimmer.visibility = View.VISIBLE
    }

    private fun hideShimmerWithoutMediaOnMain(container: ViewGroup) {
        try {
            val toRemove = mutableListOf<View>()
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child.tag == TAG_BANNER || child.tag == TAG_NATIVE || child is ShimmerFrameLayout) {
                    (child as? ShimmerFrameLayout)?.stopShimmerSafely()
                    toRemove.add(child)
                }
            }
            toRemove.forEach { child ->
                try {
                    container.removeView(child)
                } catch (_: Throwable) {
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "hideShimmerWithoutMediaOnMain failed", t)
        }
    }

    private fun ShimmerFrameLayout.startShimmerSafely() {
        try {
            startShimmer()
        } catch (_: Throwable) {
        }
    }

    private fun ShimmerFrameLayout.stopShimmerSafely() {
        try {
            stopShimmer()
        } catch (_: Throwable) {
        }
    }

    private const val TAG_BANNER = "shimmer_banner"
    private const val TAG_NATIVE = "shimmer_native"
}
