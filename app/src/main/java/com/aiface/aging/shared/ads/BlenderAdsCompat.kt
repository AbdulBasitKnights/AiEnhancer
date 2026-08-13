package com.aiface.aging.shared.ads

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity

/** HD-Camera-compatible blender/frames ad helpers. */
fun preloadInterEditSave(context: Context) = Unit

fun showInterEditSave(activity: FragmentActivity, onClose: () -> Unit) {
    activity.showHomeInterstitialThen(forFragment = false, onContinue = onClose)
}

fun loadEditorAdaptiveBanner(
    activity: FragmentActivity,
    bannerContainer: FrameLayout,
    shimmerView: View?,
    clAd: View?,
) {
    bannerContainer.visibility = View.GONE
    shimmerView?.visibility = View.GONE
    clAd?.visibility = View.GONE
}

fun preloadPickerInterstitial(context: Context) = Unit

fun showPickerInterstitial(activity: FragmentActivity, onClose: () -> Unit) {
    onClose()
}

fun reloadEditorBanner(
    adContainer: FrameLayout? = null,
    activity: FragmentActivity? = null,
    shimmerView: View? = null,
    clAd: View? = null,
    bannerAdView: Any? = null,
    normalAdId: String = "",
    highFloorAdId: String = "",
) {
    adContainer?.visibility = View.GONE
    shimmerView?.visibility = View.GONE
    clAd?.visibility = View.GONE
}

/**
 * Blend / FaceSwap signature.
 * Loader → request rewarded → show if available. Does **not** set inter cooldown.
 */
fun showRewardedAd(
    activity: FragmentActivity,
    highFloorId: String,
    normalId: String,
    isHf: Boolean,
    isNormal: Boolean,
    onReward: (RewardItem) -> Unit = {},
    onFailed: () -> Unit = {},
    onDismissed: () -> Unit = {},
) {
    if (!AdsHelper.shouldShowAds()) {
        onReward(RewardItem())
        onDismissed()
        return
    }
    GenerationRewardGate.loadAndShowRewarded(
        activity = activity,
        highFloorId = highFloorId,
        normalId = normalId,
        isHf = isHf,
        isNormal = isNormal,
        onUnlocked = {
            onReward(RewardItem())
            onDismissed()
        },
        onUnavailable = onFailed,
    )
}
