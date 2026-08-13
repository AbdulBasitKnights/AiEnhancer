package com.aiface.aging.features.uninstall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.MainActivity
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.databinding.FragmentUninstallMainBinding
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.utils.FirebaseLogUtils


class ShortcutUninstallMainFragment : Fragment() {

    private var binding: FragmentUninstallMainBinding? = null

    private var mActivity: FragmentActivity? = null

    private var nativeUninstall: NativeAd? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUninstallMainBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            FirebaseLogUtils.logEvent("uninstall_problem_confirm_view", "User views the confirm uninstall screen")

            if (isProVersion.value == false) {
                if (AiFaceApp.nativeUninstallHf && AiFaceApp.nativeUninstall) {
                    startNative(tryHigh = true)
                } else if (AiFaceApp.nativeUninstall) {
                    startNative(tryHigh = false)
                } else {
                    binding?.clAd?.visibility = View.GONE
                }
            } else {
                binding?.clAd?.visibility = View.GONE
            }

            clickListeners()
            handleBackPress(activity)


        }
    }

    private fun clickListeners() {
        binding?.ivBack?.setOnClickListener {
            FirebaseLogUtils.logEvent("uninstall_problem_confirm_back", "uninstall_problem_confirm_back")
            mActivity?.onBackPressed()
        }
        binding?.ivHome?.setOnClickListener {
            mActivity?.onBackPressed()
        }

        binding?.tvUninstallAnyway?.setOnClickListener {
            FirebaseLogUtils.logEvent("uninstall_confirm_still_want", "uninstall_confirm_still_want")
            findNavController().navigate(ShortcutUninstallMainFragmentDirections.actionUnstallToQuestions())
        }
        binding?.btnDontUninstall?.setOnClickListener {
            FirebaseLogUtils.logEvent("uninstall_confirm_dont_yet", "uninstall_confirm_dont_yet")
            mActivity?.onBackPressed()
        }
        binding?.btnRemove?.setOnClickListener {
            goToSubscription()
        }
        binding?.btnUpdate?.setOnClickListener {
            goToSubscription()
        }
        binding?.btnEnhance?.setOnClickListener {
            goToSubscription()
        }
    }

    private fun goToSubscription() {
     /*   if (isProVersion.value == false){
            val intent = Intent(requireActivity(), IAPActivity::class.java)
            startActivity(intent)
        }else{*/
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
//        }

    }

    private fun handleBackPress(activity: FragmentActivity) {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!activity.isFinishing && !activity.isDestroyed){
                    findNavController().popBackStack()
                }

            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback)
    }




    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()

    }

    override fun onDestroy() {
        nativeUninstall?.destroy()
        nativeUninstall = null
        super.onDestroy()
        mActivity?.let {
          //  AppUtils.getMain(activity).showBanner()
        }
        mActivity = null
    }

    private fun startNative(tryHigh: Boolean) {
        try {
            if (!AdsHelper.shouldShowAds()) {
                binding?.clAd?.visibility = View.GONE
                AdShimmerHelper.hideNativeAdSlot(
                    adSlot = binding?.clAd,
                    shimmerWrapper = binding?.shimmer,
                    nativeContainer = binding?.nativeAdView,
                )
                return
            }
            AdShimmerHelper.showLayoutNativePlaceholder(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_home_hf,
                normalUnitId = BuildConfig.native_home,
                onLoaded = { ad, unitId ->
                    try {
                        if (!isAdded || view == null || binding == null) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativeUninstall?.destroy()
                        nativeUninstall = ad
                        val container = binding?.nativeAdView
                        if (container == null) {
                            ad.destroy()
                            AdShimmerHelper.hideNativeAdSlot(
                                adSlot = binding?.clAd,
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
                            layoutResId = R.layout.layout_native_ads_without_mediaview_b,
                            shimmer = binding?.shimmer
                        )
                    } catch (t: Throwable) {
                        try {
                            ad.destroy()
                        } catch (_: Throwable) {
                        }
                        AdShimmerHelper.hideNativeAdSlot(
                            adSlot = binding?.clAd,
                            shimmerWrapper = binding?.shimmer,
                            nativeContainer = binding?.nativeAdView,
                        )
                    }
                },
                onFailed = {
                    AdShimmerHelper.hideNativeAdSlot(
                        adSlot = binding?.clAd,
                        shimmerWrapper = binding?.shimmer,
                        nativeContainer = binding?.nativeAdView,
                    )
                }
            )
        } catch (t: Throwable) {
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }

}