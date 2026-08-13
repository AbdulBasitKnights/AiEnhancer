package com.aiface.aging.features.tools

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentToolsBinding
import com.aiface.aging.features.home.HomeFragment
import com.aiface.aging.features.home.HomeViewModel
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.setSafeClickListener
import com.aiface.aging.utils.FirebaseLogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ToolsFragment : Fragment() {

    private var binding: FragmentToolsBinding? = null
    private var mActivity: FragmentActivity? = null
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            FirebaseLogUtils.logEvent("tools_view", "")
            setupHeaderActions(activity)
            setupToolsGrid(activity)
        }
    }

    private fun setupHeaderActions(activity: FragmentActivity) {
        if (isProVersion.value == true) {
            binding?.btnPro?.visibility = View.GONE
        }
        binding?.btnPro?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_premium_click", "")
            startActivity(Intent(activity, IAPActivity::class.java))
        }
        binding?.btnSetting?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_setting_click", "")
            try {
                findNavController().navigate(R.id.actionHomeToSettings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupToolsGrid(activity: FragmentActivity) {
        lifecycleScope.launch {
            viewModel.getToolsFeatureList(activity).collectLatest { list ->
                val adapter =
                    ToolsFeatureAdapter(list) { item ->
                        resolveHomeFragment()?.handleToolsFeatureClick(item, activity)
                    }
                binding?.rvTools?.layoutManager = GridLayoutManager(activity, 2)
                binding?.rvTools?.adapter = adapter
            }
        }
    }

    private fun resolveHomeFragment(): HomeFragment? {
        return (parentFragment as? com.aiface.aging.features.main.MainFragment)
            ?.getHomeFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
