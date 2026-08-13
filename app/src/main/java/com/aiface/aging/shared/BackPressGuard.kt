package com.aiface.aging.shared

import android.os.SystemClock
import android.util.Log

/**
 * Debounces consecutive/system back presses to avoid double pop / crash.
 */
object BackPressGuard {
    private const val TAG = "BackPressGuard"
    const val DEFAULT_INTERVAL_MS = 750L

    private val lock = Any()

    @Volatile
    private var lastAcceptedElapsedMs: Long = 0L

    @Volatile
    private var inFlight: Boolean = false

    /** Soft debounce — use for instantaneous back handlers. */
    @JvmStatic
    fun tryHandle(intervalMs: Long = DEFAULT_INTERVAL_MS): Boolean {
        synchronized(lock) {
            if (inFlight) {
                Log.d(TAG, "back blocked (inFlight)")
                return false
            }
            val now = SystemClock.elapsedRealtime()
            if (now - lastAcceptedElapsedMs < intervalMs) {
                Log.d(TAG, "back blocked (debounce)")
                return false
            }
            lastAcceptedElapsedMs = now
            return true
        }
    }

    /** Lock until [end] — use when back starts async hide animation / dialog. */
    @JvmStatic
    fun begin(intervalMs: Long = DEFAULT_INTERVAL_MS): Boolean {
        synchronized(lock) {
            if (inFlight) {
                Log.d(TAG, "begin blocked (inFlight)")
                return false
            }
            val now = SystemClock.elapsedRealtime()
            if (now - lastAcceptedElapsedMs < intervalMs) {
                Log.d(TAG, "begin blocked (debounce)")
                return false
            }
            lastAcceptedElapsedMs = now
            inFlight = true
            return true
        }
    }

    @JvmStatic
    fun end() {
        synchronized(lock) {
            inFlight = false
        }
    }
}
