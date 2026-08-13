package com.aiface.aging.shared.ads

/**
 * Shared interstitial present checks.
 * Ad loading UI must only show when an ad will actually be presented.
 */
fun canPresentHomeInterstitial(): Boolean {
    if (!AdsHelper.shouldShowAds()) return false
    if (interstitialHome == null) return false
    if (InterstitialAdGate.shouldSkipInterstitial()) return false
    return true
}
