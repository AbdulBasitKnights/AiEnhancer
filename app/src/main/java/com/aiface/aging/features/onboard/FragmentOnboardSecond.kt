package com.aiface.aging.features.onboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper.displayNative
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.AdsHelper.obNative3Enabled
import com.aiface.aging.shared.ads.AdsHelper.obNativeAd3
import com.aiface.aging.shared.ads.AdsHelper.obNativeAdHigh3
import com.aiface.aging.shared.ads.AdsHelper.obNativeHigh3Enabled
import com.aiface.aging.databinding.FragmentOnbaordingSecondBinding
import com.aiface.aging.features.onboard.adapter.PagerNav
import com.aiface.aging.utils.FirebaseLogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentOnboardSecond : Fragment() {

    private var binding: FragmentOnbaordingSecondBinding? = null

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
        binding = FragmentOnbaordingSecondBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity?.let { activity ->
            FirebaseLogUtils.logEvent("onboarding_2_view", "")

            if (AiFaceApp.isSolidObButton) {
                binding?.btnNextOnboarding?.setBackgroundResource(R.drawable.bg_selected_card)
                binding?.btnNextOnboarding?.setTextColor(ContextCompat.getColor(activity, R.color.white))
            }

            if (isProVersion.value == false) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        if (obNative3Enabled || obNativeHigh3Enabled) {
                            if (obNativeAdHigh3 != null && obNativeHigh3Enabled) {
                                displayNative(
                                    obNativeAdHigh3,
                                    binding?.nativeAdView,
                                    mActivity,
                                    binding?.shimmerContainerNative?.shimmerContainerNative!!
                                )
                            } else if (obNativeAd3 != null && obNative3Enabled) {
                                displayNative(
                                    obNativeAd3,
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
                if (!obNativeHigh3Enabled && !obNative3Enabled) {
                    binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
                }
            } else {
                binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
            }

            binding?.btnNextOnboarding?.setOnClickListener {
                FirebaseLogUtils.logEvent("onboarding_2_next", "")
                nav.goNext()
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
