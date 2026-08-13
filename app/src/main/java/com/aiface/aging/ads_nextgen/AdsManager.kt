package com.aiface.aging.ads_nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Facade over all ad repositories. Single entry point for UI layer.
 */
object AdsManager {

    private val interstitial = InterstitialAdRepository()
    private val rewarded = RewardedAdRepository()
    private val appOpen = AppOpenAdRepository()
    private val banner = BannerAdRepository()
    private val native = NativeAdRepository()

    private val _preloadBufferStatus = MutableStateFlow(readPreloadBufferStatus())
    val preloadBufferStatus: StateFlow<List<PreloadBufferStatus>> = _preloadBufferStatus.asStateFlow()

    /** Refresh shared buffer counts so Hub + Show stay in sync in real time. */
    fun refreshPreloadBufferStatus() {
        _preloadBufferStatus.value = readPreloadBufferStatus()
    }

    private fun readPreloadBufferStatus(): List<PreloadBufferStatus> = listOf(
        PreloadBufferStatus(
            AdFormat.INTERSTITIAL,
            interstitial.getPreloadCount(),
            interstitial.isPreloadActive()
        ),
        PreloadBufferStatus(
            AdFormat.REWARDED,
            rewarded.getPreloadCount(),
            rewarded.isPreloadActive()
        ),
        PreloadBufferStatus(
            AdFormat.APP_OPEN,
            appOpen.getPreloadCount(),
            appOpen.isPreloadActive()
        ),
        PreloadBufferStatus(AdFormat.BANNER, banner.getPreloadCount(), banner.isPreloadActive()),
        PreloadBufferStatus(AdFormat.NATIVE, native.getPreloadCount(), native.isPreloadActive()),
    )

    private fun withBufferRefresh(onState: (AdUiState) -> Unit): (AdUiState) -> Unit = { state ->
        refreshPreloadBufferStatus()
        onState(state)
    }

    // region Normal load (single ad API)

    fun loadInterstitialNormal(onState: (AdUiState) -> Unit) = interstitial.loadNormal(onState)

    fun loadInterstitialNormal(adUnitId: String, onState: (AdUiState) -> Unit) =
        interstitial.loadNormal(adUnitId, onState)

    fun loadRewardedNormal(onState: (AdUiState) -> Unit) = rewarded.loadNormal(onState)
    fun loadAppOpenNormal(onState: (AdUiState) -> Unit) = appOpen.loadNormal(onState)
    fun loadBannerNormal(activity: Activity, adView: AdView, onState: (AdUiState) -> Unit) =
        banner.loadNormal(activity, adView, onState)

    fun loadBannerNormal(
        activity: Activity,
        adView: AdView,
        adUnitId: String,
        onState: (AdUiState) -> Unit
    ) = banner.loadNormal(activity, adView, adUnitId, onState)

    fun loadNativeNormal(onState: (AdUiState) -> Unit, onAdReady: (NativeAd) -> Unit) =
        native.loadNormal(onState, onAdReady)

    fun loadNativeNormal(
        adUnitId: String,
        onState: (AdUiState) -> Unit,
        onAdReady: (NativeAd) -> Unit
    ) = native.loadNormal(adUnitId, onState, onAdReady)

    // endregion

    // region Preload API

    fun preloadInterstitial(onState: (AdUiState) -> Unit) =
        interstitial.startPreload(withBufferRefresh(onState))

    fun preloadInterstitial(adUnitId: String, onState: (AdUiState) -> Unit) =
        interstitial.startPreload(adUnitId, withBufferRefresh(onState))

    fun preloadRewarded(onState: (AdUiState) -> Unit) =
        rewarded.startPreload(withBufferRefresh(onState))

    fun preloadRewarded(adUnitId: String, onState: (AdUiState) -> Unit) =
        rewarded.startPreload(adUnitId, withBufferRefresh(onState))

    fun pollInterstitialPreloaded(): com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd? {
        val ad = interstitial.pollPreloadedAd()
        refreshPreloadBufferStatus()
        return ad
    }

    fun pollRewardedPreloaded(): com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd? {
        val ad = rewarded.pollPreloadedAd()
        refreshPreloadBufferStatus()
        return ad
    }

