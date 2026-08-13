package com.aiface.aging.shared.ads

import android.os.SystemClock
import android.util.Log

/**
 * Blocks interstitial for [cooldownMs] after a fullscreen ad is dismissed
 * (App Open **or** Interstitial).
 *
 * Cooldown length from Remote Config [REMOTE_KEY] (seconds), default
 * [DEFAULT_COOLDOWN_SEC]. Call [resetCooldown] / dismiss helpers to restart the window.
 */
object InterstitialAdGate {

    const val REMOTE_KEY = "inter_cooldown"
    private const val TAG = "InterstitialAdGate"
    private const val DEFAULT_COOLDOWN_SEC = 15L

    @Volatile
    private var cooldownMs: Long = DEFAULT_COOLDOWN_SEC * 1_000L

    @Volatile
    private var lastCooldownStartedAtMs: Long = 0L

    /** Apply remote value (seconds). Missing / ≤0 → default 15s. */
    fun applyRemoteCooldownSeconds(seconds: Long?) {
        val sec = when {
            seconds == null || seconds <= 0L -> DEFAULT_COOLDOWN_SEC
            else -> seconds
        }
        cooldownMs = sec * 1_000L
        Log.d(TAG, "cooldown set to ${sec}s from remote")
    }

    /** Restart cooldown from now (app-open or inter dismiss). */
    fun resetCooldown(reason: String) {
        lastCooldownStartedAtMs = SystemClock.elapsedRealtime()
        Log.d(TAG, "$reason dismissed — inter cooldown ${cooldownMs}ms reset")
    }

    fun onAppOpenAdDismissed() = resetCooldown("app-open")

    fun onInterstitialAdDismissed() = resetCooldown("inter")

    fun shouldSkipInterstitial(): Boolean {
        val startedAt = lastCooldownStartedAtMs
        if (startedAt == 0L) return false
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val skip = elapsed in 0 until cooldownMs
        if (skip) {
            Log.d(TAG, "skip inter — ${cooldownMs - elapsed}ms left in cooldown")
        }
        return skip
    }

    fun canShowInterstitial(): Boolean = !shouldSkipInterstitial()
}
