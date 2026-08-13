package com.aiface.aging.shared

import android.os.SystemClock
import android.util.Log
import com.aiface.aging.ads_nextgen.AppOpenAdManager
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.utils.GlobalLoader

/**
 * App-wide debounce + in-flight lock for monkey taps (multi ad / multi navigate).
 */
object ClickGuard {
    private const val TAG = "ClickGuard"
    const val DEFAULT_INTERVAL_MS = 900L

    private val lock = Any()

    @Volatile
    private var lastAcceptedElapsedMs: Long = 0L

    @Volatile
    private var locked: Boolean = false

    fun isBlocked(): Boolean {
        return locked ||
            isShowingAd ||
            AppOpenAdManager.isFullscreenAdShowing ||
            GlobalLoader.isLoaderShowing
    }

    /** Soft debounce for UI that does not start an ad lock. */
    @JvmStatic
    fun tryClick(intervalMs: Long = DEFAULT_INTERVAL_MS): Boolean {
        synchronized(lock) {
            if (isBlocked()) {
                Log.d(TAG, "click blocked (busy/ad/loader)")
                return false
            }
            val now = SystemClock.elapsedRealtime()
            if (now - lastAcceptedElapsedMs < intervalMs) {
                Log.d(TAG, "click blocked (debounce)")
                return false
            }
            lastAcceptedElapsedMs = now
            return true
        }
    }

    /**
     * Lock for ad/nav flow covering loader delay before [isShowingAd] flips.
     * Always pair with [unlock] on every exit path.
     */
    @JvmStatic
    fun tryLock(): Boolean {
        synchronized(lock) {
            if (locked ||
                isShowingAd ||
                AppOpenAdManager.isFullscreenAdShowing ||
                GlobalLoader.isLoaderShowing
            ) {
                Log.d(TAG, "lock blocked (busy/ad/loader)")
                return false
            }
            locked = true
            lastAcceptedElapsedMs = SystemClock.elapsedRealtime()
            return true
        }
    }

    @JvmStatic
    fun unlock() {
        synchronized(lock) {
            locked = false
        }
    }
}
