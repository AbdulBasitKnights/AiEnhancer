package com.aiface.aging.features.uninstall

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
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
import com.aiface.aging.databinding.FragmentUninstallSubBinding


class ShortcutUninstallSubFragment : Fragment() {
    private var binding: FragmentUninstallSubBinding? = null

    private var mActivity: FragmentActivity? = null
    private var nativeUninstall: NativeAd? = null

    private var isReloaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUninstallSubBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->


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


            binding?.ivBack?.setOnClickListener {
                findNavController().popBackStack()
            }

            binding?.ivHome?.setOnClickListener {

                mActivity?.let {


                        it.startActivity(Intent(it, MainActivity::class.java))
                        it.finish()

                }
            }

            binding?.cbReason1?.setOnCheckedChangeListener { buttonView, isChecked ->
                checkStatus()
            }
            binding?.cbReason2?.setOnCheckedChangeListener { buttonView, isChecked ->
                checkStatus()
            }
            binding?.cbReason3?.setOnCheckedChangeListener { buttonView, isChecked ->
                checkStatus()
            }
            binding?.cbReason4?.setOnCheckedChangeListener { buttonView, isChecked ->
                checkStatus()
            }
            binding?.cbReason5?.setOnCheckedChangeListener { buttonView, isChecked ->
                checkStatus()
            }

            binding?.btnUninstall?.setOnClickListener {
                if (isCheckboxSelected()) {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${requireContext().packageName}")
                        }
                    com.aiface.aging.shared.ads.AppOpenManager.suppressForSettings()
                    startActivity(intent)
                }

            }

            binding?.btnCancel?.setOnClickListener {
             //   AppUtils.getMain(activity).loadBannerAd()
             mActivity?.let {


                     it.startActivity(Intent(it, MainActivity::class.java))
                     it.finish()



             }
            }
        }
    }




    private fun checkStatus() {
        mActivity?.let {
            if (!isReloaded) {
                isReloaded = true

            }


        if (isCheckboxSelected()) {
            binding?.btnUninstall?.setTextColor(Color.parseColor("#8850FF"))
            binding?.btnUninstall?.let { btn->
                btn.background =
                    ContextCompat.getDrawable(it, R.drawable.bg_circled_uninstall_2)
//                ViewCompat.setBackgroundTintList(
//                    it,
//                    ColorStateList.valueOf(Color.parseColor("#8850FF"))
//                )
            }
        } else {
            binding?.btnUninstall?.setTextColor(Color.parseColor("#BFA4FE"))
            binding?.btnUninstall?.let { btn->
                btn.background =
                    ContextCompat.getDrawable(it, R.drawable.bg_circled_uninstall_1)
//                ViewCompat.setBackgroundTintList(
//                    it,
//                    ColorStateList.valueOf(Color.parseColor("#8850FF"))
//                )
            }
//            binding?.btnUninstall?.let {
//                ViewCompat.setBackgroundTintList(
//                    it,
//                    ColorStateList.valueOf(Color.parseColor("#F2F2F2"))
//                )
//            }

        }
    }
    }

    private fun isCheckboxSelected(): Boolean {
        return if (binding?.cbReason1?.isChecked == true) {
            true
        } else if (binding?.cbReason2?.isChecked == true) {
            true
        } else if (binding?.cbReason3?.isChecked == true) {
            true
        } else if (binding?.cbReason4?.isChecked == true) {
            true
        } else if (binding?.cbReason5?.isChecked == true) {
            true
        } else false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
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

    override fun onDestroy() {
        nativeUninstall?.destroy()
        nativeUninstall = null
        super.onDestroy()
    }
}