package com.aiface.aging.features.look.instruction

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.databinding.FragmentInstructionBinding
import com.aiface.aging.features.home.HomeFragment.Companion.requestPermission
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.features.look.LookConstants
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.shared.ads.canPresentHomeInterstitial
import com.aiface.aging.shared.ads.interstitialHome
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InstructionFragment : Fragment() {

    private var _binding: FragmentInstructionBinding? = null
    private val binding get() = _binding
    private var mActivity: FragmentActivity? = null
    private var nativeInstruction: NativeAd? = null

    private val type: String by lazy {
        requireArguments().getString(ARG_TYPE).orEmpty()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInstructionBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        mActivity?.let { loadAds(it) }
        binding?.btnGotIt?.setOnClickListener {
            mActivity?.let { activity ->
                showInterForGallery(activity)
            }
        }
        binding?.back?.setOnClickListener {
            mActivity?.finish()
        }
    }

    private fun loadAds(activity: FragmentActivity) {
        if (AiFaceApp.isNativePreviewAiHf && AiFaceApp.isNativePreviewAi) {
            startNative(tryHigh = true)
        } else if (AiFaceApp.isNativePreviewAi) {
            startNative(tryHigh = false)
        } else {
            binding?.clAd?.visibility = View.INVISIBLE
        }
    }

    private fun startNative(tryHigh: Boolean) {
        try {
            if (!AdsHelper.shouldShowAds()) {
                binding?.clAd?.visibility = View.INVISIBLE
                AdShimmerHelper.hideNativeAdSlot(
                    adSlot = binding?.clAd,
                    shimmerWrapper = binding?.shimmer,
                    nativeContainer = binding?.nativeAdView,
                )
                return
            }
            binding?.clAd?.visibility = View.VISIBLE
            AdShimmerHelper.showLayoutNativePlaceholder(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_home_hf,
                normalUnitId = BuildConfig.native_home,
                onLoaded = { ad, _ ->
                    try {
                        if (!isAdded || view == null || binding == null) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        val host = activity ?: run {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativeInstruction?.destroy()
                        nativeInstruction = ad
                        showNativeInstruction(host)
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
                },
            )
        } catch (t: Throwable) {
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }

    private fun showNativeInstruction(activity: FragmentActivity) {
        binding?.clAd?.visibility = View.VISIBLE
        AdsHelper.bindNativeAdToContainerSmall(
            nativeInstruction,
            binding?.nativeAdView,
            binding?.shimmerContainerNative?.shimmerContainerNative,
            activity,
            binding?.shimmer,
        )
    }

    private fun galleryEditorKey(): String = when (type) {
        LookConstants.SCREEN_HAIR_COLOR -> "hair"
        LookConstants.SCREEN_MAKEUP -> "makeup"
        else -> "makeup"
    }

    private fun showInterForGallery(currentActivity: FragmentActivity) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false) {
                    if (canPresentHomeInterstitial()) {
                        GlobalLoader.show(currentActivity)
                        delay(1000)
                        if (canPresentHomeInterstitial()) {
                            interstitialHome?.showFullscreenAd(
                                activity = currentActivity,
                                contentCallback = object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    requestPermission.value = false
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                        LogUtils.printLog(
                                            "inter_home shown",
                                            interstitialTrackedUnitId(interstitialHome),
                                        )
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                    LogUtils.printLog(
                                        "inter_home failed to shown",
                                        interstitialTrackedUnitId(interstitialHome),
                                    )
                                    com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                        requestPermission.value = true
                                        openCustomGallery(currentActivity)
                                    }
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialHome = null
                                }
                            },
                                forFragment = true,
                                onContinue = {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                    // Gallery + storage permission only after inter fully gone.
                                    com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                        requestPermission.value = true
                                        openCustomGallery(currentActivity)
                                    }
                                },
                            )
                        } else {
                            GlobalLoader.hide(currentActivity)
                        }
                        interstitialHome = null
                    } else {
                        requestPermission.value = true
                        interstitialHome = null
                        openCustomGallery(currentActivity)
                    }
                } else {
                    requestPermission.value = true
                    openCustomGallery(currentActivity)
                }
            } catch (e: Exception) {
                requestPermission.value = true
                e.printStackTrace()
            }
        }
    }

    private fun openCustomGallery(activity: FragmentActivity) {
        TedImagePicker.with(activity, galleryEditorKey())
            .image()
            .max(1, "cannot select more than 1 image")
            .min(1, "select at least 1 image")
            .bundleExtras(bundleOf(LookConstants.EXTRA_FEATURE_TYPE to type))
            .albumType(AlbumType.DROP_DOWN)
            .startMultiImageFragment()
    }

    override fun onDestroyView() {
        nativeInstruction?.destroy()
        nativeInstruction = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TYPE = "type"

        fun newInstance(type: String): InstructionFragment {
            return InstructionFragment().apply {
                arguments = bundleOf(ARG_TYPE to type)
            }
        }
    }
}