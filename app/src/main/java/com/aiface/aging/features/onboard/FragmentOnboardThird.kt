package com.aiface.aging.features.onboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentOnbaordingThirdBinding
import com.aiface.aging.features.onboard.adapter.PagerNav
import com.aiface.aging.utils.FirebaseLogUtils

class FragmentOnboardThird : Fragment() {

    private var binding: FragmentOnbaordingThirdBinding? = null

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
        binding = FragmentOnbaordingThirdBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            FirebaseLogUtils.logEvent("onboarding_3_view", "")

            if (AiFaceApp.isSolidObButton) {
                binding?.btnNextOnboarding?.setBackgroundResource(R.drawable.bg_selected_card)
                binding?.btnNextOnboarding?.setTextColor(ContextCompat.getColor(activity, R.color.white))
            }

            binding?.btnNextOnboarding?.setOnClickListener {
                FirebaseLogUtils.logEvent("onboarding_3_next", "")
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
