package com.aiface.aging.ads_nextgen

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.AdsHelper

/**
 * Screen-owned rewarded load/show. Paid via [RewardedAdEventCallback.onAdPaid].
 *
 * All GMA event callbacks are marshaled to the main thread before UI / flags.
 */
object NextGenRewardedHelper {

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    fun load(
        adUnitId: String,
        onLoaded: (RewardedAd) -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (!AdsHelper.shouldShowAds()) {
            NextGenAdCheck.skip(NextGenAdCheck.REWARD, adUnitId, "shouldShowAds=false / pro")
            onMain { onFailed("ads disabled") }
            return
        }
        NextGenAdCheck.request(NextGenAdCheck.REWARD, adUnitId, "mode=helper")
        RewardedAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    onMain {
                        NextGenAdCheck.loaded(NextGenAdCheck.REWARD, adUnitId, "mode=helper")
                        onLoaded(ad)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    onMain {
                        NextGenAdCheck.failed(
                            NextGenAdCheck.REWARD,
                            adUnitId,
                            adError.message,
                            "mode=helper",
                        )
                        onFailed(adError.message)
                    }
                }
            }
        )
    }

    fun loadWithFallback(
        tryHigh: Boolean,
        highUnitId: String,
        normalUnitId: String,
        onLoaded: (ad: RewardedAd, adUnitId: String) -> Unit,
        onFailed: () -> Unit
    ) {
        if (tryHigh) {
            load(
                adUnitId = highUnitId,
                onLoaded = { onLoaded(it, highUnitId) },
                onFailed = {
                    load(
                        adUnitId = normalUnitId,
                        onLoaded = { onLoaded(it, normalUnitId) },
                        onFailed = { onFailed() }
                    )
                }
            )
        } else {
            load(
                adUnitId = normalUnitId,
                onLoaded = { onLoaded(it, normalUnitId) },
                onFailed = { onFailed() }
            )
        }
    }

    fun show(
        activity: Activity,
        ad: RewardedAd,
        adUnitId: String,
        onReward: () -> Unit,
        onShowed: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null,
        onFailedShow: (() -> Unit)? = null
    ) {
        onMain {
            if (activity.isFinishing || activity.isDestroyed) return@onMain
            if (isShowingAd || AppOpenAdManager.isFullscreenAdShowing) {
                NextGenAdCheck.skip(NextGenAdCheck.REWARD, adUnitId, "already fullscreen — skip show")
                onFailedShow?.invoke()
                return@onMain
            }
            AppOpenAdManager.isFullscreenAdShowing = true
            isShowingAd = true
            ad.adEventCallback = object : RewardedAdEventCallback {
                override fun onAdPaid(value: AdValue) {
                    NextGenAdRevenue.track(adUnitId, value, "Rewarded Ad")
                }

                override fun onAdShowedFullScreenContent() {
                    onMain { onShowed?.invoke() }
                }

                override fun onAdDismissedFullScreenContent() {
                    onMain {
                        AppOpenAdManager.isFullscreenAdShowing = false
                        isShowingAd = false
                        HostUiAfterFullscreenRestorer.restore(activity)
                        onDismissed?.invoke()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                    onMain {
                        AppOpenAdManager.isFullscreenAdShowing = false
                        isShowingAd = false
                        HostUiAfterFullscreenRestorer.restore(activity)
                        NextGenAdCheck.failed(
                            NextGenAdCheck.REWARD,
                            adUnitId,
                            error.message,
                            "mode=show",
                        )
                        onFailedShow?.invoke()
                    }
                }

                override fun onAdImpression() {
                    NextGenAdCheck.impression(NextGenAdCheck.REWARD, adUnitId)
                }

                override fun onAdClicked() = Unit
            }
            ad.show(activity, OnUserEarnedRewardListener { onMain { onReward() } })
        }
    }
}
