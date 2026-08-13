package com.aiface.aging.shared.ads

import android.content.Intent
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.AiFaceApp
import com.aiface.aging.BuildConfig
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.NextGenRewardedHelper
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.utils.DialogUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Image-generation gate: Pro / Watch-ad dialog.
 * Watch → loading → request rewarded → show if ready.
 * Does **not** touch [InterstitialAdGate] cooldown.
 */
object GenerationRewardGate {

    /**
     * Home / See All / catalog premium templates.
     * Non-pro + premium → rewarded dialog (or direct rewarded) then [onContinue] (auto-click).
     * Pro / free item → [onContinue] immediately.
     */
    fun gateHomePremiumTemplate(
        activity: FragmentActivity,
        isPremiumItem: Boolean,
        onContinue: () -> Unit,
        onOpenIap: (() -> Unit)? = null,
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!isPremiumItem || !AdsHelper.shouldShowAds()) {
            onContinue()
            return
        }

        val rewardEnabled = AiFaceApp.isRewardHome || AiFaceApp.isRewardHomeHf
        if (!rewardEnabled || !AdsHelper.shouldShowAds()) {
            onOpenIap?.invoke() ?: run {
                activity.startActivity(Intent(activity, IAPActivity::class.java))
            }
            return
        }

        val continueAfterReward = {
            isRewarded = true
            onContinue()
        }

        if (AiFaceApp.showRewardDialog) {
            showProOrWatchDialog(
                activity = activity,
                highFloorId = BuildConfig.reward_home_hf,
                normalId = BuildConfig.reward_home,
                isHf = AiFaceApp.isRewardHomeHf,
                isNormal = AiFaceApp.isRewardHome,
                isFromEdit = false,
                onUnlocked = continueAfterReward,
            )
        } else {
            loadAndShowRewarded(
                activity = activity,
                highFloorId = BuildConfig.reward_home_hf,
                normalId = BuildConfig.reward_home,
                isHf = AiFaceApp.isRewardHomeHf,
                isNormal = AiFaceApp.isRewardHome,
                onUnlocked = continueAfterReward,
            )
        }
    }

    fun showProOrWatchDialog(
        activity: FragmentActivity,
        highFloorId: String,
        normalId: String,
        isHf: Boolean,
        isNormal: Boolean,
        isFromEdit: Boolean = true,
        onUnlocked: () -> Unit,
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!AdsHelper.shouldShowAds()) {
            onUnlocked()
            return
        }

        val dialog = DialogUtils.getDialogue(activity, R.layout.dialog_reward)
        FirebaseLogUtils.logEvent("reward_dialog_view", "user view pop-up premium")

        dialog.findViewById<ImageView>(R.id.close_dg)?.setOnClickListener {
            if (!activity.isFinishing) dialog.cancel()
        }

        dialog.findViewById<ConstraintLayout>(R.id.goPremium)?.setOnClickListener {
            FirebaseLogUtils.logEvent(
                "reward_dialog_get_pro_click",
                "user click button get pro on pop-up premium",
            )
            if (!activity.isFinishing) dialog.cancel()
            val intent = Intent(activity, IAPActivity::class.java)
            intent.putExtra("isFromEdit", isFromEdit)
            activity.startActivity(intent)
        }

        dialog.findViewById<ConstraintLayout>(R.id.watch_video)?.setOnClickListener {
            FirebaseLogUtils.logEvent(
                "reward_dialog_watch_ad_click",
                "user click button unlock on pop-up premium",
            )
            if (!activity.isFinishing) dialog.cancel()
            if (!NetworkUtils.isOnline(activity)) {
                Toast.makeText(activity, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loadAndShowRewarded(
                activity = activity,
                highFloorId = highFloorId,
                normalId = normalId,
                isHf = isHf,
                isNormal = isNormal,
                onUnlocked = onUnlocked,
            )
        }

        if (!activity.isFinishing) dialog.show()
    }

    /**
     * Show loader, request rewarded, show while loader visible.
     * Never starts interstitial cooldown.
     */
    fun loadAndShowRewarded(
        activity: FragmentActivity,
        highFloorId: String,
        normalId: String,
        isHf: Boolean,
        isNormal: Boolean,
        onUnlocked: () -> Unit,
        onUnavailable: (() -> Unit)? = null,
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!AdsHelper.shouldShowAds()) {
            onUnlocked()
            return
        }
        if (!NetworkUtils.isOnline(activity)) {
            onUnavailable?.invoke()
            return
        }

        GlobalLoader.show(activity)

        fun presentCachedOrFail() {
            val ad = rewardedAd
            if (ad == null) {
                GlobalLoader.hide(activity)
                Toast.makeText(activity, "Ad not available, please try again", Toast.LENGTH_SHORT)
                    .show()
                onUnavailable?.invoke()
                return
            }
            val unitId = ad.trackedUnitId()
            rewardedAd = null
            activity.lifecycleScope.launch {
                try {
                    delay(200)
                    NextGenRewardedHelper.show(
                        activity = activity,
                        ad = ad,
                        adUnitId = unitId,
                        onReward = { /* unlock on dismiss */ },
                        onShowed = {
                            activity.lifecycleScope.launch {
                                delay(1200)
                                GlobalLoader.hide(activity)
                            }
                        },
                        onDismissed = {
                            // No InterstitialAdGate — rewarded must not reset inter cooldown.
                            if (rewardedAd === ad) rewardedAd = null
                            MainFullscreenAdsPreloader.onFullscreenAdConsumed()
                            GlobalLoader.hide(activity)
                            requestNextRewarded(activity, highFloorId, normalId, isHf, isNormal)
                            onUnlocked()
                        },
                        onFailedShow = {
                            if (rewardedAd === ad) rewardedAd = null
                            MainFullscreenAdsPreloader.onFullscreenAdConsumed()
                            GlobalLoader.hide(activity)
                            Toast.makeText(
                                activity,
                                "Ad not available, please try again",
                                Toast.LENGTH_SHORT,
                            ).show()
                            requestNextRewarded(activity, highFloorId, normalId, isHf, isNormal)
                            onUnavailable?.invoke()
                        },
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    GlobalLoader.hide(activity)
                    onUnavailable?.invoke()
                }
            }
        }

        if (rewardedAd != null) {
            presentCachedOrFail()
            return
        }

        loadRewardedAd(activity, highFloorId, normalId, isHf, isNormal) { loaded ->
            if (activity.isFinishing || activity.isDestroyed) {
                GlobalLoader.hide(activity)
                return@loadRewardedAd
            }
            if (loaded && rewardedAd != null) {
                presentCachedOrFail()
            } else {
                GlobalLoader.hide(activity)
                Toast.makeText(activity, "Ad not available, please try again", Toast.LENGTH_SHORT)
                    .show()
                onUnavailable?.invoke()
            }
        }
    }
}
