package com.aiface.aging.features.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aiface.aging.features.aivideo.AiVideoCatalogFragment
import com.aiface.aging.features.home.HomeFragment
import com.aiface.aging.features.mywork.FragmentMyWork

class MainPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            MainFragment.PAGER_HOME -> HomeFragment()
            MainFragment.PAGER_AI_VIDEO -> AiVideoCatalogFragment()
            MainFragment.PAGER_LIBRARY -> FragmentMyWork()
            else -> HomeFragment()
        }
    }
}
