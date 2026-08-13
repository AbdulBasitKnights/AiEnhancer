package com.aiface.aging.ads_nextgen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper

/**
 * Inflate existing native layouts (Next-Gen NativeAdView root), bind assets, registerNativeAd.
 * Paid via onAdPaid (not setOnPaidEventListener).
 */
object NativeAdDisplayHelper {

    fun display(
        container: ViewGroup,
        inflater: LayoutInflater,
        nativeAd: NativeAd,
        onDestroyPrevious: () -> Unit = {},
        adUnitId: String? = null,
        @LayoutRes layoutResId: Int = R.layout.layout_native_ad_nextgen,
        shimmer: View? = null
    ) {
        AdMainThread.run {
            try {
                if (!AdsHelper.shouldShowAds()) {
                    failAndCleanup(container, shimmer, nativeAd)
                    return@run
                }
                onDestroyPrevious()
                hideExistingShimmerOnMain(shimmer)
                AdShimmerHelper.hideShimmerOnMain(container)
                container.removeAllViews()
                container.visibility = View.VISIBLE

                val nativeAdView = inflater.inflate(layoutResId, container, false) as NativeAdView
                nativeAdView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                container.addView(nativeAdView)
                bindAssets(nativeAdView, nativeAd, adUnitId, includeMedia = true)
            } catch (t: Throwable) {
                android.util.Log.e("NativeAdDisplay", "display failed", t)
                failAndCleanup(container, shimmer, nativeAd)
            }
        }
    }
    fun displayWithoutMedia(
        container: ViewGroup,
        inflater: LayoutInflater,
        nativeAd: NativeAd,
        onDestroyPrevious: () -> Unit = {},
        adUnitId: String? = null,
        @LayoutRes layoutResId: Int = R.layout.layout_native_ads_without_mediaview_b,
        shimmer: View? = null
    ) {
        AdMainThread.run {
            try {
                if (!AdsHelper.shouldShowAds()) {
                    failAndCleanup(container, shimmer, nativeAd)
                    return@run
                }
                onDestroyPrevious()
                hideExistingShimmerOnMain(shimmer)
                AdShimmerHelper.hideShimmerOnMain(container)
                container.removeAllViews()
                container.visibility = View.VISIBLE

                val nativeAdView = inflater.inflate(layoutResId, container, false) as NativeAdView
                nativeAdView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                container.addView(nativeAdView)
                bindAssets(nativeAdView, nativeAd, adUnitId, includeMedia = false)
            } catch (t: Throwable) {
                android.util.Log.e("NativeAdDisplay", "displayWithoutMedia failed", t)
                failAndCleanup(container, shimmer, nativeAd)
            }
        }
    }

    private fun failAndCleanup(container: ViewGroup, shimmer: View?, nativeAd: NativeAd) {
        try {
            try {
                nativeAd.destroy()
            } catch (_: Throwable) {
            }
            hideExistingShimmerOnMain(shimmer)
            AdShimmerHelper.hideShimmerOnMain(container)
            container.removeAllViews()
            container.visibility = View.GONE
            // Typical layout: clAd → shimmer + nativeAdView
            (container.parent as? View)?.visibility = View.GONE
        } catch (cleanupError: Throwable) {
            android.util.Log.e("NativeAdDisplay", "cleanup after display failure failed", cleanupError)
        }
    }

    fun bindIntoExisting(
        nativeAdView: NativeAdView,
        nativeAd: NativeAd,
        adUnitId: String? = null
    ) {
        bindAssets(nativeAdView, nativeAd, adUnitId, includeMedia = true)
    }

    private fun bindAssets(
        nativeAdView: NativeAdView,
        nativeAd: NativeAd,
        adUnitId: String?,
        includeMedia: Boolean = true,
    ) {
        try {
            nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
            nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
            nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)

            // Layouts may omit icon (e.g. layout_native_ads) — never force non-null findViewById.
            try {
                val iconView: View? =
                    nativeAdView.findViewById<View?>(R.id.ad_app_icon)
                        ?: nativeAdView.findViewById<View?>(R.id.ad_icon)
                nativeAdView.iconView = iconView
            } catch (e: Exception) {
                android.util.Log.e("NativeAdDisplay", "icon bind failed", e)
            }

            val mediaView: MediaView? = nativeAdView.findViewById(R.id.ad_media)
            if (!includeMedia) {
                mediaView?.visibility = View.GONE
            }

            (nativeAdView.headlineView as? TextView)?.apply {
                text = nativeAd.headline
                visibility = if (nativeAd.headline.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            (nativeAdView.bodyView as? TextView)?.apply {
                text = nativeAd.body
                visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            (nativeAdView.callToActionView as? TextView)?.apply {
                text = nativeAd.callToAction
                visibility = if (nativeAd.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            (nativeAdView.callToActionView as? Button)?.apply {
                text = nativeAd.callToAction
                visibility = if (nativeAd.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
            }

            val iconImage = nativeAdView.iconView as? ImageView
            if (nativeAd.icon?.drawable != null && iconImage != null) {
                iconImage.setImageDrawable(nativeAd.icon?.drawable)
                iconImage.visibility = View.VISIBLE
            } else {
                iconImage?.visibility = View.GONE
            }

            val paidUnitId = adUnitId ?: AdConstants.NATIVE
            NextGenNativeLoader.attachPaidCallback(nativeAd, paidUnitId)
            // Re-attach so display-time callback wins if loader already set one
            nativeAd.adEventCallback = object : NativeAdEventCallback {
                override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                    try {
                        NextGenAdRevenue.track(paidUnitId, value, "Native")
                    } catch (_: Throwable) {
                    }
                }

                override fun onAdImpression() = Unit
                override fun onAdClicked() = Unit
                override fun onAdShowedFullScreenContent() = Unit
                override fun onAdDismissedFullScreenContent() = Unit
                override fun onAdFailedToShowFullScreenContent(
                    fullScreenContentError: FullScreenContentError
                ) = Unit
            }

            nativeAdView.registerNativeAd(nativeAd, if (includeMedia) mediaView else null)
        } catch (t: Throwable) {
            android.util.Log.e("NativeAdDisplay", "bindAssets failed", t)
            throw t
        }
    }

    private fun hideExistingShimmerOnMain(shimmer: View?) {
        when (shimmer) {
            null -> Unit
            is ShimmerFrameLayout -> {
                try {
                    shimmer.stopShimmer()
                } catch (_: Exception) {
                }
                shimmer.visibility = View.GONE
            }
            is ViewGroup -> {
                stopNestedShimmers(shimmer)
                shimmer.visibility = View.GONE
            }
            else -> shimmer.visibility = View.GONE
        }
    }

    private fun stopNestedShimmers(root: ViewGroup) {
        for (index in 0 until root.childCount) {
            when (val child = root.getChildAt(index)) {
                is ShimmerFrameLayout -> {
                    try {
                        child.stopShimmer()
                    } catch (_: Exception) {
                    }
                    child.visibility = View.GONE
                }
                is ViewGroup -> stopNestedShimmers(child)
            }
        }
    }
}
