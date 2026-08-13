package com.aiface.aging.shared.ads

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.aiface.aging.ads_nextgen.NextGenRewardedHelper
import com.aiface.aging.utils.GlobalLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Compat type for legacy `RewardItem` call sites. */
data class RewardItem(val amount: Int = 1, val type: String = "")

/**
 * Shared Next-Gen rewarded show used by Home / Edit / SeeAll / IAP.
 */
fun showRewardedNextGen(
    activity: FragmentActivity,
    ad: RewardedAd,
    adUnitId: String,
    onReward: (RewardItem) -> Unit,
    onFailed: () -> Unit,
    onDismissed: () -> Unit,
    onAfterDismissLoad: (() -> Unit)? = null
) {
    activity.lifecycleScope.launch {
        try {
            GlobalLoader.show(activity)
            delay(300)
            NextGenRewardedHelper.show(
                activity = activity,
                ad = ad,
                adUnitId = adUnitId,
                onReward = { onReward(RewardItem()) },
                onShowed = {
                    activity.lifecycleScope.launch {
                        delay(1200)
                        GlobalLoader.hide(activity)
                    }
                },
                onDismissed = {
                    if (rewardedAd === ad) rewardedAd = null
                    MainFullscreenAdsPreloader.onFullscreenAdConsumed()
                    GlobalLoader.hide(activity)
                    onDismissed()
                    onAfterDismissLoad?.invoke()
                },
                onFailedShow = {
                    if (rewardedAd === ad) rewardedAd = null
                    MainFullscreenAdsPreloader.onFullscreenAdConsumed()
                    GlobalLoader.hide(activity)
                    onFailed()
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalLoader.hide(activity)
            onFailed()
        }
    }
}
