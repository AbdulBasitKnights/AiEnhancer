package com.aiface.aging.features.survey

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.MainActivity
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.AdsHelper.obEnable
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.shared.ads.interstitialSurvey
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.databinding.ActivitySurveyBinding
import com.aiface.aging.features.main.MainFragment
import com.aiface.aging.features.fullonboard.FullOnboardActivity
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SurveyActivity  : AppCompatActivity()  {

    private  var binding: ActivitySurveyBinding?= null



    private var nativeSurvey: NativeAd? = null

    private var isSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        applyLightSystemBars()
        FirebaseLogUtils.logEvent("survey_view", "")

        if (AiFaceApp.isNativeSurveyHf && AiFaceApp.isNativeSurvey) {
            startNative(tryHigh = true)
        } else if (AiFaceApp.isNativeSurvey) {
            startNative(tryHigh = false)
        } else {
            binding?.clbottom?.visibility = View.GONE
        }

//        if (AiFaceApp.isInterSurveyHf && AiFaceApp.isInterSurvey) {
//            loadInterSurveyHigh(this)
//        } else if (AiFaceApp.isInterSurvey) {
//            loadInterSurvey(this)
//        }

        binding?.btnNext?.setOnClickListener {
            showInterSurvey(this)
        }
        binding?.clGallery?.setOnClickListener {
            binding?.clGallery?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
        binding?.clNanoBanana?.setOnClickListener {
            binding?.clNanoBanana?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
        binding?.photoEnhancer?.setOnClickListener {
            binding?.photoEnhancer?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
        binding?.clEditPhoto?.setOnClickListener {
            binding?.clEditPhoto?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
        binding?.clBodyMaker?.setOnClickListener {
            binding?.clBodyMaker?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
        binding?.clCollage?.setOnClickListener {
            binding?.clCollage?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
    }


    private fun setSelected(){

        binding?.btnNext?.background = resources.getDrawable(R.drawable.bg_rounded_cl_blue)

    }

    fun showInterSurvey(
        currentActivity: FragmentActivity,
    ) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false) {

                    if (interstitialSurvey != null &&
                        !com.aiface.aging.shared.ads.InterstitialAdGate.shouldSkipInterstitial()
                    ) {
                        GlobalLoader.show(currentActivity)
                        delay(1000)
                        navigateNext()

                        if (interstitialSurvey != null &&
                            !com.aiface.aging.shared.ads.InterstitialAdGate.shouldSkipInterstitial()
                        ) {
                            interstitialSurvey?.showFullscreenAd(
                                currentActivity,
                                object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                        LogUtils.printLog(
                                            "inter_survey shown",
                                            interstitialTrackedUnitId(interstitialSurvey)
                                        )
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialSurvey = null

                                    LogUtils.printLog(
                                        "inter_survey failed to shown",
                                        interstitialTrackedUnitId(interstitialSurvey)
                                    )
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialSurvey = null

                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialSurvey = null

                                }
                            },
                            )
                        } else {
                            GlobalLoader.hide(currentActivity)

                        }
                        interstitialSurvey = null
                    } else {
                        interstitialSurvey = null

                        navigateNext()

                    }


                } else {

                    navigateNext()

                }
            } catch (e: Exception) {
                navigateNext()
                e.printStackTrace()
            }

        }

    }

    private fun navigateNext(){
        val nextActivity = if (obEnable) {
            FullOnboardActivity::class.java
        } else {
            MainFragment.selectedItem.value = 0
            MainActivity::class.java
        }

        startActivity(Intent(this, nextActivity))
        finish()
    }





    private fun startNative(tryHigh: Boolean) {
        try {
            if (!AdsHelper.shouldShowAds() || !NetworkUtils.isOnline(this)) {
                binding?.clbottom?.visibility = View.GONE
                AdShimmerHelper.hideNativeAdSlot(
                    adSlot = binding?.clbottom,
                    shimmerWrapper = binding?.shimmer,
                    nativeContainer = binding?.nativeAdView,
                )
                return
            }
            AdShimmerHelper.showLayoutNativePlaceholder(
                adSlot = binding?.clbottom,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_survey_hf,
                normalUnitId = BuildConfig.native_survey,
                onLoaded = { ad, unitId ->
                    try {
                        nativeSurvey?.destroy()
                        nativeSurvey = ad
                        val container = binding?.nativeAdView
                        if (container == null) {
                            ad.destroy()
                            AdShimmerHelper.hideNativeAdSlot(
                                adSlot = binding?.clbottom,
                                shimmerWrapper = binding?.shimmer,
                            )
                            return@loadWithFallback
                        }
                        NativeAdDisplayHelper.display(
                            container = container,
                            inflater = layoutInflater,
                            nativeAd = ad,
                            onDestroyPrevious = {},
                            adUnitId = unitId,
                            layoutResId = R.layout.layout_native_ads,
                            shimmer = binding?.shimmer
                        )
                    } catch (t: Throwable) {
                        try {
                            ad.destroy()
                        } catch (_: Throwable) {
                        }
                        AdShimmerHelper.hideNativeAdSlot(
                            adSlot = binding?.clbottom,
                            shimmerWrapper = binding?.shimmer,
                            nativeContainer = binding?.nativeAdView,
                        )
                    }
                },
                onFailed = {
                    AdShimmerHelper.hideNativeAdSlot(
                        adSlot = binding?.clbottom,
                        shimmerWrapper = binding?.shimmer,
                        nativeContainer = binding?.nativeAdView,
                    )
                }
            )
        } catch (t: Throwable) {
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clbottom,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }

    override fun onDestroy() {
        nativeSurvey?.destroy()
        nativeSurvey = null
        super.onDestroy()
    }
}