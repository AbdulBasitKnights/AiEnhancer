package com.aiface.aging.shared.ads

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.shared.ClickGuard
import com.aiface.aging.utils.GlobalLoader
import kotlinx.coroutines.launch

/**
 * Home interstitial with [InterstitialAdGate] cooldown.
 *
 * ## Activity → Activity ([forFragment]=false, default)
 * [onContinue] runs **before** ad show — start next Activity **in parallel** with the interstitial.
 * Do **not** finish this host inside [onContinue] (that kills the ad).
 * This helper finishes the host after ad dismiss / fail / skip.
 *
 * ## Fragment → Fragment ([forFragment]=true)
 * [onContinue] after ad is on screen. Host not finished.
 *
 * ```
 * showHomeInterstitialThen { openNext() }
 * showHomeInterstitialThen(forFragment = true) { navigate() }
 * ```
 * Java: `HomeInterstitialHelperKt.showHomeInterstitialThen(this, false, () -> { ... });`
 */
fun FragmentActivity.showHomeInterstitialThen(
    forFragment: Boolean = false,
    onContinue: () -> Unit,
) {
    if (isFinishing || isDestroyed) {
        onContinue()
        return
    }
    if (!ClickGuard.tryLock()) return
    lifecycleScope.launch {
        var continued = false

        fun hideLoader() {
            try {
                GlobalLoader.hide(this@showHomeInterstitialThen)
            } catch (_: Exception) {
            }
        }

        fun continueOnce() {
            if (continued) return
            continued = true
            hideLoader()
            ClickGuard.unlock()
            onContinue()
        }

        fun finishHostAfterAdIfNeeded() {
            hideLoader()
            ClickGuard.unlock()
            if (!forFragment && !isFinishing && !isDestroyed) {
                finish()
            }
        }

        try {
            if (!AdsHelper.shouldShowAds()) {
                continueOnce()
                finishHostAfterAdIfNeeded()
                return@launch
            }
            if (!canPresentHomeInterstitial()) {
                continueOnce()
                finishHostAfterAdIfNeeded()
                return@launch
            }

            val ad = interstitialHome ?: run {
                continueOnce()
                finishHostAfterAdIfNeeded()
                return@launch
            }
            interstitialHome = null

            GlobalLoader.show(this@showHomeInterstitialThen)

            ad.showFullscreenAd(
                activity = this@showHomeInterstitialThen,
                forFragment = forFragment,
                onContinue = {
                    hideLoader()
                    continueOnce()
                },
                contentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        hideLoader()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        if (!continued) continueOnce()
                        finishHostAfterAdIfNeeded()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        if (forFragment && !continued) continueOnce()
                        finishHostAfterAdIfNeeded()
                    }
                },
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (!continued) continueOnce()
            finishHostAfterAdIfNeeded()
        }
    }
}
