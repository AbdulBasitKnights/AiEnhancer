package com.aiface.aging.shared.ads

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.aiface.aging.ads_nextgen.AppOpenAdManager

/**
 * Blocks permission / system dialogs while interstitial, rewarded, or app-open
 * is occupying the screen — those dialogs must not appear above the ad.
 */
object FullscreenAdGate {

    private val mainHandler = Handler(Looper.getMainLooper())
    private const val POLL_MS = 300L
    private const val MAX_WAIT_MS = 90_000L

    fun isAdOccludingUi(): Boolean =
        AppOpenAdManager.isFullscreenAdShowing || isShowingAd

    /**
     * Run [block] only after fullscreen ads are gone (poll).
     * Always delays one frame if an ad was showing, so AdActivity can finish teardown.
     */
    fun runWhenAdsClear(block: () -> Unit) {
        if (!isAdOccludingUi()) {
            // Small post so we never race the same frame as ad dismiss callbacks.
            mainHandler.post(block)
            return
        }
        val start = SystemClock.elapsedRealtime()
        val runnable = object : Runnable {
            override fun run() {
                val timedOut = SystemClock.elapsedRealtime() - start >= MAX_WAIT_MS
                if (!isAdOccludingUi() || timedOut) {
                    // Extra beat after flags clear — system permission over AdActivity was racing here.
                    mainHandler.postDelayed(block, 200L)
                } else {
                    mainHandler.postDelayed(this, POLL_MS)
                }
            }
        }
        mainHandler.postDelayed(runnable, POLL_MS)
    }

    /** Set LiveData / request permission only after inter is fully gone. */
    fun requestPermissionWhenClear(setTrue: () -> Unit) {
        runWhenAdsClear(setTrue)
    }
}
