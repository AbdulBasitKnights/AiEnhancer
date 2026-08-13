package com.aiface.aging.ads_nextgen

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper

/**
 * Load adaptive banner into a [FrameLayout] with existing shimmer layout.
 */
object NextGenBannerHelper {

    fun loadIntoContainer(
        activity: Activity,
        container: FrameLayout,
        adUnitId: String,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (!AdsHelper.shouldShowAds()) {
            NextGenAdCheck.skip(NextGenAdCheck.BANNER, adUnitId, "shouldShowAds=false / pro")
            AdMainThread.run {
                container.removeAllViews()
                container.visibility = View.GONE
                onFailed?.invoke()
            }
            return
        }
        container.removeAllViews()

        val shimmerView = LayoutInflater.from(activity)
            .inflate(R.layout.load_fb_banner, container, false)
        val shimmer =
            shimmerView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        shimmer?.startShimmer()
        container.addView(shimmerView)

        val adView = AdView(activity)
        BannerSizeHelper.applyMatchParentWidth(adView)
        container.addView(adView)

        val adSize = BannerSizeHelper.adaptiveMatchParentSize(activity)
        val request = BannerAdRequest.Builder(adUnitId, adSize).build()
        NextGenAdCheck.request(NextGenAdCheck.BANNER, adUnitId, "mode=helper")

        adView.loadAd(
            request,
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    AdMainThread.run {
                        shimmer?.stopShimmerOnMain()
                        shimmerView.visibility = View.GONE
                        ad.adEventCallback = object : BannerAdEventCallback {
                            override fun onAdPaid(value: AdValue) {
                                NextGenAdRevenue.track(adUnitId, value, "Banner")
                            }

                            override fun onAdImpression() {
                                NextGenAdCheck.impression(NextGenAdCheck.BANNER, adUnitId)
                            }

                            override fun onAdClicked() = Unit
                            override fun onAdShowedFullScreenContent() = Unit
                            override fun onAdDismissedFullScreenContent() = Unit
                            override fun onAdFailedToShowFullScreenContent(
                                fullScreenContentError: FullScreenContentError
                            ) = Unit
                        }
                        NextGenAdCheck.loaded(NextGenAdCheck.BANNER, adUnitId, "mode=helper")
                        onLoaded?.invoke()
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    AdMainThread.run {
                        shimmer?.stopShimmerOnMain()
                        container.removeAllViews()
                        NextGenAdCheck.failed(
                            NextGenAdCheck.BANNER,
                            adUnitId,
                            adError.message,
                            "mode=helper",
                        )
                        onFailed?.invoke()
                    }
                }
            }
        )
    }

    fun loadWithFallback(
        activity: Activity,
        container: FrameLayout,
        tryHigh: Boolean,
        highUnitId: String,
        normalUnitId: String,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (tryHigh) {
            loadIntoContainer(
                activity = activity,
                container = container,
                adUnitId = highUnitId,
                onLoaded = onLoaded,
                onFailed = {
                    loadIntoContainer(
                        activity = activity,
                        container = container,
                        adUnitId = normalUnitId,
                        onLoaded = onLoaded,
                        onFailed = onFailed
                    )
                }
            )
        } else {
            loadIntoContainer(
                activity = activity,
                container = container,
                adUnitId = normalUnitId,
                onLoaded = onLoaded,
                onFailed = onFailed
            )
        }
    }

    fun destroyChildren(container: ViewGroup?) {
        container ?: return
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is AdView) {
                child.destroy()
            }
        }
        container.removeAllViews()
    }
}
