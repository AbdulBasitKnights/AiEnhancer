package com.aiface.aging.features.home

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.AiFaceApp
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.HomeNativeAdManager
import com.aiface.aging.shared.ads.HomeNativeAdParentManager
import com.aiface.aging.databinding.ItemHomeAiParentBinding
import com.aiface.aging.databinding.ItemHomeBannerBinding
import com.aiface.aging.databinding.ItemHomeCategoryShimmerBinding
import com.aiface.aging.databinding.ItemHomeImageToImageBinding
import com.aiface.aging.databinding.ItemHomeOfflineBinding
import com.aiface.aging.databinding.LayoutNativeAdHomeBinding
import com.aiface.aging.domain.model.Category
import com.aiface.aging.domain.model.Template
import com.aiface.aging.features.home.carousel.HomeCarouselSectionViewHolder
import com.aiface.aging.features.home.carousel.HomePromoBannerViewHolder
import com.aiface.aging.features.home.foryou.HomeForYouSectionViewHolder
import com.aiface.aging.features.tools.ToolsFeature
import com.aiface.aging.shared.setSafeClickListener

class HomeAdapter(
    private val context: FragmentActivity,
    private val onBannerClick: (BannerModel) -> Unit,
    private val onImageToImageClick: () -> Unit,
    private val onTemplateClick: (Template, String, String) -> Unit,
    private val onSeeAllClick: (Category) -> Unit,
    private val onToolsSeeAllClick: () -> Unit,
    private val onToolClick: (ToolsFeature) -> Unit,
    private val onCarouselSeeAllClick: () -> Unit = {},
    private val onCarouselItemClick: (HomeCarouselItem) -> Unit = {},
    private val onPromoClick: () -> Unit = {},
    private val onOfflineRetryClick: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
       // const val VIEW_TYPE_BANNER = 0
        const val VIEW_TYPE_IMAGE_TO_IMAGE = 0
        const val VIEW_TYPE_CATEGORY_SHIMMER = 1
        const val VIEW_TYPE_OFFLINE = 2
        const val VIEW_TYPE_CATEGORY = 3
        const val VIEW_TYPE_NATIVE_AD = 4
        const val VIEW_TYPE_TOOLS = 5
        const val VIEW_TYPE_CAROUSEL = 6
        const val VIEW_TYPE_PROMO = 7

        /** Insert native ad after 1st content row, then after every 3rd row (3rd, 6th, 9th, …). */
        private fun shouldInsertNativeAdAfterContentIndex(index: Int, lastIndex: Int): Boolean {
            /*if (index == lastIndex) return false
            return (index + 5) % 2 == 0*/
            return false
        }
    }

    private val items = mutableListOf<HomeItem>()
    private var contentItems: List<HomeItem> = emptyList()
    var onItemsRebuilt: (() -> Unit)? = null

    private fun shouldInsertParentNativeAds(): Boolean {
        if (!AdsHelper.shouldShowAds()) return false
        if (!AiFaceApp.isNativeHome) return false
        return HomeNativeAdParentManager.getNativeAds().isNotEmpty()
    }

    fun notifyParentNativeAdsChanged() {
        rebuildItems()
    }

    private fun rebuildItems() {
        items.clear()

        val tempList = mutableListOf<HomeItem>()
        val sourceItems = contentItems

        sourceItems.forEachIndexed { index, item ->
            tempList.add(item)

            if (!shouldInsertParentNativeAds()) return@forEachIndexed

            if (shouldInsertNativeAdAfterContentIndex(index, sourceItems.lastIndex)) {
                tempList.add(HomeItem.NativeAdItem())
            }
        }

        if (tempList.lastOrNull() is HomeItem.NativeAdItem) {
            tempList.removeAt(tempList.lastIndex)
        }

        items.addAll(tempList)
        notifyDataSetChanged()
        onItemsRebuilt?.invoke()
    }

