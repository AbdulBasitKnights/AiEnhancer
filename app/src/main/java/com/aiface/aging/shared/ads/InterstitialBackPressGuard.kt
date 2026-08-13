package com.aiface.aging.shared.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.aiface.aging.ads_nextgen.NextGenInterstitialHelper
import java.util.WeakHashMap

/** Compat shim for legacy FullScreenContentCallback call sites. */
data class AdError(val message: String, val code: Int = 0)

open class FullScreenContentCallback {
    open fun onAdShowedFullScreenContent() = Unit
    open fun onAdDismissedFullScreenContent() = Unit
    open fun onAdFailedToShowFullScreenContent(adError: AdError) = Unit
    open fun onAdImpression() = Unit
}

private val interstitialUnitIds = WeakHashMap<InterstitialAd, String>()

fun InterstitialAd.rememberAdUnitId(unitId: String): InterstitialAd {
    interstitialUnitIds[this] = unitId
    return this
}

/** Stored unit id for logging / paid tracking (Next-Gen has no public adUnitId). */
fun InterstitialAd.trackedUnitId(): String = interstitialUnitIds[this].orEmpty()

fun interstitialTrackedUnitId(ad: InterstitialAd?): String = ad?.trackedUnitId().orEmpty()

private val showMainHandler = Handler(Looper.getMainLooper())

/**
 * Show interstitial with Next-Gen callbacks.
 *
 * @param forFragment fragment→fragment: fire [onContinue] after ad shown (+500ms); activity→activity: before show.
 * @param onContinue early navigation (preferred). If null, [FullScreenContentCallback.onAdDismissedFullScreenContent]
 *                   is used as early continue (not real ad dismiss).
 * Real ad dismiss cleans flags only — put loader/nullify cleanup in [onContinue] or callback dismiss.
 */
@JvmOverloads
fun InterstitialAd.showFullscreenAd(
    activity: Activity,
    contentCallback: FullScreenContentCallback? = null,
    forFragment: Boolean = false,
    onContinue: (() -> Unit)? = null,
) {
    if (InterstitialAdGate.shouldSkipInterstitial()) {
        android.util.Log.w("showFullscreenAd", "skip — inter cooldown active")
        com.aiface.aging.shared.ClickGuard.unlock()
        // Still continue user flow (no loader / no wait).
        onContinue?.invoke()
            ?: contentCallback?.onAdDismissedFullScreenContent()
        return
    }
    if (isShowingAd ||
        com.aiface.aging.ads_nextgen.AppOpenAdManager.isFullscreenAdShowing
    ) {
        android.util.Log.w("showFullscreenAd", "skip — already showing fullscreen ad")
        com.aiface.aging.shared.ClickGuard.unlock()
        // Still continue user flow so Next/Save never dead-ends.
        onContinue?.invoke()
            ?: contentCallback?.onAdDismissedFullScreenContent()
        return
    }
    val unitId = trackedUnitId()
    // Capture at show time — call sites may null [interstitialHome] on click before dismiss.
    val wasHomeInter = interstitialHome === this
    var consumed = false
    val markConsumed = markConsumed@{
        // Early continue + real dismiss both call this — run once.
        if (consumed) return@markConsumed
        consumed = true
        if (wasHomeInter) {
            interstitialHome = null
        }
        MainFullscreenAdsPreloader.onFullscreenAdConsumed()
        com.aiface.aging.shared.ClickGuard.unlock()
    }
    NextGenInterstitialHelper.show(
        activity = activity,
        ad = this,
        adUnitId = unitId,
        forFragment = forFragment,
        onShowed = {
            showMainHandler.post {
                // Next single preload only after successful show.
                MainFullscreenAdsPreloader.onInterstitialShownSuccessfully()
                contentCallback?.onAdShowedFullScreenContent()
            }
        },
        onContinue = {
            // Always post — never run nav sync on whatever thread GMA handed us.
            showMainHandler.post {
                markConsumed()
                if (onContinue != null) {
                    onContinue()
                } else {
                    contentCallback?.onAdDismissedFullScreenContent()
                }
            }
        },
        onFailedShow = { msg ->
            showMainHandler.post {
                markConsumed()
                MainFullscreenAdsPreloader.onInterstitialShowFailed()
                contentCallback?.onAdFailedToShowFullScreenContent(AdError(msg))
            }
        },
        onImpression = { showMainHandler.post { contentCallback?.onAdImpression() } },
        onClosed = {
            showMainHandler.post {
                markConsumed()
                // Recover if onShowed never fired (still blocked from next preload).
                MainFullscreenAdsPreloader.ensurePreloadAfterDismiss()
                if (onContinue != null) {
                    contentCallback?.onAdDismissedFullScreenContent()
                }
            }
        },
    )
}
