package com.aiface.aging.ads_nextgen

import android.util.Log

/**
 * Unified ad diagnostics.
 *
 * Tag: [TAG]
 * Levels: request=W, loaded=D, failed=E, skip=D, impression=D
 * Names: inter | reward | banner | openad | native
 * Screen: Splash | Home+
 */
object NextGenAdCheck {

    const val TAG = "NextGenAdCheck"

    const val INTER = "inter"
    const val REWARD = "reward"
    const val BANNER = "banner"
    const val OPEN_AD = "openad"
    const val NATIVE = "native"

    const val SCREEN_SPLASH = "Splash"
    const val SCREEN_HOME = "Home+"

    @Volatile
    var screen: String = SCREEN_HOME
        private set

    fun setScreen(screen: String) {
        this.screen = screen
        Log.d(TAG, "screen → $screen")
    }

    fun request(name: String, unitId: String, extra: String = "") {
        Log.w(TAG, format(name, "request", unitId, extra))
    }

    fun loaded(name: String, unitId: String, extra: String = "") {
        Log.d(TAG, format(name, "loaded", unitId, extra))
    }

    fun failed(name: String, unitId: String, reason: String, extra: String = "") {
        val detail = if (extra.isBlank()) reason else "$reason | $extra"
        Log.e(TAG, format(name, "failed", unitId, detail))
    }

    fun skip(name: String, unitId: String, reason: String = "already loaded / available to show") {
        Log.d(TAG, format(name, "skip request", unitId, reason))
    }

    fun impression(name: String, unitId: String, extra: String = "") {
        Log.d(TAG, format(name, "impression", unitId, extra))
    }

    private fun format(name: String, event: String, unitId: String, extra: String): String {
        val base = "$name $event screen=$screen unit=$unitId"
        return if (extra.isBlank()) base else "$base $extra"
    }
}
