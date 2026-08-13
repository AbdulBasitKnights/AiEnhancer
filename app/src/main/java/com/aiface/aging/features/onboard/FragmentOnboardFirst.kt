package com.aiface.aging.features.onboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper.displayNative
import com.aiface.aging.shared.ads.AdsHelper.getMediationInfo
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.AdsHelper.obCtaColor
import com.aiface.aging.shared.ads.AdsHelper.obCtaTextColor
import com.aiface.aging.shared.ads.AdsHelper.obCtaTextStyle
import com.aiface.aging.shared.ads.AdsHelper.obNative1Enabled
import com.aiface.aging.shared.ads.AdsHelper.obNativeAd1
import com.aiface.aging.shared.ads.AdsHelper.obNativeAdHigh1
import com.aiface.aging.shared.ads.AdsHelper.obNativeFormat
import com.aiface.aging.shared.ads.AdsHelper.obNativeHigh1Enabled
import com.aiface.aging.databinding.FragmentOnbaordingFirstBinding
import com.aiface.aging.features.onboard.adapter.PagerNav
import com.aiface.aging.utils.FirebaseLogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentOnboardFirst : Fragment() {

    private var binding: FragmentOnbaordingFirstBinding? = null

    private val nav: PagerNav by lazy {
        (parentFragment as? PagerNav)
            ?: (activity as? PagerNav)
            ?: error(
                "Host must implement OnboardingFragment.PagerNav " +
                        "(either the parent fragment or the activity)."
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnbaordingFirstBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity?.let { activity ->
            FirebaseLogUtils.logEvent("onboarding_1_view", "")

            if (AiFaceApp.isSolidObButton){
                binding?.btnNextOnboarding?.setBackgroundResource(R.drawable.bg_selected_card)
                binding?.btnNextOnboarding?.setTextColor(ContextCompat.getColor(activity, R.color.white))
            }

            if (isProVersion.value == false) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        if (obNative1Enabled || obNativeHigh1Enabled) {
                            if (obNativeAdHigh1 != null && obNativeHigh1Enabled) {
                                displayNative(
                                    obNativeAdHigh1,
                                    binding?.nativeAdView,
                                    mActivity,
                                    binding?.shimmerContainerNative?.shimmerContainerNative!!
                                )
                            } else if (obNativeAd1 != null && obNative1Enabled) {
                                displayNative(
                                    obNativeAd1,
                                    binding?.nativeAdView,
                                    mActivity,
                                    binding?.shimmerContainerNative?.shimmerContainerNative!!
                                )
                            } else {
                                binding?.apply {
                                    shimmerContainerNative.shimmerContainerNative.let { shimmerLayout ->
                                        shimmerLayout.stopShimmer()
                                        shimmerLayout.visibility = View.INVISIBLE
                                    }
                                }
                            }
                        }
                    }
                }
                binding?.apply {
                    shimmerContainerNative.shimmerContainerNative.let { shimmerLayout ->
                        shimmerLayout.startShimmer()
                    }
                }
                if (!obNativeHigh1Enabled && !obNative1Enabled) {
                    binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
                }
            } else {
                binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
            }

            binding?.btnNextOnboarding?.setOnClickListener {
                FirebaseLogUtils.logEvent("onboarding_1_next", "")
                nav.goNext()
                // OnboardingActivity.selectedPosition.value = 1
            }
        }

    }




    private var mActivity: FragmentActivity? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }
}