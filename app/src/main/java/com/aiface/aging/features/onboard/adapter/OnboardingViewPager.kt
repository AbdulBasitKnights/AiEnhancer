package com.aiface.aging.features.onboard.adapter

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.obNativeAdFullScr1
import com.aiface.aging.shared.ads.AdsHelper.obNativeAdFullScr2
import com.aiface.aging.shared.ads.AdsHelper.obNativeAdHighFullScr1
import com.aiface.aging.shared.ads.AdsHelper.obNativeAdHighFullScr2
import com.aiface.aging.features.onboard.FragmentOnboardFifth
import com.aiface.aging.features.onboard.FragmentOnboardFirst
import com.aiface.aging.features.onboard.FragmentOnboardFourth
import com.aiface.aging.features.onboard.FragmentOnboardSecond
import com.aiface.aging.features.onboard.FragmentOnboardThird
import com.aiface.aging.features.onboard.OnBoardingFullScr1
import com.aiface.aging.features.onboard.OnBoardingFullScr2
import kotlin.reflect.KClass

class OnboardingViewPager(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private enum class Page(
        val clazz: KClass<out Fragment>,
        val id: Long,
        val factory: () -> Fragment
    ) {
        HALF_1(FragmentOnboardFirst::class, 1L, { FragmentOnboardFirst() }),
        HALF_2(FragmentOnboardSecond::class, 2L, { FragmentOnboardSecond() }),
        FULL_NATIVE_1(OnBoardingFullScr1::class, 3L, { OnBoardingFullScr1() }),
        FULL_ONBOARD_3(FragmentOnboardThird::class, 4L, { FragmentOnboardThird() }),
        FULL_NATIVE_2(OnBoardingFullScr2::class, 5L, { OnBoardingFullScr2() }),
        FULL_ONBOARD_4(FragmentOnboardFourth::class, 6L, { FragmentOnboardFourth() }),
        FULL_NATIVE_3(FragmentOnboardFifth::class, 7L, { FragmentOnboardFifth() }),
    }

    private val pages = mutableListOf<Page>()

    init {
        rebuildPages()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        val before = pages.toList()
        rebuildPages()
        if (before != pages) notifyDataSetChanged()
    }

    private fun rebuildPages() {
        pages.clear()
        // 1. Half onboarding + native
        if (AdsHelper.obFirstEnable) {
            pages += Page.HALF_1
        }
        // 2. Half onboarding + native
        if (AdsHelper.obSecondEnable) {
            pages += Page.HALF_2
        }
        // 3. Full-screen native
        if (obNativeAdHighFullScr1 != null || obNativeAdFullScr1 != null) {
            pages += Page.FULL_NATIVE_1
        }
        // 4. Full-screen onboarding
        if (AdsHelper.obThirdEnable) {
            pages += Page.FULL_ONBOARD_3
        }
        // 5. Full-screen native
        if (obNativeAdHighFullScr2 != null || obNativeAdFullScr2 != null) {
            pages += Page.FULL_NATIVE_2
        }
        // 6. Full-screen onboarding
        if (AdsHelper.obFourthEnable) {
            pages += Page.FULL_ONBOARD_4
        }
       /* // 7. Full-screen native (exit + interstitial)
        if (AdsHelper.obFifthEnable) {
            pages += Page.FULL_NATIVE_3
        }*/
    }

    override fun getItemCount(): Int = pages.size
    override fun createFragment(position: Int): Fragment = pages[position].factory()
    override fun getItemId(position: Int): Long = pages[position].id
    override fun containsItem(itemId: Long): Boolean = pages.any { it.id == itemId }
}