/*    fun submitItems(newItems: List<HomeItem>) {

        items.clear()

        val tempList = mutableListOf<HomeItem>()

        newItems.forEachIndexed { index, item ->

            tempList.add(item)

            val shouldAddAd =
                (index + 1) % 3 == 0 &&
                        index != newItems.lastIndex

            if (shouldAddAd) {
                tempList.add(HomeItem.NativeAdItem())
            }
        }

        // Safety
        if (tempList.lastOrNull() is HomeItem.NativeAdItem) {
            tempList.removeLast()
        }

        items.addAll(tempList)

        notifyDataSetChanged()
    }*/

    fun submitItems(newItems: List<HomeItem>) {
        contentItems = newItems.filter { it !is HomeItem.NativeAdItem }
        rebuildItems()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
          //  is HomeItem.Banner -> VIEW_TYPE_BANNER
            is HomeItem.ImageToImageButton -> VIEW_TYPE_IMAGE_TO_IMAGE
            is HomeItem.CategoryShimmer -> VIEW_TYPE_CATEGORY_SHIMMER
            is HomeItem.OfflineMessage -> VIEW_TYPE_OFFLINE
            is HomeItem.CategoryItem -> VIEW_TYPE_CATEGORY
            is HomeItem.NativeAdItem -> VIEW_TYPE_NATIVE_AD
            is HomeItem.ToolsSection -> VIEW_TYPE_TOOLS
            is HomeItem.CarouselSection -> VIEW_TYPE_CAROUSEL
            is HomeItem.PromoBanner -> VIEW_TYPE_PROMO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
//            VIEW_TYPE_BANNER -> {
//                val binding = ItemHomeBannerBinding.inflate(inflater, parent, false)
//                BannerViewHolder(binding)
//            }

            VIEW_TYPE_IMAGE_TO_IMAGE -> {
                val binding = ItemHomeImageToImageBinding.inflate(inflater, parent, false)
                ImageToImageViewHolder(binding)
            }

            VIEW_TYPE_CATEGORY_SHIMMER -> {
                val binding = ItemHomeCategoryShimmerBinding.inflate(inflater, parent, false)
                CategoryShimmerViewHolder(binding)
            }

            VIEW_TYPE_OFFLINE -> {
                val binding = ItemHomeOfflineBinding.inflate(inflater, parent, false)
                OfflineViewHolder(binding)
            }

            VIEW_TYPE_CATEGORY -> {
                val binding = ItemHomeAiParentBinding.inflate(inflater, parent, false)
                CategorySectionViewHolder(binding)
            }
            VIEW_TYPE_TOOLS -> {
                HomeForYouSectionViewHolder.create(
                    parent = parent,
                    onSeeAllClick = onToolsSeeAllClick,
                    onToolClick = onToolClick,
                )
            }
            VIEW_TYPE_CAROUSEL -> {
                HomeCarouselSectionViewHolder.create(
                    parent = parent,
                    onSeeAllClick = onCarouselSeeAllClick,
                    onItemClick = onCarouselItemClick,
                )
            }
            VIEW_TYPE_PROMO -> {
                HomePromoBannerViewHolder.create(
                    parent = parent,
                    onCtaClick = onPromoClick,
                )
            }
            VIEW_TYPE_NATIVE_AD -> {

                val binding = LayoutNativeAdHomeBinding.inflate(
                    inflater,
                    parent,
                    false
                )

                NativeAdViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unsupported viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
           // is HomeItem.Banner -> (holder as BannerViewHolder).bind(item)
            is HomeItem.ImageToImageButton -> (holder as ImageToImageViewHolder).bind(item)
            is HomeItem.CategoryShimmer -> (holder as CategoryShimmerViewHolder).bind()
            is HomeItem.OfflineMessage -> (holder as OfflineViewHolder).bind(item)
            is HomeItem.CategoryItem -> (holder as CategorySectionViewHolder).bind(item.category)
            is HomeItem.ToolsSection -> (holder as HomeForYouSectionViewHolder).bind(item.tools)
            is HomeItem.CarouselSection -> (holder as HomeCarouselSectionViewHolder).bind(item)
            is HomeItem.PromoBanner -> (holder as HomePromoBannerViewHolder).bind(item)
            is HomeItem.NativeAdItem -> (holder as NativeAdViewHolder).bind()
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is BannerViewHolder) {
            holder.startAutoScroll()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is BannerViewHolder) {
            holder.stopAutoScroll()
        }
    }

    inner class BannerViewHolder(
        private val binding: ItemHomeBannerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val handler = Handler(Looper.getMainLooper())
        private val autoScrollRunnable = object : Runnable {
            override fun run() {
                val currentItem = binding.viewPagerBanner.currentItem
                val itemCount = binding.viewPagerBanner.adapter?.itemCount ?: 0
                if (itemCount > 1) {
                    val nextItem = (currentItem + 1) % itemCount
                    binding.viewPagerBanner.setCurrentItem(nextItem, true)
                }
                handler.postDelayed(this, 4000)
            }
        }

        fun startAutoScroll() {
            handler.removeCallbacks(autoScrollRunnable)
            handler.postDelayed(autoScrollRunnable, 3000)
        }

        fun stopAutoScroll() {
            handler.removeCallbacks(autoScrollRunnable)
        }
    }

    inner class ImageToImageViewHolder(
        private val binding: ItemHomeImageToImageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeItem.ImageToImageButton) {
            binding.btnImageToImage.setOnClickListener {
                onImageToImageClick()
            }
        }
    }




    inner class CategorySectionViewHolder(
        private val binding: ItemHomeAiParentBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {


            binding.tvCategoryTitle.text = category.name

            val adapter = CategoryAdapter(
                onTemplateClick,
                category.name,
                category.id,
                context
            )

            binding.rvTemplates.adapter = adapter

            adapter.submitList(
                category.templates.take(5),
                HomeNativeAdManager.getNativeAds()
            )


            binding.btnSeeAll.setSafeClickListener {
                onSeeAllClick(category)
            }
        }
    }

    inner class CategoryShimmerViewHolder(
        private val binding: ItemHomeCategoryShimmerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() = Unit
    }

    inner class OfflineViewHolder(
        private val binding: ItemHomeOfflineBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeItem.OfflineMessage) {
            binding.tvOffline.text = item.text
            binding.root.setOnClickListener { onOfflineRetryClick() }
            binding.tvOffline.setOnClickListener { onOfflineRetryClick() }
        }
    }

    inner class NativeAdViewHolder(
        private val binding: LayoutNativeAdHomeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            val layoutParams = binding.root.layoutParams as? RecyclerView.LayoutParams
                ?: RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )

            if (!AdsHelper.shouldShowAds()) {
                HomeNativeAdParentManager.destroyAds()
                collapseNativeAdRow(layoutParams)
                return
            }

            val nativeAd = HomeNativeAdParentManager
                .getNativeAds()
                .randomOrNull()

            if (nativeAd == null) {
                collapseNativeAdRow(layoutParams)
                return
            }

            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.root.layoutParams = layoutParams
            binding.root.visibility = View.VISIBLE

            val adView = binding.root

            adView.headlineView = binding.adHeadline
            adView.bodyView = binding.adBody
            adView.callToActionView = binding.adCallToAction

            binding.adHeadline.text = nativeAd.headline
            binding.adBody.text = nativeAd.body
            binding.adCallToAction.text = nativeAd.callToAction

            adView.registerNativeAd(nativeAd, binding.adMedia)
        }

        private fun collapseNativeAdRow(layoutParams: RecyclerView.LayoutParams) {
            binding.root.visibility = View.GONE
            layoutParams.height = 0
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.topMargin = 0
            layoutParams.bottomMargin = 0
            binding.root.layoutParams = layoutParams
        }
    }
}
