package com.aiface.aging.ads_nextgen

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.aiface.aging.AiFaceApp
import com.aiface.aging.shared.ads.AdsHelper

/**
 * Screen-owned native loads (no shared AdsManager cache).
 * Use for load-and-show screens. Paid revenue via [onAdPaid].
 */
object NextGenNativeLoader {

    private const val TAG = "nativeAdFlow"
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            safeRun(block)
        } else {
            mainHandler.post { safeRun(block) }
        }
    }

    private fun safeRun(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.e(TAG, "callback failed", t)
        }
    }

    private fun runWhenSdkReady(onMissing: (() -> Unit)? = null, block: () -> Unit) {
        try {
            val app = AiFaceApp.getTheContext().applicationContext as? AiFaceApp
            val initializer = app?.adsInitializer
            if (initializer == null) {
                Log.e(TAG, "AdsInitializer missing — cannot load native")
                onMain { onMissing?.invoke() }
                return
            }
            initializer.runWhenInitialized {
                safeRun(block)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "runWhenSdkReady failed", t)
            onMain { onMissing?.invoke() }
        }
    }

    fun load(
        adUnitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (!AdsHelper.shouldShowAds()) {
            NextGenAdCheck.skip(NextGenAdCheck.NATIVE, adUnitId, "shouldShowAds=false / pro")
            onMain { onFailed("ads disabled") }
            return
        }
        runWhenSdkReady(onMissing = { onFailed("AdsInitializer missing") }) {
            loadInternal(adUnitId, onLoaded, onFailed)
        }
    }

    private fun loadInternal(
        adUnitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit
    ) {
        try {
            NextGenAdCheck.request(NextGenAdCheck.NATIVE, adUnitId, "mode=helper")
            val request = NativeAdRequest.Builder(
                adUnitId,
                listOf(NativeAd.NativeAdType.NATIVE)
            ).build()

            NativeAdLoader.load(
                request,
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        onMain {
                            try {
                                attachPaidCallback(nativeAd, adUnitId)
                                NextGenAdCheck.loaded(NextGenAdCheck.NATIVE, adUnitId, "mode=helper")
                                onLoaded(nativeAd)
                            } catch (t: Throwable) {
                                Log.e(TAG, "onNativeAdLoaded handler failed", t)
                                try {
                                    nativeAd.destroy()
                                } catch (_: Throwable) {
                                }
                                onFailed(t.message ?: "native bind failed")
                            }
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        onMain {
                            NextGenAdCheck.failed(
                                NextGenAdCheck.NATIVE,
                                adUnitId,
                                adError.message,
                                "mode=helper",
                            )
                            onFailed(adError.message)
                        }
                    }
                }
            )
        } catch (e: IllegalStateException) {
            NextGenAdCheck.failed(
                NextGenAdCheck.NATIVE,
                adUnitId,
                e.message ?: "SDK not initialized",
                "mode=helper",
            )
            onMain { onFailed(e.message ?: "SDK not initialized") }
        } catch (t: Throwable) {
            Log.e(TAG, "loadInternal crashed unit=$adUnitId", t)
            NextGenAdCheck.failed(
                NextGenAdCheck.NATIVE,
                adUnitId,
                t.message ?: "native load crash",
                "mode=helper",
            )
            onMain { onFailed(t.message ?: "native load crash") }
        }
    }

    /**
     * High-floor then normal fallback. Caller owns returned [NativeAd] lifecycle.
     */
    fun loadWithFallback(
        tryHigh: Boolean,
        highUnitId: String,
        normalUnitId: String,
        onLoaded: (ad: NativeAd, adUnitId: String) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!AdsHelper.shouldShowAds()) {
            NextGenAdCheck.skip(NextGenAdCheck.NATIVE, normalUnitId, "shouldShowAds=false / pro")
            onMain { onFailed() }
            return
        }
        runWhenSdkReady(onMissing = onFailed) {
            if (tryHigh) {
                loadInternal(
                    adUnitId = highUnitId,
                    onLoaded = { onLoaded(it, highUnitId) },
                    onFailed = {
                        loadInternal(
                            adUnitId = normalUnitId,
                            onLoaded = { onLoaded(it, normalUnitId) },
                            onFailed = { onFailed() }
                        )
                    }
                )
            } else {
                loadInternal(
                    adUnitId = normalUnitId,
                    onLoaded = { onLoaded(it, normalUnitId) },
                    onFailed = { onFailed() }
                )
            }
        }
    }

    fun attachPaidCallback(ad: NativeAd, adUnitId: String) {
        try {
            ad.adEventCallback = object : NativeAdEventCallback {
                override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                    try {
                        NextGenAdRevenue.track(adUnitId, value, "Native")
                    } catch (t: Throwable) {
                        Log.e(TAG, "onAdPaid track failed", t)
                    }
                }

                override fun onAdImpression() {
                    try {
                        NextGenAdCheck.impression(NextGenAdCheck.NATIVE, adUnitId)
                    } catch (_: Throwable) {
                    }
                }

                override fun onAdClicked() = Unit
                override fun onAdShowedFullScreenContent() = Unit
                override fun onAdDismissedFullScreenContent() = Unit
                override fun onAdFailedToShowFullScreenContent(
                    fullScreenContentError: FullScreenContentError
                ) = Unit
            }
        } catch (t: Throwable) {
            Log.e(TAG, "attachPaidCallback failed", t)
        }
    }
}
