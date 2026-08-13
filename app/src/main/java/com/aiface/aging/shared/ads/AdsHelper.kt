package com.aiface.aging.shared.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustEvent
import com.aiface.aging.AiFaceApp.Companion.fromHome
import com.facebook.appevents.AppEventsLogger
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdMainThread
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.stopShimmerOnMain
import com.aiface.aging.ads_nextgen.NextGenBannerHelper
import com.aiface.aging.ads_nextgen.NextGenInterstitialHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.utils.AdjustConstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object AdsHelper {
    /**
     * Temporary: treat every user as pro so no ads load/show.
     * Flip to false when real IAP/ads should return.
     */
    const val FORCE_PRO_NO_ADS = false

    //InApp Purchases
    var isProVersion = MutableLiveData<Boolean?>()

    const val SUBSCRIPTION_CHECK_TIMEOUT_MS = 6_500L

    /** Ads allowed only when user is explicitly pro-subscriber. */
    fun shouldShowAds(): Boolean {
        return isProVersion.value != true
    }

    fun updateProVersion(isPro: Boolean) {
        isProVersion.postValue(/*if (FORCE_PRO_NO_ADS) true else */isPro)
    }

    private var loadingDialog: Dialog? = null

    //===================Splash=================//
    private const val TAG = "SplashInterstitial"


    //===================Language=================//

    var langInterstitialHigh: InterstitialAd? = null
    var langInterstitialAll: InterstitialAd? = null
    var langNativeAd1: NativeAd? = null
    var langNativeAdHigh1: NativeAd? = null
    var langNativeAd2: NativeAd? = null
    var langNativeAdHigh2: NativeAd? = null

    var languageButtonDelay: Int = 3
    var languageButtonStyle: Int = 1
    var langSessionRemote = 3
    var langInterstitialEnabled: Boolean = true
    var langInterstitialHighEnabled: Boolean = true
    var langNative1Enabled: Boolean = true
    var langNativeHigh1Enabled: Boolean = true

    var langNative2Enabled: Boolean = true
    var langNativeHigh2Enabled: Boolean = true
    var langNativeFormat: Int = 2
    var langCtaColor: String = "#8A38F5"
    var langCtaTextColor: String = "#FFFFFF"
    var langCtaTextStyle: String = "bold"

    //===================Onboarding=================//
    var obNativeAd1: NativeAd? = null
    var obNativeAdHigh1: NativeAd? = null

    var obNativeAd3: NativeAd? = null
    var obNativeAdHigh3: NativeAd? = null

    var obNativeAd4: NativeAd? = null
    var obNativeAdHigh4: NativeAd? = null

    var obNativeAdFullScr1: NativeAd? = null
    var obNativeAdHighFullScr1: NativeAd? = null

    var obNativeAdFullScr2: NativeAd? = null
    var obNativeAdHighFullScr2: NativeAd? = null


    var obEnable: Boolean = true


    var obInterstitialEnabled: Boolean = true
    var obInterstitialHighEnabled: Boolean = true

    var obFirstEnable: Boolean = true
    var obSecondEnable: Boolean = true
    var obThirdEnable: Boolean = true
    var obFourthEnable: Boolean = true
    var obFifthEnable: Boolean = true

    var obNative1Enabled: Boolean = true
    var obNative3Enabled: Boolean = true
    var obNative4Enabled: Boolean = true

    var obNativeHigh1Enabled: Boolean = true
    var obNativeHigh3Enabled: Boolean = true
    var obNativeHigh4Enabled: Boolean = true

    var obNativeHighFullScr1Enabled: Boolean = false
    var obNativeHighFullScr2Enabled: Boolean = false
    var obNativeFullScr1Enabled: Boolean = false
    var obNativeFullScr2Enabled: Boolean = false

    var featureNative1Enabled: Boolean = true
    var featureNativeHigh1Enabled: Boolean = true

    var obNativeFormat: Int = 2
    var obCtaColor: String = "#8A38F5"
    var obCtaTextColor: String = "#FFFFFF"
    var obCtaTextStyle: String = "bold"

    var isShowNativeFullCross = true
    var nativeFullCrossDelay = 3


    private val _obFull1Loaded = MutableLiveData(false)
    val obFull1Loaded: LiveData<Boolean> get() = _obFull1Loaded

    private val _obFull2Loaded = MutableLiveData(false)
    val obFull2Loaded: LiveData<Boolean> get() = _obFull2Loaded


    fun obFull1Ready() {
        if (_obFull1Loaded.value != true) _obFull1Loaded.postValue(true)
    }

    fun obFull2Ready() {
        if (_obFull2Loaded.value != true) _obFull2Loaded.postValue(true)
    }


    fun loadWithFallback(
        activity: Activity,
        highFloorAdId: String,
        normalAdId: String,
        showHighfloor: Boolean = true,
        showNormalfloor: Boolean = true,
        onAdLoadedHigh: (nativeAd: NativeAd) -> Unit,
        onAdLoadedNormal: (nativeAd: NativeAd) -> Unit,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (!shouldShowAds()) {
            onAdFailed?.invoke()
            return
        }
        try {
            if (showHighfloor) {
                NextGenNativeLoader.load(
                    adUnitId = highFloorAdId,
                    onLoaded = onAdLoadedHigh,
                    onFailed = {
                        Log.w("nativeAdFlow", "Ad failed ❌ Requesting Normal $highFloorAdId")
                        loadNormalAd(normalAdId, showNormalfloor, onAdLoadedNormal, onAdFailed)
                    }
                )
            } else {
                loadNormalAd(normalAdId, showNormalfloor, onAdLoadedNormal, onAdFailed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onAdFailed?.invoke()
        }
    }

    private fun loadNormalAd(
        adId: String,
        showAd: Boolean = true,
        onAdLoaded: (nativeAd: NativeAd) -> Unit,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (!shouldShowAds()) {
            onAdFailed?.invoke()
            return
        }
        if (!showAd) {
            onAdFailed?.invoke()
            return
        }
        NextGenNativeLoader.load(
            adUnitId = adId,
            onLoaded = onAdLoaded,
            onFailed = {
                Log.e("nativeAdFlow", "Ad failed ❌ Requesting Normal $adId")
                onAdFailed?.invoke()
            }
        )
    }

    fun loadBanner(
        activity: Activity,
        highFloorAdId: String,
        normalAdId: String,
        showHighFloor: Boolean = true,
        showNormalFloor: Boolean = true,
        isCollapsable: Boolean = false,
        onLoaded: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null,
        adContainer: FrameLayout
    ) {
        if (!shouldShowAds()) {
            com.aiface.aging.ads_nextgen.NextGenAdCheck.skip(
                com.aiface.aging.ads_nextgen.NextGenAdCheck.BANNER,
                normalAdId,
                "shouldShowAds=false / pro",
            )
            return
        }
        @Suppress("UNUSED_VARIABLE")
        val unusedCollapsible = isCollapsable
        NextGenBannerHelper.loadWithFallback(
            activity = activity,
            container = adContainer,
            tryHigh = showHighFloor,
            highUnitId = highFloorAdId,
            normalUnitId = if (showNormalFloor) normalAdId else highFloorAdId,
            onLoaded = onLoaded,
            onFailed = onAdFailed
        )
    }

    fun loadBannerAd(
        activity: Activity,
        container: FrameLayout,
        adId: String,
        isCollapsable: Boolean = false,
        onLoaded: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        if (!shouldShowAds()) {
            onFailure?.invoke()
            return
        }
        @Suppress("UNUSED_VARIABLE")
        val unusedCollapsible = isCollapsable
        NextGenBannerHelper.loadIntoContainer(
            activity = activity,
            container = container,
            adUnitId = adId,
            onLoaded = onLoaded,
            onFailed = onFailure
        )
    }

    fun attachRevenueListener(interstitialAd: InterstitialAd, adUnitId: String, activity: Activity) {
        interstitialAd.rememberAdUnitId(adUnitId)
    }

    fun loadFallbackInterstitialAd(
        activity: Activity,
        highFloorAdId: String,
        normalAdId: String,
        loadHighFloor: Boolean = true,
        loadNormalFloor: Boolean = true,
        onAdLoadedHigh: (interstitialAd: InterstitialAd) -> Unit,
        onAdLoadedNormal: (interstitialAd: InterstitialAd) -> Unit,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (!shouldShowAds()) {
            onAdFailed?.invoke()
            return
        }

        if (loadHighFloor) {
            NextGenInterstitialHelper.load(
                adUnitId = highFloorAdId,
                onLoaded = { ad ->
                    onAdLoadedHigh(ad.rememberAdUnitId(highFloorAdId))
                },
                onFailed = {
                    if (loadNormalFloor) {
                        NextGenInterstitialHelper.load(
                            adUnitId = normalAdId,
                            onLoaded = { ad ->
                                onAdLoadedNormal(ad.rememberAdUnitId(normalAdId))
                            },
                            onFailed = { onAdFailed?.invoke() }
                        )
                    } else {
                        onAdFailed?.invoke()
                    }
                }
            )
        } else if (loadNormalFloor) {
            NextGenInterstitialHelper.load(
                adUnitId = normalAdId,
                onLoaded = { ad ->
                    onAdLoadedNormal(ad.rememberAdUnitId(normalAdId))
                },
                onFailed = { onAdFailed?.invoke() }
            )
        }
    }

    fun showInterstitial(
        forFragment: Boolean = false,
        interstitialAd: InterstitialAd,
        activity: FragmentActivity,
        onDismissed: (() -> Unit)? = null,
        eventName: String = ""
    ) {
        if (!shouldShowAds()) {
            onDismissed?.invoke()
            return
        }
        if (InterstitialAdGate.shouldSkipInterstitial()) {
            Log.w(TAG, "show Interstitial skipped — cooldown active (no loading dialog)")
            onDismissed?.invoke()
            return
        }
        @Suppress("UNUSED_VARIABLE")
        val unusedEvent = eventName
        activity.lifecycleScope.launch {
            try {
                showLoading(activity)
                delay(1000)
                // Early continue: activity immediate, fragment +500ms — not on real dismiss.
                interstitialAd.showFullscreenAd(
                    activity = activity,
                    contentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "show Interstitial → SHOW ✔")
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "show Interstitial SHOW FAILED ❌: ${adError.message}")
                            hideLoading()
                            onDismissed?.invoke()
                        }
                    },
                    forFragment = forFragment,
                    onContinue = {
                        Log.d(TAG, "show Interstitial → CONTINUE (early)")
                        nullifyUsedAd(interstitialAd)
                        hideLoading()
                        onDismissed?.invoke()
                    },
                )
            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                onDismissed?.invoke()
            }
        }
    }

    private fun nullifyUsedAd(interAd: InterstitialAd) {
        when (interAd) {
//            obInterstitialHigh -> obInterstitialHigh = null
//            obInterstitialAll -> obInterstitialAll = null
        }
    }

    fun showLoading(context: Context) {
        if (loadingDialog?.isShowing == true) return

        loadingDialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.ad_dialog)
            window?.apply {
                // Make background fully transparent
                setBackgroundDrawable(ColorDrawable(Color.WHITE))

                // Remove all margins → truly fullscreen
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val img = findViewById<ImageView>(R.id.progressImage)
            img?.startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate))

            // Dialog properties
            setCanceledOnTouchOutside(false)
            setCancelable(false)

            setOnDismissListener {
                loadingDialog = null
            }
        }

        try {
            loadingDialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideLoading() {
        try {
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
                loadingDialog = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun trackAdjustAdRevenue(
        adUnitId: String?,
        revenue: Double = 0.00,
        currency: String = "USD",
        token: String = "admob_sdk", context : Activity
    ) {
        try {
            val event = AdjustEvent(AdjustConstant.AD_IMPRESSION_TOKEN)
            // Assign custom identifier to event which will be reported in success/failure callbacks.
            event.addCallbackParameter("ad_unit_id", adUnitId)
            event.setRevenue(revenue, currency)
            Adjust.trackEvent(event)
            // printDebugLog("Revenue: $revenue Currency:$currency AdUnitId:$adUnitId")


            val metaParams = Bundle().apply {
                putString("ad_unit_id", adUnitId ?: "unknown")
                putString("currency", currency)
            }
            if (context != null) {
                val appEventsLogger = AppEventsLogger.newLogger(context)
                appEventsLogger.logEvent("ad_impression", metaParams)
                appEventsLogger.logEvent("ad_revenue", revenue, metaParams)
            }

        } catch (e: Exception) {
            // printDebugLog(" Failed to send revenue event: ${e.message}")
        }
    }

    fun getMediationInfo(nativeAd: NativeAd): String {
        val mediationAdapterClassName = try {
            nativeAd.getResponseInfo()?.toString()
        } catch (_: Exception) {
            null
        }
        return when {
            mediationAdapterClassName?.contains("facebook", ignoreCase = true) == true -> "meta"
            else -> "other"
        }
    }

    fun resolveNativeAdLayout(nativeAd: NativeAd): Int {
        return when (getMediationInfo(nativeAd)) {
            "meta" -> R.layout.layout_native_ads_meta
            else -> when (obNativeFormat) {
                1 -> R.layout.layout_native_ads_without_mediaview_b
                2 -> R.layout.layout_native_ads
                3 -> R.layout.layout_native_ads_ctr_up
                else -> R.layout.layout_native_ads
            }
        }
    }
    fun resolveNativeAdLayoutSmall(@Suppress("UNUSED_PARAMETER") nativeAd: NativeAd): Int {
        return R.layout.layout_native_ads_without_mediaview_b
    }
    fun resolveNativeAdLayoutSmallReel(@Suppress("UNUSED_PARAMETER") nativeAd: NativeAd): Int {
        return R.layout.layout_native_ads_without_mediaview
    }

    fun bindNativeAdToContainer(
        nativeAd: NativeAd?,
        container: FrameLayout?,
        shimmer: ShimmerFrameLayout?,
        activity: FragmentActivity,
        shimmerWrapper: View? = null,
    ) {
        if (!shouldShowAds()) {
            hideNativeShimmer(shimmer, shimmerWrapper)
            container?.visibility = View.GONE
            return
        }
        if (nativeAd == null || container == null) {
            hideNativeShimmer(shimmer, shimmerWrapper)
            container?.visibility = View.GONE
            return
        }

        try {
            val layoutResId = resolveNativeAdLayout(nativeAd)
            NativeAdDisplayHelper.display(
                container = container,
                inflater = LayoutInflater.from(activity),
                nativeAd = nativeAd,
                layoutResId = layoutResId,
                shimmer = shimmer,
            )
            // Re-apply CTA styling after display bind
            (container.getChildAt(0) as? NativeAdView)?.let { adView ->
                applyCtaStyle(adView, activity)
            }
            shimmerWrapper?.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
            hideNativeShimmer(shimmer, shimmerWrapper)
            container.visibility = View.GONE
        }
    }
    fun bindNativeAdToContainerSmall(
        nativeAd: NativeAd?,
        container: FrameLayout?,
        shimmer: ShimmerFrameLayout?,
        activity: FragmentActivity,
        shimmerWrapper: View? = null,
    ) {
        if (!shouldShowAds()) {
            hideNativeShimmer(shimmer, shimmerWrapper)
            container?.visibility = View.GONE
            return
        }
        if (nativeAd == null || container == null) {
            hideNativeShimmer(shimmer, shimmerWrapper)
            container?.visibility = View.GONE
            return
        }

        try {
            val layoutResId = resolveNativeAdLayoutSmall(nativeAd)
            NativeAdDisplayHelper.displayWithoutMedia(
                container = container,
                inflater = LayoutInflater.from(activity),
                nativeAd = nativeAd,
                layoutResId = layoutResId,
                shimmer = shimmer,
            )
            (container.getChildAt(0) as? NativeAdView)?.let { adView ->
                applyCtaStyle(adView, activity)
            }
            shimmerWrapper?.visibility = View.GONE
            nativeLanguage = null
            nativeLanguageAlt = null
        } catch (e: Exception) {
            e.printStackTrace()
            hideNativeShimmer(shimmer, shimmerWrapper)
            container.visibility = View.GONE
        }
    }
    fun bindNativeAdToContainerSmallReel(
        nativeAd: NativeAd?,
        container: FrameLayout?,
        shimmer: ShimmerFrameLayout?,
        activity: FragmentActivity,
        shimmerWrapper: View? = null,
    ) {
        if (!shouldShowAds()) {
            hideNativeShimmer(shimmer, shimmerWrapper)
            container?.visibility = View.GONE
            return
        }
        if (nativeAd == null || container == null) {
            hideNativeShimmer(shimmer, shimmerWrapper)
            container?.visibility = View.GONE
            return
        }

        try {
            val layoutResId = resolveNativeAdLayoutSmallReel(nativeAd)
            NativeAdDisplayHelper.displayWithoutMedia(
                container = container,
                inflater = LayoutInflater.from(activity),
                nativeAd = nativeAd,
                layoutResId = layoutResId,
                shimmer = shimmer,
            )
            (container.getChildAt(0) as? NativeAdView)?.let { adView ->
                applyCtaStyle(adView, activity)
            }
            shimmerWrapper?.visibility = View.GONE
            nativeLanguage = null
            nativeLanguageAlt = null
        } catch (e: Exception) {
            e.printStackTrace()
            hideNativeShimmer(shimmer, shimmerWrapper)
            container.visibility = View.GONE
        }
    }

    private fun hideNativeShimmer(shimmer: ShimmerFrameLayout?, shimmerWrapper: View?) {
        AdMainThread.run {
            shimmer?.stopShimmerOnMain()
            shimmer?.visibility = View.GONE
            shimmerWrapper?.visibility = View.GONE
        }
    }

    fun displayNative(
        nativeAd: NativeAd?,
        adBinding: FrameLayout?,
        activity: FragmentActivity?,
        shimmer: ShimmerFrameLayout
    ) {
        try {
            if (!shouldShowAds()) {
                hideNativeShimmer(shimmer, null)
                adBinding?.visibility = View.GONE
                return
            }
            if (nativeAd == null || activity == null || adBinding == null) {
                AdMainThread.run {
                    shimmer.stopShimmerOnMain()
                    shimmer.visibility = View.GONE
                    adBinding?.visibility = View.GONE
                }
                return
            }

            bindNativeAdToContainer(nativeAd, adBinding, shimmer, activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyCtaStyle(adView: NativeAdView, activity: FragmentActivity) {
        if(!fromHome) {
            val callToActionView = adView.findViewById<AppCompatButton>(R.id.ad_call_to_action)
                ?: adView.callToActionView as? AppCompatButton
            callToActionView?.apply {
                try {
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor(obCtaColor))
                    typeface = if (obCtaTextStyle.equals("bold", ignoreCase = true)) {
                        ResourcesCompat.getFont(activity, R.font.inter_bold)
                    } else {
                        ResourcesCompat.getFont(activity, R.font.inter_regular)
                    }
                    setTextColor(Color.parseColor(obCtaTextColor))
                } catch (_: Exception) {
                }
            }
        }
    }

    fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        activity: FragmentActivity
    ) {
        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        val callToActionView = adView.findViewById<AppCompatButton>(R.id.ad_call_to_action)
        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)

        adView.headlineView = headlineView
        adView.bodyView = bodyView
        adView.callToActionView = callToActionView

        headlineView?.apply {
            text = nativeAd.headline
            visibility = if (nativeAd.headline.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        bodyView?.apply {
            text = nativeAd.body
            visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        callToActionView?.apply {
            text = nativeAd.callToAction
            visibility = if (nativeAd.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
            backgroundTintList = ColorStateList.valueOf(Color.parseColor(obCtaColor))
            typeface = if (obCtaTextStyle.equals("bold", ignoreCase = true)) {
                ResourcesCompat.getFont(activity, R.font.inter_bold)
            } else {
                ResourcesCompat.getFont(activity, R.font.inter_regular)
            }
            setTextColor(Color.parseColor(obCtaTextColor))
        }

        val icon = nativeAd.icon
        if (iconView != null && icon != null) {
            adView.iconView = iconView
            iconView.setImageDrawable(icon.drawable)
            iconView.visibility = View.VISIBLE
        } else {
            adView.iconView = null
            iconView?.visibility = View.GONE
        }

        adView.registerNativeAd(nativeAd, mediaView)
    }

    fun populateNativeAdViewSmall(
        nativeAd: NativeAd,
        adView: NativeAdView,
        @Suppress("UNUSED_PARAMETER") activity: FragmentActivity,
    ) {
        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        try {
            val callToActionView = adView.findViewById<AppCompatButton>(R.id.ad_call_to_action)
            adView.callToActionView = callToActionView
            callToActionView?.apply {
                text = nativeAd.callToAction
                visibility = if (nativeAd.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
                backgroundTintList = ColorStateList.valueOf(Color.parseColor(obCtaColor))
                typeface = if (obCtaTextStyle.equals("bold", ignoreCase = true)) {
                    ResourcesCompat.getFont(activity, R.font.inter_bold)
                } else {
                    ResourcesCompat.getFont(activity, R.font.inter_regular)
                }
                setTextColor(Color.parseColor(obCtaTextColor))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        adView.headlineView = headlineView
        adView.bodyView = bodyView

        headlineView?.apply {
            text = nativeAd.headline
            visibility = if (nativeAd.headline.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        bodyView?.apply {
            text = nativeAd.body
            visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        mediaView?.visibility = View.GONE

        val icon = nativeAd.icon
        if (iconView != null && icon != null) {
            adView.iconView = iconView
            iconView.setImageDrawable(icon.drawable)
            iconView.visibility = View.VISIBLE
        } else {
            adView.iconView = null
            iconView?.visibility = View.GONE
        }

        adView.registerNativeAd(nativeAd, null)
    }
}