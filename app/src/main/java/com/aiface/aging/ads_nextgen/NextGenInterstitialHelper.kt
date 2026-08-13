package com.aiface.aging.ads_nextgen

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.InterstitialAdGate
import com.aiface.aging.shared.ads.AdsHelper

/**
 * Screen-owned interstitial load/show (no shared AdsManager slot).
 *
 * Navigation continue timing:
 * - Activity hops: [onContinue] runs first, then [InterstitialAd.show] (parallel nav + ad).
 * - Fragment hops: [onContinue] after [onAdShowedFullScreenContent] + [FRAGMENT_CONTINUE_DELAY_MS].
 *
 * [onClosed] is the real dismiss / fail cleanup (flags already cleared).
 */
object NextGenInterstitialHelper {

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    fun load(
        adUnitId: String,
        onLoaded: (InterstitialAd) -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (!AdsHelper.shouldShowAds()) {
            NextGenAdCheck.skip(NextGenAdCheck.INTER, adUnitId, "shouldShowAds=false / pro")
            onMain { onFailed("ads disabled") }
            return
        }
        NextGenAdCheck.request(NextGenAdCheck.INTER, adUnitId, "mode=helper")
        InterstitialAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    onMain {
                        NextGenAdCheck.loaded(NextGenAdCheck.INTER, adUnitId, "mode=helper")
                        onLoaded(ad)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    onMain {
                        NextGenAdCheck.failed(
                            NextGenAdCheck.INTER,
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
        onLoaded: (ad: InterstitialAd, adUnitId: String) -> Unit,
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
        ad: InterstitialAd,
        adUnitId: String,
        forFragment: Boolean = false,
        onShowed: (() -> Unit)? = null,
        onContinue: (() -> Unit)? = null,
        onFailedShow: ((String) -> Unit)? = null,
        onImpression: (() -> Unit)? = null,
        onClosed: (() -> Unit)? = null,
    ) {
        onMain {
            if (activity.isFinishing || activity.isDestroyed) return@onMain
            if (InterstitialAdGate.shouldSkipInterstitial()) {
                NextGenAdCheck.skip(
                    NextGenAdCheck.INTER,
                    adUnitId,
                    "app-open inter cooldown active",
                )
                onContinue?.invoke()
                onClosed?.invoke()
                return@onMain
            }
            // Hard stop: never stack two fullscreen inters from monkey taps.
            if (isShowingAd || AppOpenAdManager.isFullscreenAdShowing) {
                NextGenAdCheck.skip(NextGenAdCheck.INTER, adUnitId, "already fullscreen — skip show")
                return@onMain
            }

            var continueDelivered = false
            var pendingContinue: Runnable? = null

            fun deliverContinueOnce() {
                if (continueDelivered) return
                continueDelivered = true
                AdMainThread.cancel(pendingContinue)
                pendingContinue = null
                // Always marshal — GMA(BG) must never run nav/UI continue directly.
                AdMainThread.run {
                    onContinue?.invoke()
                }
            }

            fun scheduleFragmentContinueAfterShow() {
                if (continueDelivered || !forFragment) return
                pendingContinue = AdMainThread.postDelayed(AdMainThread.FRAGMENT_CONTINUE_DELAY_MS) {
                    deliverContinueOnce()
                }
            }

            AppOpenAdManager.isFullscreenAdShowing = true
            isShowingAd = true

            ad.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdPaid(value: AdValue) {
                    NextGenAdRevenue.track(adUnitId, value, "Interstitial")
                }

                override fun onAdShowedFullScreenContent() {
                    onMain {
                        onShowed?.invoke()
                        scheduleFragmentContinueAfterShow()
                    }
                }

                override fun onAdImpression() {
                    onMain {
                        NextGenAdCheck.impression(NextGenAdCheck.INTER, adUnitId)
                        onImpression?.invoke()
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    onMain {
                        AppOpenAdManager.isFullscreenAdShowing = false
                        isShowingAd = false
                        InterstitialAdGate.onInterstitialAdDismissed()
                        // Real dismiss: cleanup only — navigate already via onContinue.
                        HostUiAfterFullscreenRestorer.restore(activity)
                        onClosed?.invoke()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                    onMain {
                        AppOpenAdManager.isFullscreenAdShowing = false
                        isShowingAd = false
                        AdMainThread.cancel(pendingContinue)
                        pendingContinue = null
                        HostUiAfterFullscreenRestorer.restore(activity)
                        NextGenAdCheck.failed(
                            NextGenAdCheck.INTER,
                            adUnitId,
                            error.message,
                            "mode=show",
                        )
                        if (!continueDelivered) {
                            continueDelivered = true
                            onFailedShow?.invoke(error.message)
                        } else {
                            onClosed?.invoke()
                        }
                    }
                }

                override fun onAdClicked() = Unit
            }
            if (!forFragment) {
                deliverContinueOnce()
            }
            ad.show(activity)
        }
    }
}
