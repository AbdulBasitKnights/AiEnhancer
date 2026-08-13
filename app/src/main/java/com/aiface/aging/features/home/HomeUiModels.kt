package com.aiface.aging.features.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aiface.aging.domain.model.Category
import com.aiface.aging.features.tools.ToolsFeature

data class BannerModel(
    val title: String,
    val imageRes: Int,
    val prompt: String,
)

data class HomeCarouselItem(
    val id: String,
    val title: String,
    @DrawableRes val imageRes: Int,
)

sealed class HomeItem {
    data class ImageToImageButton(val title: String) : HomeItem()
    data class CategoryShimmer(val count: Int = 6) : HomeItem()
    data class OfflineMessage(val text: String = "Offline") : HomeItem()
    data class CategoryItem(val category: Category) : HomeItem()
    data class ToolsSection(val tools: List<ToolsFeature>) : HomeItem()
    data class CarouselSection(
        @StringRes val titleRes: Int,
        val items: List<HomeCarouselItem>,
    ) : HomeItem()

    data class PromoBanner(
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int,
        @StringRes val ctaRes: Int,
        @DrawableRes val imageRes: Int,
    ) : HomeItem()

    data class NativeAdItem(
        val dummy: Int = 0,
    ) : HomeItem()
}
