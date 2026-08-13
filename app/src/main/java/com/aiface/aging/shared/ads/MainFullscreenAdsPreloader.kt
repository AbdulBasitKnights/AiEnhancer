package com.aiface.aging.shared.ads

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.ads_nextgen.AdUiState
import com.aiface.aging.ads_nextgen.AdsManager
import com.aiface.aging.ads_nextgen.AppOpenAdManager
import com.aiface.aging.ads_nextgen.NextGenAdCheck
import com.aiface.aging.shared.ads.AdsHelper.isProVersion

/**
 * Preloads interstitial + rewarded from [MainActivity] onwards.
 * Buffer size 1 ([AdPreloadConfigFactory]) — only one ready inter at a time.
 * After taking into [interstitialHome], preload stops until ad is successfully shown.
 */
object MainFullscreenAdsPreloader {

    private const val TAG = "MainAdsPreload"
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var started = false

    @Volatile
    private var awaitingShowBeforeReload = false

    private var slotFillRunnable: Runnable? = null

    fun startFromMainWhenAdsClear() {
        // Splash early-continues to Main while inter still up — wait for impression/dismiss.
        FullscreenAdGate.runWhenAdsClear {
            startFromMain()
        }
    }

    fun startFromMain() {
        if (!AdsHelper.shouldShowAds()) {
            NextGenAdCheck.skip(NextGenAdCheck.INTER, "-", "shouldShowAds=false")
            return
        }
        if (isProVersion.value == true) {
            NextGenAdCheck.skip(NextGenAdCheck.INTER, "-", "pro user")
            return
        }
        if (AppOpenAdManager.isFullscreenAdShowing || isShowingAd) {
            NextGenAdCheck.skip(
                NextGenAdCheck.INTER,
                resolveInterUnitId().orEmpty().ifBlank { "-" },
                "fullscreen still up — defer until ads clear",
            )
            startFromMainWhenAdsClear()
            return
        }
        // Hold only one: do not refill preload while home slot already has an ad.
        if (interstitialHome != null || awaitingShowBeforeReload) {
            NextGenAdCheck.skip(
                NextGenAdCheck.INTER,
                resolveInterUnitId().orEmpty().ifBlank { "-" },
                "home slot held or waiting for show",
            )
            return
        }
        if (started && AdsManager.isInterstitialPreloadActive()) {
            NextGenAdCheck.skip(
                NextGenAdCheck.INTER,
                resolveInterUnitId().orEmpty().ifBlank { "-" },
                "preload already active — backup=1",
            )
            scheduleHomeSlotFillFromPreload()
            return
        }

        val interUnit = resolveInterUnitId()
        val rewardUnit = resolveRewardUnitId()
        if (interUnit == null && rewardUnit == null) {
            NextGenAdCheck.skip(NextGenAdCheck.INTER, "-", "both formats disabled by RC")
            return
        }

        started = true
        interUnit?.let { unitId ->
            AdsManager.preloadInterstitial(unitId) { state ->
                if (state is AdUiState.Ready) {
                    refillHomeInterFromPreload()
                }
            }
        }
        rewardUnit?.let { unitId ->
            AdsManager.preloadRewarded(unitId) { }
        }
        Log.d(TAG, "started inter=$interUnit reward=$rewardUnit buffer=1")
    }

    /** Prefer an already-preloaded inter; stops preload so a 2nd ad is not buffered. */
    fun takeInterstitial(): InterstitialAd? {
        if (!AdsHelper.shouldShowAds() || isProVersion.value == true) return null
        ensureStarted()
        val ad = AdsManager.pollInterstitialPreloaded() ?: return null
        val unitId = AdsManager.interstitialPreloadUnitId()
        // One-at-a-time: destroy buffer so SDK cannot refill a second ready ad.
        AdsManager.destroyInterstitialPreload()
        started = false
        awaitingShowBeforeReload = true
        return ad.rememberAdUnitId(unitId)
    }

    fun takeRewarded(): RewardedAd? {
        if (!AdsHelper.shouldShowAds() || isProVersion.value == true) return null
        ensureStarted()
        val ad = AdsManager.pollRewardedPreloaded() ?: return null
        val unitId = AdsManager.rewardedPreloadUnitId()
        return ad.rememberAdUnitId(unitId)
    }

    /**
     * After inter consumed from slot — do not restart preload here.
     * Next preload starts only after [onInterstitialShownSuccessfully].
     */
    fun onFullscreenAdConsumed() {
        if (!AdsHelper.shouldShowAds() || isProVersion.value == true) return
        scheduleHomeSlotFillFromPreload()
    }

