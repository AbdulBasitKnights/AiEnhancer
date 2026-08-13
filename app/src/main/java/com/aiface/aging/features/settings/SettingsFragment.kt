package com.aiface.aging.features.settings

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.AiFaceApp.Companion.isNativeSetting
import com.aiface.aging.AiFaceApp.Companion.langSetting
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.AppOpenManager
import com.aiface.aging.shared.privacyPolicy
import com.aiface.aging.shared.rateUs
import com.aiface.aging.shared.shareApp
import com.aiface.aging.shared.termsOfServices
import com.aiface.aging.databinding.FragmentSettingBinding
import com.aiface.aging.features.language.LanguageActivity
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class SettingsFragment : Fragment(), SettingsClickListener {
    private var binding: FragmentSettingBinding? = null
    private var settingsAdapter: SettingsAdapter? = null
    private var mActivity: FragmentActivity? = null

    private var nativeSettings: NativeAd? = null

    companion object {
        private const val TAG = "SettingsNative"
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        setClickListeners()
        return binding?.root
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->

            FirebaseLogUtils.logEvent(
                "setting_view",
                ""
            )

            AppUtils.hideHomeBannerAd(activity)
            langSetting=true
            settingsAdapter = SettingsAdapter(activity, arrayListOf(), this)
            val layoutManager = LinearLayoutManager(activity)
            binding?.rvSettings?.layoutManager = layoutManager
            binding?.rvSettings?.adapter = settingsAdapter
            lifecycleScope.launch {
                getSettingsFlow().collectLatest { list ->
                    withContext(Dispatchers.Main) {
                        settingsAdapter?.updateList(list)

                    }
                }
            }

            binding?.back?.setOnClickListener {
                findNavController().popBackStack()
            }

            // Always attempt settings native for non-pro (RC defaults keep flag true).
            loadSettingsNativeAd(activity)
        }
    }

    private fun setClickListeners() {


    }

    private fun loadSettingsNativeAd(activity: FragmentActivity) {
        if (!AdsHelper.shouldShowAds() || !isNativeSetting) {
            binding?.clbottom?.visibility = View.GONE
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clbottom,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
            Log.d(TAG, "skip — pro/ads off")
            return
        }
        // If RC never set defaults and key missing, force-enable for settings.
        if (!AiFaceApp.isNativeSetting) {
            Log.w(TAG, "native_setting=false from RC — still loading settings native")
        }
        if (!NetworkUtils.isOnline(activity)) {
            binding?.clbottom?.visibility = View.GONE
            Log.d(TAG, "skip — offline")
            return
        }
        if(isNativeSetting) {
            startNative(tryHigh = AiFaceApp.isNativeSetting)
        }
    }

    private fun getDrawable(int: Int): Drawable? {
        return mActivity?.let { ContextCompat.getDrawable(it, int) }
    }

    private fun getSettingsFlow(): Flow<List<ModelSettings>> = flow {
        val list = arrayListOf<ModelSettings>()

        list.add(
            ModelSettings(
                1,
                getString(R.string.change_language),
                false,
                icon = getDrawable(R.drawable.language_svg),
                background = getDrawable(R.drawable.bg_setting_top)
            )
        )
        list.add(
            ModelSettings(
                2,
                getString(R.string.rate_us),
                false,
                icon = getDrawable(R.drawable.rate_svg),
                background = getDrawable(R.drawable.bg_setting_middle)
            )
        )


        list.add(
            ModelSettings(
                3,
                getString(R.string.share_the_app),
                false,
                icon = getDrawable(R.drawable.share_svg),
                background = getDrawable(R.drawable.bg_setting_middle),
            )

        )
        list.add(
            ModelSettings(
                4,
                getString(R.string.privacy_policy),
                false,
                icon = getDrawable(R.drawable.privacy_svg),
                background = getDrawable(R.drawable.bg_setting_middle)
            )
        )
        list.add(
            ModelSettings(
                5, getString(R.string.terms_of_use),
                false,
                icon = getDrawable(R.drawable.terms_svg),
                background = getDrawable(R.drawable.bg_setting_bottom)
            )
        )


        emit(list)
    }

    override fun onSettingItemClick(id: Int) {
        mActivity?.let { activity ->
            AppOpenManager.disableAppOpen = true
            when (id) {
                1 -> {
                    langSetting=true
                    FirebaseLogUtils.logEvent(
                        "setting_language_click",
                        ""
                    )

                    startActivity(Intent(activity, LanguageActivity::class.java))

                }
                2->{
                    FirebaseLogUtils.logEvent(
                        "setting_rateus_click",
                        ""
                    )
                    showRateUsDialog()
                }
                3->{
                    FirebaseLogUtils.logEvent(
                        "setting_share_app",
                        ""
                    )
                    mActivity?.shareApp()

                }
                4->{
                    FirebaseLogUtils.logEvent(
                        "setting_privacy",
                        ""
                    )
                    mActivity?.privacyPolicy()

                }
                5->{
                    FirebaseLogUtils.logEvent(
                        "setting_termsconsition",
                        ""
                    )
                    mActivity?.termsOfServices()

                }

            }
        }
    }

    private fun goToCancelSubscription() {
        mActivity?.let { activity ->
            try {
                val packageName = activity.packageName
                val uri =
                    Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun deleteDir(dir: File?): Boolean {
        try {
            if (dir != null && dir.isDirectory) {
                val children = dir.list()
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir?.delete() ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(500)
            AppOpenManager.disableAppOpen = false
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

    override fun onDestroy() {
        nativeSettings?.destroy()
        nativeSettings = null
        super.onDestroy()
      mActivity?.let { activity ->
          AppUtils.showHomeBannerAd(activity)
          AppOpenManager.disableAppOpen = false
      }
    }

    private fun openSocialMedia(link: String) {
        val socialIntent = Uri.parse(link)
        val facebookIntent = Intent(Intent.ACTION_VIEW, socialIntent)
        try {
            if (mActivity != null) {
                if (mActivity?.packageManager?.let { facebookIntent.resolveActivity(it) } != null) {
                    mActivity?.startActivity(facebookIntent)
                } else {
                    val browserIntent = Intent(Intent.ACTION_VIEW, socialIntent)
                    mActivity?.startActivity(browserIntent)
                }
            } else {
                // Handle the case where mActivity is null
            }
        } catch (e: Exception) {
            // Handle the exception here (e.g., log it or show an error message)
            e.printStackTrace()
        }
    }


    private fun showRateUsDialog() {
       mActivity?.let { activity ->
           activity.rateUs()
       }
    }


    private fun startNative(tryHigh: Boolean) {
        try {
            val host = activity ?: return
            if (!AdsHelper.shouldShowAds()) {
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
            Log.d(TAG, "request native tryHigh=$tryHigh unit=${BuildConfig.native_share}")
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_share_hf,
                normalUnitId = BuildConfig.native_share,
                onLoaded = { ad, _ ->
                    try {
                        if (!isAdded || view == null || binding == null) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativeSettings?.destroy()
                        nativeSettings = ad
                        val container = binding?.nativeAdView
                        if (container == null) {
                            ad.destroy()
                            AdShimmerHelper.hideNativeAdSlot(
                                adSlot = binding?.clbottom,
                                shimmerWrapper = binding?.shimmer,
                            )
                            return@loadWithFallback
                        }
                        Log.d(TAG, "native loaded — binding to container")
                        AdsHelper.bindNativeAdToContainer(
                            nativeAd = ad,
                            container = container,
                            shimmer = binding?.root?.findViewById(R.id.shimmer_container_native),
                            activity = host,
                            shimmerWrapper = binding?.shimmer,
                        )
                        binding?.shimmer?.visibility = View.GONE
                        container.visibility = View.VISIBLE
                        container.bringToFront()
                        binding?.clbottom?.requestLayout()
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
                    Log.e(TAG, "native failed to load")
                    AdShimmerHelper.hideNativeAdSlot(
                        adSlot = binding?.clbottom,
                        shimmerWrapper = binding?.shimmer,
                        nativeContainer = binding?.nativeAdView,
                    )
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "startNative failed", t)
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clbottom,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }
}