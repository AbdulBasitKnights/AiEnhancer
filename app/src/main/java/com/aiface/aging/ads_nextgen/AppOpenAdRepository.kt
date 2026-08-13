package com.aiface.aging.ads_nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo

class AppOpenAdRepository {

    private var normalAd: AppOpenAd? = null
    private var normalLoadTimeMs: Long = 0L
    private var isNormalLoading = false
    private var isPreloadStarted = false

    fun loadNormal(onState: (AdUiState) -> Unit) {
        val unitId = AdConstants.APP_OPEN
        if (isNormalAdAvailable()) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "normal cache already loaded")
            onState(AdUiState.Ready(AdFormat.APP_OPEN, AdLoadMode.NORMAL))
            return
        }
        if (isNormalLoading) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "normal load already in progress")
            return
        }

        isNormalLoading = true
        onState(AdUiState.Loading(AdFormat.APP_OPEN, AdLoadMode.NORMAL))
        NextGenAdCheck.request(NextGenAdCheck.OPEN_AD, unitId, "mode=normal")

        AppOpenAd.load(
            AdRequest.Builder(unitId).build(),
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isNormalLoading = false
                    normalAd = ad
                    normalLoadTimeMs = System.currentTimeMillis()
                    NextGenAdCheck.loaded(NextGenAdCheck.OPEN_AD, unitId, "mode=normal")
                    onState(AdUiState.Ready(AdFormat.APP_OPEN, AdLoadMode.NORMAL))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isNormalLoading = false
                    normalAd = null
                    NextGenAdCheck.failed(NextGenAdCheck.OPEN_AD, unitId, adError.message, "mode=normal")
                    onState(AdUiState.Error(AdFormat.APP_OPEN, AdLoadMode.NORMAL, adError.message))
                }
            }
        )
    }

    fun startPreload(onState: (AdUiState) -> Unit) {
        val unitId = AdConstants.APP_OPEN
        if (isPreloadStarted) {
            NextGenAdCheck.skip(
                NextGenAdCheck.OPEN_AD,
                unitId,
                "preload already active buffer=${getPreloadCount()}",
            )
            onState(AdUiState.Ready(AdFormat.APP_OPEN, AdLoadMode.PRELOAD, getPreloadCount()))
            return
        }

        onState(AdUiState.Loading(AdFormat.APP_OPEN, AdLoadMode.PRELOAD))
        NextGenAdCheck.request(NextGenAdCheck.OPEN_AD, unitId, "mode=preload")
        val config = AdPreloadConfigFactory.create(AdRequest.Builder(unitId).build())
        val started = AppOpenAdPreloader.start(
            AdConstants.PRELOAD_APP_OPEN,
            config,
            object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    NextGenAdCheck.loaded(
                        NextGenAdCheck.OPEN_AD,
                        unitId,
                        "mode=preload buffer=${getPreloadCount()}",
                    )
                    onState(AdUiState.Ready(AdFormat.APP_OPEN, AdLoadMode.PRELOAD, getPreloadCount()))
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    NextGenAdCheck.failed(
                        NextGenAdCheck.OPEN_AD,
                        unitId,
                        adError.message,
                        "mode=preload",
                    )
                    onState(AdUiState.Error(AdFormat.APP_OPEN, AdLoadMode.PRELOAD, adError.message))
                }
            }
        )
        isPreloadStarted = started
        if (!started) {
            NextGenAdCheck.skip(
                NextGenAdCheck.OPEN_AD,
                unitId,
                "preload start returned false buffer=${getPreloadCount()}",
            )
            onState(AdUiState.Ready(AdFormat.APP_OPEN, AdLoadMode.PRELOAD, getPreloadCount()))
        }
    }

    fun getPreloadCount(): Int =
        AppOpenAdPreloader.getNumAdsAvailable(AdConstants.PRELOAD_APP_OPEN)

    fun isPreloadActive(): Boolean = isPreloadStarted

    fun showNormal(activity: Activity, onState: (AdUiState) -> Unit) {
        val ad = normalAd
        if (ad == null || !isNormalAdAvailable()) {
            normalAd = null
            NextGenAdCheck.failed(NextGenAdCheck.OPEN_AD, AdConstants.APP_OPEN, "No valid cached ad.")
            onState(AdUiState.Error(AdFormat.APP_OPEN, AdLoadMode.NORMAL, "No valid cached ad."))
            return
        }
        showAd(activity, ad, AdLoadMode.NORMAL, onState) { normalAd = null }
    }

    fun showPreloaded(activity: Activity, onState: (AdUiState) -> Unit) {
        val ad = AppOpenAdPreloader.pollAd(AdConstants.PRELOAD_APP_OPEN)
        if (ad == null) {
            NextGenAdCheck.failed(NextGenAdCheck.OPEN_AD, AdConstants.APP_OPEN, "Buffer empty. Preload first.")
            onState(AdUiState.Error(AdFormat.APP_OPEN, AdLoadMode.PRELOAD, "Buffer empty. Preload first."))
            return
        }
        showAd(activity, ad, AdLoadMode.PRELOAD, onState)
    }

    private fun showAd(
        activity: Activity,
        ad: AppOpenAd,
        mode: AdLoadMode,
        onState: (AdUiState) -> Unit,
        onDismissExtra: () -> Unit = {}
    ) {
        val unitId = AdConstants.APP_OPEN
        AdMainThread.run {
            onState(AdUiState.Showing(AdFormat.APP_OPEN))
            AppOpenAdManager.isFullscreenAdShowing = true
            ad.adEventCallback = object : AppOpenAdEventCallback {
                override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                    NextGenAdRevenue.track(unitId, value, "App Open")
                }

                override fun onAdImpression() {
                    NextGenAdCheck.impression(NextGenAdCheck.OPEN_AD, unitId)
                }

                override fun onAdDismissedFullScreenContent() {
                    AdMainThread.run {
                        AppOpenAdManager.isFullscreenAdShowing = false
                        onDismissExtra()
                        onState(AdUiState.Idle)
                    }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    AdMainThread.run {
                        AppOpenAdManager.isFullscreenAdShowing = false
                        onDismissExtra()
                        NextGenAdCheck.failed(
                            NextGenAdCheck.OPEN_AD,
                            unitId,
                            fullScreenContentError.message,
                            "mode=show",
                        )
                        onState(AdUiState.Error(AdFormat.APP_OPEN, mode, fullScreenContentError.message))
                    }
                }
            }
            ad.show(activity)
        }
    }

    private fun isNormalAdAvailable(): Boolean {
        if (normalAd == null) return false
        val ageHours = (System.currentTimeMillis() - normalLoadTimeMs) / 3_600_000L
        return ageHours < AdConstants.APP_OPEN_MAX_AGE_HOURS
    }

    fun destroyPreload() {
        AppOpenAdPreloader.destroy(AdConstants.PRELOAD_APP_OPEN)
        isPreloadStarted = false
    }

    fun clearNormalCache() {
        normalAd = null
    }

    fun hasNormalCache(): Boolean = isNormalAdAvailable()
}
