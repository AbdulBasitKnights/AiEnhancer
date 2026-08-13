package com.aiface.aging.ads_nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener

class RewardedAdRepository {

    private var normalAd: RewardedAd? = null
    private var isNormalLoading = false
    private var isPreloadStarted = false

    fun loadNormal(onState: (AdUiState) -> Unit) {
        val unitId = AdConstants.REWARDED
        if (normalAd != null) {
            NextGenAdCheck.skip(NextGenAdCheck.REWARD, unitId, "normal cache already loaded")
            onState(AdUiState.Ready(AdFormat.REWARDED, AdLoadMode.NORMAL))
            return
        }
        if (isNormalLoading) {
            NextGenAdCheck.skip(NextGenAdCheck.REWARD, unitId, "normal load already in progress")
            return
        }

        isNormalLoading = true
        onState(AdUiState.Loading(AdFormat.REWARDED, AdLoadMode.NORMAL))
        NextGenAdCheck.request(NextGenAdCheck.REWARD, unitId, "mode=normal")

        RewardedAd.load(
            AdRequest.Builder(unitId).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    isNormalLoading = false
                    normalAd = ad
                    NextGenAdCheck.loaded(NextGenAdCheck.REWARD, unitId, "mode=normal")
                    onState(AdUiState.Ready(AdFormat.REWARDED, AdLoadMode.NORMAL))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isNormalLoading = false
                    normalAd = null
                    NextGenAdCheck.failed(NextGenAdCheck.REWARD, unitId, adError.message, "mode=normal")
                    onState(AdUiState.Error(AdFormat.REWARDED, AdLoadMode.NORMAL, adError.message))
                }
            }
        )
    }

    private var preloadAdUnitId: String = AdConstants.REWARDED

    fun startPreload(onState: (AdUiState) -> Unit) {
        startPreload(AdConstants.REWARDED, onState)
    }

    fun startPreload(adUnitId: String, onState: (AdUiState) -> Unit) {
        if (isPreloadStarted && preloadAdUnitId == adUnitId) {
            NextGenAdCheck.skip(
                NextGenAdCheck.REWARD,
                adUnitId,
                "preload already active buffer=${getPreloadCount()}",
            )
            onState(AdUiState.Ready(AdFormat.REWARDED, AdLoadMode.PRELOAD, getPreloadCount()))
            return
        }

        if (isPreloadStarted && preloadAdUnitId != adUnitId) {
            destroyPreload()
        }

        preloadAdUnitId = adUnitId
        onState(AdUiState.Loading(AdFormat.REWARDED, AdLoadMode.PRELOAD))
        NextGenAdCheck.request(NextGenAdCheck.REWARD, adUnitId, "mode=preload")
        val config = AdPreloadConfigFactory.create(AdRequest.Builder(adUnitId).build())
        val started = RewardedAdPreloader.start(
            AdConstants.PRELOAD_REWARDED,
            config,
            object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    NextGenAdCheck.loaded(
                        NextGenAdCheck.REWARD,
                        adUnitId,
                        "mode=preload buffer=${getPreloadCount()}",
                    )
                    onState(AdUiState.Ready(AdFormat.REWARDED, AdLoadMode.PRELOAD, getPreloadCount()))
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    NextGenAdCheck.failed(
                        NextGenAdCheck.REWARD,
                        adUnitId,
                        adError.message,
                        "mode=preload",
                    )
                    onState(AdUiState.Error(AdFormat.REWARDED, AdLoadMode.PRELOAD, adError.message))
                }
            }
        )
        isPreloadStarted = started
        if (!started) {
            NextGenAdCheck.skip(
                NextGenAdCheck.REWARD,
                adUnitId,
                "preload start returned false buffer=${getPreloadCount()}",
            )
            onState(AdUiState.Ready(AdFormat.REWARDED, AdLoadMode.PRELOAD, getPreloadCount()))
        }
    }

    fun getPreloadAdUnitId(): String = preloadAdUnitId

    fun pollPreloadedAd(): RewardedAd? =
        RewardedAdPreloader.pollAd(AdConstants.PRELOAD_REWARDED)

    fun getPreloadCount(): Int =
        RewardedAdPreloader.getNumAdsAvailable(AdConstants.PRELOAD_REWARDED)

    fun isPreloadActive(): Boolean = isPreloadStarted

    fun showNormal(activity: Activity, onState: (AdUiState) -> Unit, onReward: () -> Unit) {
        val ad = normalAd
        if (ad == null) {
            NextGenAdCheck.failed(NextGenAdCheck.REWARD, AdConstants.REWARDED, "No cached ad. Load first.")
            onState(AdUiState.Error(AdFormat.REWARDED, AdLoadMode.NORMAL, "No cached ad. Load first."))
            return
        }
        showAd(activity, ad, AdLoadMode.NORMAL, onState, onReward) { normalAd = null }
    }

    fun showPreloaded(activity: Activity, onState: (AdUiState) -> Unit, onReward: () -> Unit) {
        val ad = pollPreloadedAd()
        if (ad == null) {
            NextGenAdCheck.failed(NextGenAdCheck.REWARD, preloadAdUnitId, "Buffer empty. Preload first.")
            onState(AdUiState.Error(AdFormat.REWARDED, AdLoadMode.PRELOAD, "Buffer empty. Preload first."))
            return
        }
        showAd(activity, ad, AdLoadMode.PRELOAD, onState, onReward)
    }

    private fun showAd(
        activity: Activity,
        ad: RewardedAd,
        mode: AdLoadMode,
        onState: (AdUiState) -> Unit,
        onReward: () -> Unit,
        onDismissExtra: () -> Unit = {}
    ) {
        val unitId = preloadAdUnitId.ifBlank { AdConstants.REWARDED }
        AdMainThread.run {
            onState(AdUiState.Showing(AdFormat.REWARDED))
            AppOpenAdManager.isFullscreenAdShowing = true
            ad.adEventCallback = object : RewardedAdEventCallback {
                override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                    NextGenAdRevenue.track(AdConstants.REWARDED, value, "Rewarded")
                }

                override fun onAdImpression() {
                    NextGenAdCheck.impression(NextGenAdCheck.REWARD, unitId)
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
                            NextGenAdCheck.REWARD,
                            unitId,
                            fullScreenContentError.message,
                            "mode=show",
                        )
                        onState(AdUiState.Error(AdFormat.REWARDED, mode, fullScreenContentError.message))
                    }
                }
            }
            ad.show(
                activity,
                OnUserEarnedRewardListener { AdMainThread.run { onReward() } }
            )
        }
    }

    fun destroyPreload() {
        RewardedAdPreloader.destroy(AdConstants.PRELOAD_REWARDED)
        isPreloadStarted = false
    }

    fun clearNormalCache() {
        normalAd = null
    }

    fun hasNormalCache(): Boolean = normalAd != null
}