    fun interstitialPreloadUnitId(): String = interstitial.getPreloadAdUnitId()

    fun rewardedPreloadUnitId(): String = rewarded.getPreloadAdUnitId()

    fun isInterstitialPreloadActive(): Boolean = interstitial.isPreloadActive()

    fun isRewardedPreloadActive(): Boolean = rewarded.isPreloadActive()

    fun preloadAppOpen(onState: (AdUiState) -> Unit) =
        appOpen.startPreload(withBufferRefresh(onState))

    fun preloadBanner(activity: Activity, onState: (AdUiState) -> Unit) =
        banner.startPreload(activity, withBufferRefresh(onState))

    fun preloadNative(onState: (AdUiState) -> Unit) =
        native.startPreload(withBufferRefresh(onState))

    // endregion

    // region Show

    fun showInterstitialNormal(
        activity: Activity,
        forFragment: Boolean = false,
        onState: (AdUiState) -> Unit,
    ) = interstitial.showNormal(activity, forFragment, onState)

    fun showInterstitialPreloaded(
        activity: Activity,
        forFragment: Boolean = false,
        onState: (AdUiState) -> Unit,
    ) = interstitial.showPreloaded(activity, forFragment, withBufferRefresh(onState))

    fun showRewardedNormal(activity: Activity, onState: (AdUiState) -> Unit, onReward: () -> Unit) =
        rewarded.showNormal(activity, onState, onReward)

    fun showRewardedPreloaded(activity: Activity, onState: (AdUiState) -> Unit, onReward: () -> Unit) =
        rewarded.showPreloaded(activity, withBufferRefresh(onState), onReward)

    fun showAppOpenNormal(activity: Activity, onState: (AdUiState) -> Unit) =
        appOpen.showNormal(activity, onState)

    fun showAppOpenPreloaded(activity: Activity, onState: (AdUiState) -> Unit) =
        appOpen.showPreloaded(activity, withBufferRefresh(onState))

    fun showBannerPreloaded(activity: Activity, adView: AdView, onState: (AdUiState) -> Unit) =
        banner.showPreloaded(activity, adView, withBufferRefresh(onState))

    fun pollNativePreloaded(onState: (AdUiState) -> Unit, onAdReady: (NativeAd) -> Unit) =
        native.pollPreloaded(withBufferRefresh(onState), onAdReady)

    // endregion

    // region Cache status

    fun getPreloadBufferStatus(): List<PreloadBufferStatus> = preloadBufferStatus.value

    fun getNormalCacheSummary(): String = buildString {
        appendLine("Normal cache:")
        appendLine("• Interstitial: ${if (interstitial.hasNormalCache()) "held" else "empty"}")
        appendLine("• Rewarded: ${if (rewarded.hasNormalCache()) "held" else "empty"}")
        appendLine("• App Open: ${if (appOpen.hasNormalCache()) "held" else "empty"}")
        appendLine("• Banner: ${if (banner.hasNormalCache()) "held" else "empty"}")
        appendLine("• Native: ${if (native.hasNormalCache()) "held" else "empty"}")
    }

    fun clearAllNormalCache() {
        interstitial.clearNormalCache()
        rewarded.clearNormalCache()
        appOpen.clearNormalCache()
        banner.clearNormalCache()
        native.clearNormalCache()
    }

    fun destroyDisplayedNativeAd() {
        native.clearNormalCache()
    }

    fun destroyInterstitialPreload() {
        interstitial.destroyPreload()
        refreshPreloadBufferStatus()
    }

    fun destroyAllPreloads() {
        interstitial.destroyPreload()
        rewarded.destroyPreload()
        appOpen.destroyPreload()
        banner.destroyPreload()
        native.destroyPreload()
        refreshPreloadBufferStatus()
    }

    fun hasInterstitialCache(): Boolean = interstitial.hasNormalCache()
    fun hasBannerCache(): Boolean = banner.hasNormalCache()
    fun hasNativeCache(): Boolean = native.hasNormalCache()

    fun getNormalNativeAd(): NativeAd? = native.getNormalAd()

    fun getLastNativeAdUnitId(): String? = native.getLastAdUnitId()
}
