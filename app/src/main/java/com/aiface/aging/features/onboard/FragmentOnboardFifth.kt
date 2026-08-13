package com.aiface.aging.features.onboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.aiface.aging.MainActivity
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.shared.ads.AdsHelper.getMediationInfo
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.AdsHelper.isShowNativeFullCross
import com.aiface.aging.shared.ads.AdsHelper.nativeFullCrossDelay
import com.aiface.aging.shared.ads.AdsHelper.obCtaColor
import com.aiface.aging.shared.ads.AdsHelper.obCtaTextStyle
import com.aiface.aging.shared.ads.AdsHelper.obNative4Enabled
import com.aiface.aging.shared.ads.AdsHelper.obNativeAd4
import com.aiface.aging.shared.ads.AdsHelper.obNativeAdHigh4
import com.aiface.aging.shared.ads.AdsHelper.obNativeHigh4Enabled
import com.aiface.aging.shared.ads.interstitialOb
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.databinding.FragmentOnbaordFullNativeBinding
import com.aiface.aging.features.permission.PermissionActivity
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FragmentOnboardFifth : Fragment() {

    private var binding: FragmentOnbaordFullNativeBinding? = null
    private var mActivity: FragmentActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnbaordFullNativeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            FirebaseLogUtils.logEvent("onboarding_5_view", "")
            if (isProVersion.value == false) {
                showNativeFull(activity)
            } else {
                exitOnboarding(activity)
            }
        }
    }

    private fun showNativeFull(activity: FragmentActivity) {
        try {
            val nativeFull = when {
                obNativeAdHigh4 != null && obNativeHigh4Enabled -> obNativeAdHigh4
                obNativeAd4 != null && obNative4Enabled -> obNativeAd4
                else -> null
            }
            nativeFull?.let {
                val type = getMediationInfo(it)
                val layoutResId = when (type) {
                    "meta" -> R.layout.layout_native_full_screen_meta
                    else -> R.layout.layout_native_full_screen
                }

                val adView = LayoutInflater.from(activity)
                    .inflate(layoutResId, null) as NativeAdView

                populateNativeAdView(it, adView, activity)

                binding?.shimmerContainerNative?.shimmerContainerNative?.stopShimmer()
                binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.GONE
                binding?.nativeAdView?.removeAllViews()
                binding?.nativeAdView?.addView(adView)
                binding?.nativeAdView?.visibility = View.VISIBLE

                val close = adView.findViewById<ImageView>(R.id.closeAd)
                if (type == "meta") {
                    lifecycleScope.launch {
                        delay(nativeFullCrossDelay.toLong() * 1000)
                        close?.visibility = View.VISIBLE
                    }
                } else if (isShowNativeFullCross) {
                    lifecycleScope.launch {
                        delay(nativeFullCrossDelay.toLong() * 1000)
                        close?.visibility = View.VISIBLE
                    }
                }
                close?.setOnClickListener {
                    FirebaseLogUtils.logEvent("onboarding_5_next", "")
                    showInterOb(activity)
                }
            } ?: run {
                exitOnboarding(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            exitOnboarding(activity)
        }
    }

    private fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        activity: FragmentActivity
    ) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        val mediaView = adView.findViewById<com.google.android.libraries.ads.mobile.sdk.nativead.MediaView>(R.id.ad_media)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? AppCompatButton)?.text = nativeAd.callToAction
                (adView.callToActionView as? AppCompatButton)?.backgroundTintList =
            ColorStateList.valueOf(obCtaColor.toColorInt())
        val typeface = if (obCtaTextStyle.equals("bold", ignoreCase = true)) {
            ResourcesCompat.getFont(activity, R.font.inter_bold)
        } else {
            ResourcesCompat.getFont(activity, R.font.inter_regular)
        }
        (adView.callToActionView as? AppCompatButton)?.typeface = typeface
        adView.registerNativeAd(nativeAd, mediaView)
    }

    private fun exitOnboarding(activity: FragmentActivity) {
        if (isProVersion.value == false) {
            showInterOb(activity)
        } else {
            navigateNext(activity)
        }
    }

    private fun showInterOb(currentActivity: FragmentActivity) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false &&
                    interstitialOb != null &&
                    !com.aiface.aging.shared.ads.InterstitialAdGate.shouldSkipInterstitial()
                ) {
                    GlobalLoader.show(currentActivity)
                    delay(1000)
                    navigateNext(currentActivity)
                    if (interstitialOb != null &&
                        !com.aiface.aging.shared.ads.InterstitialAdGate.shouldSkipInterstitial()
                    ) {
                        interstitialOb?.showFullscreenAd(
                            currentActivity,
                            object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() {
                                currentActivity.lifecycleScope.launch {
                                    delay(1500)
                                    GlobalLoader.hide(currentActivity)
                                    LogUtils.printLog(
                                        "inter_home shown",
                                        interstitialTrackedUnitId(interstitialOb)
                                    )
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                GlobalLoader.hide(currentActivity)
                                interstitialOb = null
                            }

                            override fun onAdDismissedFullScreenContent() {
                                GlobalLoader.hide(currentActivity)
                                interstitialOb = null
                            }

                            override fun onAdImpression() {
                                super.onAdImpression()
                                interstitialOb = null
                            }
                        },
                        )
                    } else {
                        GlobalLoader.hide(currentActivity)
                    }
                    interstitialOb = null
                } else {
                    interstitialOb = null
                    navigateNext(currentActivity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                navigateNext(currentActivity)
            }
        }
    }

    private fun hasPermission(activity: FragmentActivity): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                audioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun navigateNext(currentActivity: FragmentActivity) {
        if (AiFaceApp.isShowPermission && !hasPermission(currentActivity)) {
            startActivity(Intent(currentActivity, PermissionActivity::class.java))
            currentActivity.finish()
        } else {
            startActivity(Intent(currentActivity, MainActivity::class.java))
            currentActivity.finish()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }
}