package com.aiface.aging.ads_nextgen

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aiface.aging.AiFaceApp
import com.aiface.aging.AiFaceApp.Companion.isAppOpenResume
import com.aiface.aging.SplashActivity
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.InterstitialAdGate
import kotlin.math.max

/**
 * App-open ads on process resume (background → foreground).
 *
 * - Attach from [NextGenAdsApp].
 * - Call [startPreloadAfterSplash] when splash finishes (enables show + starts preload).
 * - Use companion flags to block show during inter/rewarded or system dialogs/settings.
 */
class AppOpenAdManager(
    private val application: AiFaceApp
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null
    private var isShowingAppOpen = false
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Enable resume shows + start app-open preload (call after splash → main). */
    fun startPreloadAfterSplash() {
        if (!isAppOpenResume) return
        canShowOnResume = true
        startPreload()
    }

    fun startPreload() {
        if (!isAppOpenResume) return
        application.adsInitializer.runWhenInitialized {
            mainHandler.post {
                AdsManager.preloadAppOpen { }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (consumeResumeSuppress("process-onStart")) {
            return
        }
        showAdIfAvailable()
    }

    fun showAdIfAvailable() {
        val unitId = AdConstants.APP_OPEN
        if (!AdsHelper.shouldShowAds()) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "pro / ads disabled")
            return
        }
        if (!canShowOnResume) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "canShowOnResume=false")
            return
        }
        if (disableAppOpen) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "disableAppOpen=true")
            return
        }
        if (isResumeSuppressed()) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "resume suppress window active")
            return
        }
        if (isShowingAppOpen) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "app open already showing")
            return
        }
        if (isFullscreenAdShowing) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "interstitial/rewarded showing")
            return
        }

        val activity = currentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "no valid activity")
            return
        }
        if (activity is SplashActivity) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "splash")
            return
        }
        if (activity is IAPActivity) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "AdActivity")
            return
        }
        if (FullscreenAdInsetsHelper.isFullscreenAdActivity(activity)) {
            NextGenAdCheck.skip(NextGenAdCheck.OPEN_AD, unitId, "AdActivity")
            return
        }

        AdsManager.showAppOpenPreloaded(activity) { state ->
            mainHandler.post {
                when (state) {
                    is AdUiState.Showing -> {
                        isShowingAppOpen = true
                        isFullscreenAdShowing = true
                    }
                    is AdUiState.Idle -> {
                        isShowingAppOpen = false
                        isFullscreenAdShowing = false
                        // Reset inter cooldown window after app-open dismiss.
                        InterstitialAdGate.onAppOpenAdDismissed()
                    }
                    is AdUiState.Error -> {
                        isShowingAppOpen = false
                        isFullscreenAdShowing = false
                        InterstitialAdGate.onAppOpenAdDismissed()
                        NextGenAdCheck.failed(
                            NextGenAdCheck.OPEN_AD,
                            unitId,
                            state.message,
                            "mode=show",
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    // region Activity callbacks

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        // Return from Settings / overlay screens: keep app-open off until UI settles.
        if (pendingSettingsReturn) {
            pendingSettingsReturn = false
            disableAppOpen = true
            suppressForSystemUi()
            mainHandler.postDelayed({
                disableAppOpen = false
                Log.d(TAG, "Settings-return suppress cleared")
            }, SETTINGS_RETURN_CLEAR_MS)
        }
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    // endregion

    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val SETTINGS_RETURN_CLEAR_MS = 1_500L
        private const val SYSTEM_UI_HOLD_MS = 3_000L
        private const val SETTINGS_HOLD_MS = 60_000L

        /**
         * Master switch. Stays false until splash completes and
         * [startPreloadAfterSplash] runs.
         */
        @JvmField
        var canShowOnResume: Boolean = false

        /**
         * Legacy flag used across UI (settings, pickers, etc.).
         * When true, resume app-open is skipped.
         */
        @JvmField
        var disableAppOpen: Boolean = false

        /**
         * Set true before opening permission / system dialogs / pickers that
         * may pause/resume the app.
         */
        @JvmField
        var suppressNextResume: Boolean = false

        /**
         * True while interstitial or rewarded (or this app-open) is on screen.
         * Prevents stacking fullscreen ads.
         */
        @JvmField
        var isFullscreenAdShowing: Boolean = false

        /** Set when leaving for app/settings UI; cleared on next [onActivityResumed]. */
        @JvmField
        var pendingSettingsReturn: Boolean = false

        /** Skip N process-resume shows (dialog then settings return). */
        @Volatile
        private var suppressResumeCount: Int = 0

        /** Soft hold window so intermediate resumes don't burn a one-shot flag. */
        @Volatile
        private var suppressUntilElapsedMs: Long = 0L

        /** Short suppress for permission / in-app system UI. */
        @JvmStatic
        fun suppressForSystemUi() {
            suppressNextResume = true
            suppressResumeCount = max(suppressResumeCount, 1)
            suppressUntilElapsedMs = max(
                suppressUntilElapsedMs,
                SystemClock.elapsedRealtime() + SYSTEM_UI_HOLD_MS
            )
        }

        /**
         * Call before navigating to system Settings (permission details / overlay).
         * Blocks app-open until user returns and UI settles.
         */
        @JvmStatic
        fun suppressForSettings() {
            pendingSettingsReturn = true
            disableAppOpen = true
            suppressNextResume = true
            suppressResumeCount = max(suppressResumeCount, 2)
            suppressUntilElapsedMs = max(
                suppressUntilElapsedMs,
                SystemClock.elapsedRealtime() + SETTINGS_HOLD_MS
            )
            Log.d(TAG, "suppressForSettings armed")
        }

        private fun isResumeSuppressed(): Boolean {
            if (suppressNextResume) return true
            if (suppressResumeCount > 0) return true
            if (SystemClock.elapsedRealtime() < suppressUntilElapsedMs) return true
            if (pendingSettingsReturn) return true
            return false
        }

        /** Consume suppress for this process onStart (or decide to keep duration hold). */
        private fun consumeResumeSuppress(reason: String): Boolean {
            if (pendingSettingsReturn || disableAppOpen) {
                Log.d(TAG, "Skip resume show ($reason: settings/disable)")
                if (suppressNextResume) suppressNextResume = false
                if (suppressResumeCount > 0) suppressResumeCount--
                return true
            }
            if (suppressResumeCount > 0) {
                suppressResumeCount--
                suppressNextResume = false
                Log.d(TAG, "Skip resume show ($reason: count→$suppressResumeCount)")
                return true
            }
            if (suppressNextResume) {
                suppressNextResume = false
                Log.d(TAG, "Skip resume show ($reason: suppressNextResume)")
                return true
            }
            if (SystemClock.elapsedRealtime() < suppressUntilElapsedMs) {
                Log.d(TAG, "Skip resume show ($reason: hold window)")
                return true
            }
            return false
        }
    }
}
