package com.aiface.aging.ads_nextgen

import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest

class NativeAdRepository {

    private var normalAd: NativeAd? = null
    private var normalAdUnitId: String? = null
    private var isNormalLoading = false
    private var isPreloadStarted = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    fun loadNormal(onState: (AdUiState) -> Unit, onAdReady: (NativeAd) -> Unit) {
        loadNormal(AdConstants.NATIVE, onState, onAdReady)
    }

    fun loadNormal(adUnitId: String, onState: (AdUiState) -> Unit, onAdReady: (NativeAd) -> Unit) {
        if (normalAd != null) {
            NextGenAdCheck.skip(NextGenAdCheck.NATIVE, adUnitId, "normal cache already loaded")
            onMain {
                onState(AdUiState.Ready(AdFormat.NATIVE, AdLoadMode.NORMAL))
                normalAd?.let { onAdReady(it) }
            }
            return
        }
        if (isNormalLoading) {
            NextGenAdCheck.skip(NextGenAdCheck.NATIVE, adUnitId, "normal load already in progress")
            return
        }

        isNormalLoading = true
        onState(AdUiState.Loading(AdFormat.NATIVE, AdLoadMode.NORMAL))
        NextGenAdCheck.request(NextGenAdCheck.NATIVE, adUnitId, "mode=normal")

        val app = com.aiface.aging.AiFaceApp.getTheContext()
            .applicationContext as? com.aiface.aging.AiFaceApp
        val initializer = app?.adsInitializer
        if (initializer == null) {
            isNormalLoading = false
            NextGenAdCheck.failed(NextGenAdCheck.NATIVE, adUnitId, "SDK not ready", "mode=normal")
            onMain {
                onState(AdUiState.Error(AdFormat.NATIVE, AdLoadMode.NORMAL, "SDK not ready"))
            }
            return
        }

        initializer.runWhenInitialized {
            val request = NativeAdRequest.Builder(
                adUnitId,
                listOf(NativeAd.NativeAdType.NATIVE)
            ).build()

            try {
                NativeAdLoader.load(
                    request,
                    object : NativeAdLoaderCallback {
                        override fun onNativeAdLoaded(nativeAd: NativeAd) {
                            onMain {
                                isNormalLoading = false
                                normalAd = nativeAd
                                normalAdUnitId = adUnitId
                                attachNativeCallbacks(nativeAd, adUnitId)
                                NextGenAdCheck.loaded(NextGenAdCheck.NATIVE, adUnitId, "mode=normal")
                                onState(AdUiState.Ready(AdFormat.NATIVE, AdLoadMode.NORMAL))
                                onAdReady(nativeAd)
                            }
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            onMain {
                                isNormalLoading = false
                                normalAd = null
                                normalAdUnitId = null
                                NextGenAdCheck.failed(
                                    NextGenAdCheck.NATIVE,
                                    adUnitId,
                                    adError.message,
                                    "mode=normal",
                                )
                                onState(
                                    AdUiState.Error(
                                        AdFormat.NATIVE,
                                        AdLoadMode.NORMAL,
                                        adError.message
                                    )
                                )
                            }
                        }
                    }
                )
            } catch (e: IllegalStateException) {
                isNormalLoading = false
                NextGenAdCheck.failed(
                    NextGenAdCheck.NATIVE,
                    adUnitId,
                    e.message ?: "SDK not initialized",
                    "mode=normal",
                )
                onMain {
                    onState(
                        AdUiState.Error(
                            AdFormat.NATIVE,
                            AdLoadMode.NORMAL,
                            e.message ?: "SDK not initialized"
                        )
                    )
                }
            }
        }
    }

    fun startPreload(onState: (AdUiState) -> Unit) {
        val unitId = AdConstants.NATIVE
        if (isPreloadStarted) {
            NextGenAdCheck.skip(
                NextGenAdCheck.NATIVE,
                unitId,
                "preload already active buffer=${getPreloadCount()}",
            )
            onState(AdUiState.Ready(AdFormat.NATIVE, AdLoadMode.PRELOAD, getPreloadCount()))
            return
        }

        onState(AdUiState.Loading(AdFormat.NATIVE, AdLoadMode.PRELOAD))
        NextGenAdCheck.request(NextGenAdCheck.NATIVE, unitId, "mode=preload")
        val request = NativeAdRequest.Builder(
            unitId,
            listOf(NativeAd.NativeAdType.NATIVE)
        ).build()
        val config = AdPreloadConfigFactory.create(request)
        val started = NativeAdPreloader.start(
            AdConstants.PRELOAD_NATIVE,
            config,
            object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    onMain {
                        NextGenAdCheck.loaded(
                            NextGenAdCheck.NATIVE,
                            unitId,
                            "mode=preload buffer=${getPreloadCount()}",
                        )
                        onState(AdUiState.Ready(AdFormat.NATIVE, AdLoadMode.PRELOAD, getPreloadCount()))
                    }
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    onMain {
                        NextGenAdCheck.failed(
                            NextGenAdCheck.NATIVE,
                            unitId,
                            adError.message,
                            "mode=preload",
                        )
                        onState(AdUiState.Error(AdFormat.NATIVE, AdLoadMode.PRELOAD, adError.message))
                    }
                }
            }
        )
        isPreloadStarted = started
        if (!started) {
            NextGenAdCheck.skip(
                NextGenAdCheck.NATIVE,
                unitId,
                "preload start returned false buffer=${getPreloadCount()}",
            )
            onState(AdUiState.Ready(AdFormat.NATIVE, AdLoadMode.PRELOAD, getPreloadCount()))
        }
    }

    fun pollPreloaded(onState: (AdUiState) -> Unit, onAdReady: (NativeAd) -> Unit) {
        val result = NativeAdPreloader.pollAd(AdConstants.PRELOAD_NATIVE)
        val nativeAd = (result as? NativeAdLoadResult.NativeAdSuccess)?.ad
        if (nativeAd == null) {
            onState(AdUiState.Error(AdFormat.NATIVE, AdLoadMode.PRELOAD, "Buffer empty. Preload first."))
            return
        }
        attachNativeCallbacks(nativeAd, AdConstants.NATIVE)
        onMain {
            onState(AdUiState.Ready(AdFormat.NATIVE, AdLoadMode.PRELOAD, getPreloadCount()))
            onAdReady(nativeAd)
        }
    }

    fun getPreloadCount(): Int =
        NativeAdPreloader.getNumAdsAvailable(AdConstants.PRELOAD_NATIVE)

    fun isPreloadActive(): Boolean = isPreloadStarted

    fun getLastAdUnitId(): String? = normalAdUnitId

    private fun attachNativeCallbacks(ad: NativeAd, adUnitId: String) {
        ad.adEventCallback = object : NativeAdEventCallback {
            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                NextGenAdRevenue.track(adUnitId, value, "Native")
            }

            override fun onAdImpression() {
                NextGenAdCheck.impression(NextGenAdCheck.NATIVE, adUnitId)
            }
            override fun onAdClicked() = Unit
            override fun onAdShowedFullScreenContent() = Unit
            override fun onAdDismissedFullScreenContent() = Unit
            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) = Unit
        }
    }

    fun destroyPreload() {
        NativeAdPreloader.destroy(AdConstants.PRELOAD_NATIVE)
        isPreloadStarted = false
    }

    fun clearNormalCache() {
        normalAd?.destroy()
        normalAd = null
        normalAdUnitId = null
    }

    fun hasNormalCache(): Boolean = normalAd != null

    fun getNormalAd(): NativeAd? = normalAd
}
