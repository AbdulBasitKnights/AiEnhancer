package com.aiface.aging.ads_nextgen

import android.app.Activity
import com.aiface.aging.shared.ads.InterstitialAdGate
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader

/**
 * Interstitial show continues navigation early (not on ad dismiss):
 * - Activity → activity: [onContinue] / Idle immediately after [InterstitialAd.show]
 * - Fragment → fragment: same after [AdMainThread.FRAGMENT_CONTINUE_DELAY_MS]
 *
 * Real dismiss only clears fullscreen flags (avoids navigate delay).
 */
class InterstitialAdRepository {

    private var normalAd: InterstitialAd? = null
    private var normalAdUnitId: String? = null
    private var isNormalLoading = false
    private var isPreloadStarted = false

    fun loadNormal(onState: (AdUiState) -> Unit) {
        loadNormal(AdConstants.INTERSTITIAL, onState)
    }

    fun loadNormal(adUnitId: String, onState: (AdUiState) -> Unit) {
        if (normalAd != null) {
            NextGenAdCheck.skip(NextGenAdCheck.INTER, adUnitId, "normal cache already loaded")
            onState(AdUiState.Ready(AdFormat.INTERSTITIAL, AdLoadMode.NORMAL))
            return
        }
        if (isNormalLoading) {
            NextGenAdCheck.skip(NextGenAdCheck.INTER, adUnitId, "normal load already in progress")
            return
        }

        isNormalLoading = true
        onState(AdUiState.Loading(AdFormat.INTERSTITIAL, AdLoadMode.NORMAL))
        NextGenAdCheck.request(NextGenAdCheck.INTER, adUnitId, "mode=normal")
        InterstitialAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isNormalLoading = false
                    normalAd = ad
                    normalAdUnitId = adUnitId
                    NextGenAdCheck.loaded(NextGenAdCheck.INTER, adUnitId, "mode=normal")
                    onState(AdUiState.Ready(AdFormat.INTERSTITIAL, AdLoadMode.NORMAL))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isNormalLoading = false
                    normalAd = null
                    normalAdUnitId = null
                    NextGenAdCheck.failed(NextGenAdCheck.INTER, adUnitId, adError.message, "mode=normal")
                    onState(AdUiState.Error(AdFormat.INTERSTITIAL, AdLoadMode.NORMAL, adError.message))
                }
            }
        )
    }

    private var preloadAdUnitId: String = AdConstants.INTERSTITIAL

    fun startPreload(onState: (AdUiState) -> Unit) {
        startPreload(AdConstants.INTERSTITIAL, onState)
    }

    fun startPreload(adUnitId: String, onState: (AdUiState) -> Unit) {
        if (isPreloadStarted && preloadAdUnitId == adUnitId) {
            NextGenAdCheck.skip(
                NextGenAdCheck.INTER,
                adUnitId,
                "preload already active buffer=${getPreloadCount()}",
            )
            onState(
                AdUiState.Ready(
                    AdFormat.INTERSTITIAL,
                    AdLoadMode.PRELOAD,
                    getPreloadCount()
                )
            )
            return
        }

        if (isPreloadStarted && preloadAdUnitId != adUnitId) {
            destroyPreload()
        }

        preloadAdUnitId = adUnitId
        onState(AdUiState.Loading(AdFormat.INTERSTITIAL, AdLoadMode.PRELOAD))
        NextGenAdCheck.request(NextGenAdCheck.INTER, adUnitId, "mode=preload")
        val config = AdPreloadConfigFactory.create(AdRequest.Builder(adUnitId).build())
        val started = InterstitialAdPreloader.start(
            AdConstants.PRELOAD_INTERSTITIAL,
            config,
            object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                  /*  NextGenAdCheck.loaded(
                        NextGenAdCheck.INTER,
                        adUnitId,
                        "mode=preload buffer=${getPreloadCount()}",
                    )*/
                    onState(
                        AdUiState.Ready(
                            AdFormat.INTERSTITIAL,
                            AdLoadMode.PRELOAD,
                            getPreloadCount()
                        )
                    )
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    NextGenAdCheck.failed(
                        NextGenAdCheck.INTER,
                        adUnitId,
                        adError.message,
                        "mode=preload",
                    )
                    onState(
                        AdUiState.Error(
                            AdFormat.INTERSTITIAL,
                            AdLoadMode.PRELOAD,
                            adError.message
                        )
                    )
                }
            }
        )
        isPreloadStarted = started
        if (!started) {
            NextGenAdCheck.skip(
                NextGenAdCheck.INTER,
                adUnitId,
                "preload start returned false buffer=${getPreloadCount()}",
            )
            onState(
                AdUiState.Ready(
                    AdFormat.INTERSTITIAL,
                    AdLoadMode.PRELOAD,
                    getPreloadCount()
                )
            )
        }
    }

    fun getPreloadAdUnitId(): String = preloadAdUnitId

    /** Take one preloaded ad; caller holds only that one until shown. */
    fun pollPreloadedAd(): InterstitialAd? =
        InterstitialAdPreloader.pollAd(AdConstants.PRELOAD_INTERSTITIAL)

    fun getPreloadCount(): Int =
        InterstitialAdPreloader.getNumAdsAvailable(AdConstants.PRELOAD_INTERSTITIAL)

    fun isPreloadActive(): Boolean = isPreloadStarted

    fun showNormal(
        activity: Activity,
        forFragment: Boolean = false,
        onState: (AdUiState) -> Unit,
    ) {
        val ad = normalAd
        if (ad == null) {
            onState(AdUiState.Error(AdFormat.INTERSTITIAL, AdLoadMode.NORMAL, "No cached ad. Load first."))
            return
        }
        val unitId = normalAdUnitId ?: AdConstants.INTERSTITIAL
        showAd(activity, ad, unitId, AdLoadMode.NORMAL, onState, forFragment) {
            normalAd = null
            normalAdUnitId = null
        }
    }

    fun showPreloaded(
        activity: Activity,
        forFragment: Boolean = false,
        onState: (AdUiState) -> Unit,
    ) {
        val ad = pollPreloadedAd()
        if (ad == null) {
            onState(AdUiState.Error(AdFormat.INTERSTITIAL, AdLoadMode.PRELOAD, "Buffer empty. Preload first."))
            return
        }
        showAd(activity, ad, preloadAdUnitId, AdLoadMode.PRELOAD, onState, forFragment)
    }

    private fun showAd(
        activity: Activity,
        ad: InterstitialAd,
        adUnitId: String,
        mode: AdLoadMode,
        onState: (AdUiState) -> Unit,
        forFragment: Boolean,
        onContinueExtra: () -> Unit = {},
    ) {
        AdMainThread.run {
            if (InterstitialAdGate.shouldSkipInterstitial()) {
                NextGenAdCheck.skip(
                    NextGenAdCheck.INTER,
                    adUnitId,
                    "app-open inter cooldown active",
                )
                onContinueExtra()
                onState(
                    AdUiState.Error(
                        AdFormat.INTERSTITIAL,
                        mode,
                        "app-open inter cooldown active",
                    ),
                )
                return@run
            }
            var continueDelivered = false
            var pendingContinue: Runnable? = null

            fun deliverContinueOnce(successIdle: Boolean) {
                if (continueDelivered) return
                continueDelivered = true
                AdMainThread.cancel(pendingContinue)
                pendingContinue = null
                AdMainThread.run {
                    onContinueExtra()
                    if (successIdle) {
                        onState(AdUiState.Idle)
                    }
                }
            }

            fun scheduleFragmentContinueAfterShow() {
                if (continueDelivered || !forFragment) return
                pendingContinue = AdMainThread.postDelayed(AdMainThread.FRAGMENT_CONTINUE_DELAY_MS) {
                    deliverContinueOnce(successIdle = true)
                }
            }

            onState(AdUiState.Showing(AdFormat.INTERSTITIAL))
            AppOpenAdManager.isFullscreenAdShowing = true
            com.aiface.aging.shared.ads.isShowingAd = true
            ad.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                    NextGenAdRevenue.track(adUnitId, value, "Interstitial")
                }

                override fun onAdImpression() {
                    NextGenAdCheck.impression(NextGenAdCheck.INTER, adUnitId)
                }

                override fun onAdShowedFullScreenContent() {
                    AdMainThread.run {
                        scheduleFragmentContinueAfterShow()
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    // Do not navigate here — early continue already fired.
                    AdMainThread.run {
                        AppOpenAdManager.isFullscreenAdShowing = false
                        com.aiface.aging.shared.ads.isShowingAd = false
                        InterstitialAdGate.onInterstitialAdDismissed()
                        HostUiAfterFullscreenRestorer.restore(activity)
                    }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    AdMainThread.run {
                        AppOpenAdManager.isFullscreenAdShowing = false
                        com.aiface.aging.shared.ads.isShowingAd = false
                        HostUiAfterFullscreenRestorer.restore(activity)
                        NextGenAdCheck.failed(
                            NextGenAdCheck.INTER,
                            adUnitId,
                            fullScreenContentError.message,
                            "mode=show",
                        )
                        if (!continueDelivered) {
                            continueDelivered = true
                            AdMainThread.cancel(pendingContinue)
                            pendingContinue = null
                            onContinueExtra()
                            onState(
                                AdUiState.Error(
                                    AdFormat.INTERSTITIAL,
                                    mode,
                                    fullScreenContentError.message
                                )
                            )
                        }
                    }
                }
            }
            if (!forFragment) {
                deliverContinueOnce(successIdle = true)
            }
            ad.show(activity)
        }
    }

    fun destroyPreload() {
        InterstitialAdPreloader.destroy(AdConstants.PRELOAD_INTERSTITIAL)
        isPreloadStarted = false
    }

    fun clearNormalCache() {
        normalAd = null
        normalAdUnitId = null
    }

    fun hasNormalCache(): Boolean = normalAd != null

    companion object {
        // Logs via NextGenAdCheck
    }
}
