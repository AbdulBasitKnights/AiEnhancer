package com.aiface.aging.features.look

import androidx.fragment.app.FragmentActivity
import com.aiface.aging.shared.ads.showHomeInterstitialThen

/**
 * Hair / Makeup Next → home interstitial → Preview (same activity, fragment hop).
 * No rewarded — offline look editors are not API image generation.
 */
object LookFeatureAds {
    fun showAdThenNavigate(activity: FragmentActivity, onNavigate: () -> Unit) {
        activity.showHomeInterstitialThen(forFragment = true, onContinue = onNavigate)
    }
}
