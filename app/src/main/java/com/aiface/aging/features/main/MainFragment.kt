package com.aiface.aging.features.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.aiface.aging.R
import com.aiface.aging.features.home.HomeFragment
import com.aiface.aging.ads_nextgen.ProductAnalytics
import com.aiface.aging.ui.glassnav.MainGlassShell
import com.aiface.aging.ui.glassnav.TAB_AI_VIDEO
import com.aiface.aging.ui.glassnav.TAB_HOME
import com.aiface.aging.ui.glassnav.TAB_LIBRARY
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.FirebaseLogUtils

class MainFragment : Fragment() {
    private var mActivity: FragmentActivity? = null
    private var viewPager: ViewPager2? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (savedInstanceState == null) {
            resetToHomeTab()
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val pagerTab by selectedItem.observeAsState(PAGER_HOME)
                val navTab by navSelectedTab.observeAsState(TAB_HOME)

                MainGlassShell(
                    selectedTab = navTab,
                    onHomeClick = {
                        FirebaseLogUtils.logEvent("bottom_nav_home_click", "")
                        openHomeTab()
                    },
                    onLibraryClick = {
                        FirebaseLogUtils.logEvent("bottom_nav_library_click", "")
                        openLibraryTab()
                    },
                    onAiVideoClick = {
                        FirebaseLogUtils.logEvent("bottom_nav_ai_video_click", "")
                        openAiVideoTab()
                    },
                ) {
                    AndroidView(
                        factory = { ctx -> createViewPager(ctx) },
                        update = { pager ->
                            val tab = pagerTab.coerceIn(PAGER_HOME, PAGER_LIBRARY)
                            if (pager.currentItem != tab) {
                                pager.setCurrentItem(tab, false)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun createViewPager(context: Context): ViewPager2 {
        return ViewPager2(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            adapter = MainPagerAdapter(childFragmentManager, lifecycle)
            isUserInputEnabled = false
            offscreenPageLimit = 3
            setCurrentItem(selectedItem.value ?: PAGER_HOME, false)
            viewPager = this
        }
    }

    companion object {
        const val PAGER_HOME = 0
        const val PAGER_AI_VIDEO = 1
        const val PAGER_LIBRARY = 2

        var selectedItem = MutableLiveData(PAGER_HOME)
        var navSelectedTab = MutableLiveData(TAB_HOME)
        var requestLibraryPhotosTab = MutableLiveData(false)
    }

    private fun resetToHomeTab() {
        selectedItem.value = PAGER_HOME
        navSelectedTab.value = TAB_HOME
        requestLibraryPhotosTab.value = false
    }

    fun openHomeTab() {
        navSelectedTab.value = TAB_HOME
        selectPagerTab(PAGER_HOME)
    }

    fun openLibraryTab() {
        navSelectedTab.value = TAB_LIBRARY
        requestLibraryPhotosTab.value = true
        selectPagerTab(PAGER_LIBRARY)
    }

    fun openAiVideoTab() {
        navSelectedTab.value = TAB_AI_VIDEO
        selectPagerTab(PAGER_AI_VIDEO)
        FirebaseLogUtils.logEvent(ProductAnalytics.AI_VIDEO_VIEW, "")
    }

    fun selectTab(tab: Int) {
        selectPagerTab(tab)
    }

    private fun selectPagerTab(tab: Int) {
        selectedItem.value = tab.coerceIn(PAGER_HOME, PAGER_LIBRARY)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            AppUtils.showHomeBannerAd(activity)
            FirebaseLogUtils.logEvent("home_view", "")
        }
    }

    fun openSettings() {
        try {
            findNavController().navigate(MainFragmentDirections.actionHomeToSettings())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHomeFragment(): HomeFragment? {
        return childFragmentManager.fragments.filterIsInstance<HomeFragment>().firstOrNull()
    }

    fun openAging() {
        // AI Aging entry removed — no-op.
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
        viewPager = null
    }
}
