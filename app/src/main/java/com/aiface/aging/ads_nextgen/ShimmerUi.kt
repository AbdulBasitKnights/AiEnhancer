package com.aiface.aging.ads_nextgen

import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout

/** Facebook Shimmer animators must run on the main looper. */
internal fun ShimmerFrameLayout.stopShimmerOnMain() {
    AdMainThread.run {
        try {
            stopShimmer()
        } catch (_: Exception) {
        }
    }
}

internal fun ShimmerFrameLayout.startShimmerOnMain() {
    AdMainThread.run {
        try {
            startShimmer()
        } catch (_: Exception) {
        }
    }
}

internal fun hideShimmerView(shimmer: View?) {
    AdMainThread.run {
        when (shimmer) {
            null -> Unit
            is ShimmerFrameLayout -> {
                try {
                    shimmer.stopShimmer()
                } catch (_: Exception) {
                }
                shimmer.visibility = View.GONE
            }
            else -> shimmer.visibility = View.GONE
        }
    }
}
