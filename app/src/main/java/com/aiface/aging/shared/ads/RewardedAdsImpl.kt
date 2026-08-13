package com.aiface.aging.shared.ads

import androidx.fragment.app.FragmentActivity
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.aiface.aging.ads_nextgen.AdsManager
import com.aiface.aging.ads_nextgen.NextGenRewardedHelper
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.NetworkUtils
import java.util.WeakHashMap

var rewardedAd: RewardedAd? = null

private var isRewardLoading = false
var isRewarded = false

private val rewardedUnitIds = WeakHashMap<RewardedAd, String>()
private val pendingRewardLoaded = mutableListOf<(Boolean) -> Unit>()

fun RewardedAd.rememberAdUnitId(unitId: String): RewardedAd {
    rewardedUnitIds[this] = unitId
    return this
}

fun RewardedAd.trackedUnitId(): String = rewardedUnitIds[this].orEmpty()

fun loadRewardedAd(
    activity: FragmentActivity,
    highFloorId: String,
    normalId: String,
    isHf: Boolean,
    isNormal: Boolean,
    onLoaded: ((Boolean) -> Unit),
) {
    if (!AdsHelper.shouldShowAds() || !NetworkUtils.isOnline(activity)) {
        onLoaded.invoke(false)
        return
    }
    if (rewardedAd != null) {
        onLoaded.invoke(true)
        return
    }
    if (isRewardLoading) {
        LogUtils.printLog("reward skip", "already loading — queue callback")
        pendingRewardLoaded.add(onLoaded)
        return
    }

    MainFullscreenAdsPreloader.takeRewarded()?.let { ad ->
        rewardedAd = ad
        isRewardLoading = false
        onLoaded.invoke(true)
        LogUtils.printLog("reward from preload", AdsManager.rewardedPreloadUnitId())
        return
    }

    isRewardLoading = true
    val tryHigh = isHf && isNormal
    if (!tryHigh && !isNormal) {
        isRewardLoading = false
        onLoaded.invoke(false)
        return
    }

    NextGenRewardedHelper.loadWithFallback(
        tryHigh = tryHigh,
        highUnitId = highFloorId,
        normalUnitId = normalId,
        onLoaded = { ad, unitId ->
            rewardedAd = ad.rememberAdUnitId(unitId)
            isRewardLoading = false
            onLoaded.invoke(true)
            val waiters = pendingRewardLoaded.toList()
            pendingRewardLoaded.clear()
            waiters.forEach { it.invoke(true) }
            LogUtils.printLog("reward loaded", unitId)
        },
        onFailed = {
            isRewardLoading = false
            onLoaded.invoke(false)
            val waiters = pendingRewardLoaded.toList()
            pendingRewardLoaded.clear()
            waiters.forEach { it.invoke(false) }
            LogUtils.printLog("reward failed to load", normalId)
        }
    )
}

/**
 * Request next rewarded after dismiss. No-op if cached or already loading.
 */
fun requestNextRewarded(
    activity: FragmentActivity,
    highFloorId: String,
    normalId: String,
    isHf: Boolean,
    isNormal: Boolean,
) {
    if (rewardedAd != null) {
        LogUtils.printLog("reward skip", "already cached")
        return
    }
    if (isRewardLoading) {
        LogUtils.printLog("reward skip", "already loading")
        return
    }
    loadRewardedAd(activity, highFloorId, normalId, isHf, isNormal) {}
}

fun clearRewardedAfterShow() {
    rewardedAd = null
    MainFullscreenAdsPreloader.onFullscreenAdConsumed()
}