    /** Call when interstitial actually showed — start next single preload. */
    fun onInterstitialShownSuccessfully() {
        if (!AdsHelper.shouldShowAds() || isProVersion.value == true) return
        awaitingShowBeforeReload = false
        started = true
        val interUnit = resolveInterUnitId()
        val rewardUnit = resolveRewardUnitId()
        // Force preload of NEXT ad even if home still references the showing one.
        interUnit?.let { unitId ->
            if (!AdsManager.isInterstitialPreloadActive()) {
                AdsManager.preloadInterstitial(unitId) { state ->
                    if (state is AdUiState.Ready && interstitialHome == null) {
                        refillHomeInterFromPreload()
                    }
                }
            } else {
                NextGenAdCheck.skip(
                    NextGenAdCheck.INTER,
                    unitId,
                    "preload already active after show",
                )
            }
        }
        rewardUnit?.let { unitId ->
            if (!AdsManager.isRewardedPreloadActive()) {
                AdsManager.preloadRewarded(unitId) { }
            } else {
                NextGenAdCheck.skip(
                    NextGenAdCheck.REWARD,
                    unitId,
                    "preload already active after show",
                )
            }
        }
        scheduleHomeSlotFillFromPreload()
        Log.d(TAG, "preload restarted after successful show")
    }

    /** If show failed, allow preload to start again. */
    fun onInterstitialShowFailed() {
        awaitingShowBeforeReload = false
        started = false
        startFromMain()
        scheduleHomeSlotFillFromPreload()
    }

    /** After dismiss: unblock stuck awaiting flag / restart if preload dead. */
    fun ensurePreloadAfterDismiss() {
        if (!AdsHelper.shouldShowAds() || isProVersion.value == true) return
        if (awaitingShowBeforeReload) {
            onInterstitialShowFailed()
            return
        }
        if (interstitialHome == null && !AdsManager.isInterstitialPreloadActive()) {
            started = false
            startFromMain()
        }
    }

    /**
     * Poll-only retries into [interstitialHome]. No NextGenInterstitialHelper.load.
     * Covers the gap while SDK refills the buffer after poll.
     */
    fun scheduleHomeSlotFillFromPreload() {
        cancelSlotFill()
        if (awaitingShowBeforeReload) return
        val delaysMs = longArrayOf(0L, 500L, 1500L, 4000L, 8000L)
        var index = 0
        val runnable = object : Runnable {
            override fun run() {
                if (interstitialHome != null ||
                    awaitingShowBeforeReload ||
                    !AdsHelper.shouldShowAds() ||
                    isProVersion.value == true
                ) {
                    slotFillRunnable = null
                    return
                }
                refillHomeInterFromPreload()
                if (interstitialHome != null) {
                    Log.d(TAG, "home slot filled from preload")
                    slotFillRunnable = null
                    return
                }
                index++
                if (index < delaysMs.size) {
                    val wait = delaysMs[index] - delaysMs[index - 1]
                    mainHandler.postDelayed(this, wait.coerceAtLeast(0L))
                } else {
                    slotFillRunnable = null
                    Log.d(TAG, "home slot still empty after poll retries (waiting SDK preload Ready)")
                }
            }
        }
        slotFillRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun cancelSlotFill() {
        slotFillRunnable?.let { mainHandler.removeCallbacks(it) }
        slotFillRunnable = null
    }

    private fun ensureStarted() {
        if (awaitingShowBeforeReload || interstitialHome != null) return
        if (!started ||
            (!AdsManager.isInterstitialPreloadActive() && resolveInterUnitId() != null) ||
            (!AdsManager.isRewardedPreloadActive() && resolveRewardUnitId() != null)
        ) {
            started = false
            startFromMain()
        }
    }

    private fun resolveInterUnitId(): String? {
        if (!AiFaceApp.isInterHome && !AiFaceApp.isInterHomeHf) return null
        return if (AiFaceApp.isInterHomeHf) {
            BuildConfig.inter_home_high
        } else {
            BuildConfig.inter_home
        }
    }

    private fun resolveRewardUnitId(): String? {
        if (!AiFaceApp.isRewardHome && !AiFaceApp.isRewardHomeHf) return null
        return if (AiFaceApp.isRewardHomeHf) {
            BuildConfig.reward_home_hf
        } else {
            BuildConfig.reward_home
        }
    }
}
