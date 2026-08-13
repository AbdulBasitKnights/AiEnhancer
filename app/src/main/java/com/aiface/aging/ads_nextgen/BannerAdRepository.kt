package com.aiface.aging.ads_nextgen

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo

class BannerAdRepository {

    private var normalAd: BannerAd? = null
    private var normalAdView: AdView? = null
    private var normalAdUnitId: String? = null
    private var isNormalLoading = false
    private var isPreloadStarted = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    fun loadNormal(activity: Activity, adView: AdView, onState: (AdUiState) -> Unit) {
        loadNormal(activity, adView, AdConstants.BANNER, onState)
    }

    fun loadNormal(
        activity: Activity,
        adView: AdView,
        adUnitId: String,
        onState: (AdUiState) -> Unit
    ) {
        if (normalAd != null) {
            NextGenAdCheck.skip(NextGenAdCheck.BANNER, adUnitId, "normal cache already loaded")
            onState(AdUiState.Ready(AdFormat.BANNER, AdLoadMode.NORMAL))
            return
        }
        if (isNormalLoading) {
            NextGenAdCheck.skip(NextGenAdCheck.BANNER, adUnitId, "normal load already in progress")
            return
        }

        isNormalLoading = true
        normalAdView = adView
        normalAdUnitId = adUnitId
        BannerSizeHelper.applyMatchParentWidth(adView)
        onState(AdUiState.Loading(AdFormat.BANNER, AdLoadMode.NORMAL))
        NextGenAdCheck.request(NextGenAdCheck.BANNER, adUnitId, "mode=normal")

        val adSize = BannerSizeHelper.adaptiveMatchParentSize(activity)
        val request = BannerAdRequest.Builder(adUnitId, adSize).build()
        adView.loadAd(
            request,
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    onMain {
                        isNormalLoading = false
                        normalAd = ad
                        attachBannerCallbacks(ad, adUnitId)
                        // Next-Gen: must bind BannerAd to AdView or nothing renders.
                        normalAdView?.registerBannerAd(ad, activity)
                        NextGenAdCheck.loaded(NextGenAdCheck.BANNER, adUnitId, "mode=normal")
                        onState(AdUiState.Ready(AdFormat.BANNER, AdLoadMode.NORMAL))
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    onMain {
                        isNormalLoading = false
                        normalAd = null
                        normalAdUnitId = null
                        NextGenAdCheck.failed(
                            NextGenAdCheck.BANNER,
                            adUnitId,
                            adError.message,
                            "mode=normal",
                        )
                        onState(AdUiState.Error(AdFormat.BANNER, AdLoadMode.NORMAL, adError.message))
                    }
                }
            }
        )
    }

    fun startPreload(activity: Activity, onState: (AdUiState) -> Unit) {
        val unitId = AdConstants.BANNER
        if (isPreloadStarted) {
            NextGenAdCheck.skip(
                NextGenAdCheck.BANNER,
                unitId,
                "preload already active buffer=${getPreloadCount()}",
            )
            onState(AdUiState.Ready(AdFormat.BANNER, AdLoadMode.PRELOAD, getPreloadCount()))
            return
        }

        onState(AdUiState.Loading(AdFormat.BANNER, AdLoadMode.PRELOAD))
        NextGenAdCheck.request(NextGenAdCheck.BANNER, unitId, "mode=preload")
        val adSize = BannerSizeHelper.adaptiveMatchParentSize(activity)
        val request = BannerAdRequest.Builder(unitId, adSize).build()
        val config = AdPreloadConfigFactory.create(request)
        val started = BannerAdPreloader.start(
            AdConstants.PRELOAD_BANNER,
            config,
            object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    onMain {
                        NextGenAdCheck.loaded(
                            NextGenAdCheck.BANNER,
                            unitId,
                            "mode=preload buffer=${getPreloadCount()}",
                        )
                        onState(AdUiState.Ready(AdFormat.BANNER, AdLoadMode.PRELOAD, getPreloadCount()))
                    }
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    onMain {
                        NextGenAdCheck.failed(
                            NextGenAdCheck.BANNER,
                            unitId,
                            adError.message,
                            "mode=preload",
                        )
                        onState(AdUiState.Error(AdFormat.BANNER, AdLoadMode.PRELOAD, adError.message))
                    }
                }
            }
        )
        isPreloadStarted = started
        if (!started) {
            NextGenAdCheck.skip(
                NextGenAdCheck.BANNER,
                unitId,
                "preload start returned false buffer=${getPreloadCount()}",
            )
            onState(AdUiState.Ready(AdFormat.BANNER, AdLoadMode.PRELOAD, getPreloadCount()))
        }
    }

    fun showPreloaded(activity: Activity, adView: AdView, onState: (AdUiState) -> Unit) {
        val ad = BannerAdPreloader.pollAd(AdConstants.PRELOAD_BANNER)
        if (ad == null) {
            NextGenAdCheck.failed(NextGenAdCheck.BANNER, AdConstants.BANNER, "Buffer empty. Preload first.")
            onState(AdUiState.Error(AdFormat.BANNER, AdLoadMode.PRELOAD, "Buffer empty. Preload first."))
            return
        }
        BannerSizeHelper.applyMatchParentWidth(adView)
        attachBannerCallbacks(ad, AdConstants.BANNER)
        adView.registerBannerAd(ad, activity)
        onState(AdUiState.Ready(AdFormat.BANNER, AdLoadMode.PRELOAD, getPreloadCount()))
    }

    fun getPreloadCount(): Int =
        BannerAdPreloader.getNumAdsAvailable(AdConstants.PRELOAD_BANNER)

    fun isPreloadActive(): Boolean = isPreloadStarted

    private fun attachBannerCallbacks(ad: BannerAd, adUnitId: String) {
        ad.adEventCallback = object : BannerAdEventCallback {
            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                NextGenAdRevenue.track(adUnitId, value, "Banner")
            }

            override fun onAdImpression() {
                NextGenAdCheck.impression(NextGenAdCheck.BANNER, adUnitId)
            }

            override fun onAdClicked() = Unit
            override fun onAdShowedFullScreenContent() = Unit
            override fun onAdDismissedFullScreenContent() = Unit
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = Unit
        }
    }

    fun destroyPreload() {
        BannerAdPreloader.destroy(AdConstants.PRELOAD_BANNER)
        isPreloadStarted = false
    }

    fun clearNormalCache() {
        normalAdView?.destroy()
        normalAdView = null
        normalAd = null
        normalAdUnitId = null
    }

    fun hasNormalCache(): Boolean = normalAd != null
}
