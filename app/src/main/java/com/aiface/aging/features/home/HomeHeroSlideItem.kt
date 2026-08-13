package com.aiface.aging.features.home

import androidx.annotation.DrawableRes
import com.aiface.aging.R

enum class HomeHeroSlideAction {
    AGING,
    SWAP,
}

/**
 * One hero banner slide on the home screen.
 * Swap drawable resources here for each slide's artwork and dot strip.
 */
data class HomeHeroSlideItem(
    @DrawableRes val backgroundImageRes: Int,
    val type: String,
    val title: String,
    val description: String,
    val action: HomeHeroSlideAction,
    val analyticsEvent: String,
)

object HomeHeroSlides {

    fun slides(): List<HomeHeroSlideItem> =
        listOf(
            HomeHeroSlideItem(
                backgroundImageRes = R.drawable.top_slider_1,
                type = "See my Future",
                title = "Meet Your Future",
                description = "AI predicts your age.",
                action = HomeHeroSlideAction.AGING,
                analyticsEvent = "home_aging_click",
            ),
            HomeHeroSlideItem(
                backgroundImageRes = R.drawable.top_slider_2,
                type = "Start Swapping",
                title = "Create Any Identity",
                description = "Swap into any character with AI",
                action = HomeHeroSlideAction.SWAP,
                analyticsEvent = "home_swap_click",
            ),
        )
}
