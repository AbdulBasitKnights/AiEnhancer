package com.aiface.aging.features.home

import android.os.Handler
import android.os.Looper
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.shared.ads.RewardItem
import com.aiface.aging.shared.ads.showRewardedNextGen
import com.aiface.aging.shared.ads.trackedUnitId
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.AiFaceApp.Companion.bottom_nav_inter
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.HomeNativeAdManager
import com.aiface.aging.shared.ads.HomeNativeAdParentManager
import com.aiface.aging.shared.ads.canPresentHomeInterstitial
import com.aiface.aging.shared.ads.GenerationRewardGate
import com.aiface.aging.shared.ads.interstitialHome
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.shared.ads.loadInterHome
import com.aiface.aging.shared.ads.loadInterHomeHigh
import com.aiface.aging.shared.ClickGuard
import com.aiface.aging.shared.goUTM
import com.aiface.aging.shared.safeNavigate
import com.aiface.aging.databinding.FragmentHomeNBinding
import com.aiface.aging.features.credits.DailyCheckInDialogFragment
import com.aiface.aging.features.credits.DailyCheckInViewModel
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.features.tools.ToolsFeature
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.isRewarded
import com.aiface.aging.shared.ads.loadRewardedAd
import com.aiface.aging.shared.ads.trackedUnitId
import com.aiface.aging.shared.ads.rewardedAd
import com.aiface.aging.shared.setSafeClickListener
import com.aiface.aging.utils.BitmapMemoryUtils
import com.aiface.aging.utils.DialogUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var binding: FragmentHomeNBinding? = null

    private var mActivity: FragmentActivity? = null

    private val viewModel: HomeViewModel by activityViewModels()
    private val checkInViewModel: DailyCheckInViewModel by activityViewModels()

    private lateinit var homeAdapter: HomeAdapter

    private var rewardDialog: Dialog? = null



    companion object {
        var requestPermission = MutableLiveData(false)
        var isProClosedAfterReward = MutableLiveData(false)

        private var proPanelHomeObject: ProPanelHomeObject? = null

        private var homeItemsList = arrayListOf<HomeItem>()
    }


    /** Prevents double navigate after inter continue/fail callbacks. */
    private var previewNavInFlight = false

/*    private val heroSlides = HomeHeroSlides.slides()
    private var currentHeroSlideIndex = 0
    private val heroSliderHandler = Handler(Looper.getMainLooper())
    private val heroAutoScrollIntervalMs = 4_000L
    private var heroPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private val heroAutoScrollRunnable =
        object : Runnable {
            override fun run() {
                if (heroSlides.size <= 1) return
                val next = (currentHeroSlideIndex + 1) % heroSlides.size
                binding?.heroViewPager?.setCurrentItem(next, true)
                heroSliderHandler.postDelayed(this, heroAutoScrollIntervalMs)
            }
        }*/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeNBinding.inflate(inflater, container, false)
        return binding?.root
    }

    // Define the function you want to run every 2 seconds
    fun runEveryTwoSeconds(activity: FragmentActivity) {
        val handler = Handler(Looper.getMainLooper())

        // Create a runnable that will execute your function
        val runnable = object : Runnable {
            override fun run() {

                if (!NetworkUtils.isOnline(activity)) {
                    handler.postDelayed(this, 2000)
                } else {
                    viewModel.getHomeItems(forceRefresh = true)
                }
            }
        }

        // Start the first execution
        handler.post(runnable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity?.let { activity ->

            if (!NetworkUtils.isOnline(activity)) {

                runEveryTwoSeconds(activity)
            }

            // Pulse animation removed — CTA lives inside hero page now.
            setupHeroSlider(activity)
            loadAds(activity)
            setFeatureRecycler(activity)

            setupRecycler(activity)
            observeViewModel()
            val viewModelCategories = viewModel.theHomeItems.value
            if (viewModelCategories?.any { it is HomeItem.CategoryItem } == true) {
                homeItemsList.clear()
                homeItemsList.addAll(viewModelCategories)
                updateTemplatesSection(viewModelCategories)
            } else {
                homeItemsList.clear()
                showInitialTemplatesShimmer()
                viewModel.getHomeItems()
            }

            if (isProVersion.value == false && AiFaceApp.isRewardHome) {
                loadRewardedAd(
                    activity,
                    BuildConfig.reward_home_hf,
                    BuildConfig.reward_home,
                    AiFaceApp.isRewardHomeHf,
                    AiFaceApp.isRewardHome
                ) { onLoaded ->
                    //   isRewardLoaded = onLoaded

                }
            }

            isProClosedAfterReward.observe(viewLifecycleOwner, Observer {
                if (it) {
                    isProClosedAfterReward.value = false
                    navigateHomeAction(
                        R.id.action_homeFragment_to_previewFragment,
                        Bundle().apply {
                            putString("item_id", proPanelHomeObject?.item_id)
                            putString("prompt", proPanelHomeObject?.prompt.orEmpty())
                            putString("url", proPanelHomeObject?.url)
                            putString("category_name", proPanelHomeObject?.category_name)
                            putString("category_id", proPanelHomeObject?.category_id)
                            putString("title", proPanelHomeObject?.title)
                            putInt(
                                TemplateImageRequirements.ARG_IMAGE_COUNT,
                                TemplateImageRequirements.requiredCount(proPanelHomeObject?.image_count),
                            )
                        },
                    )
                }
            })
        }

    }


    private fun setupRecycler(activity: FragmentActivity) {
        homeAdapter =
            HomeAdapter(
                requireActivity(),
                onBannerClick = { banner ->
                    val bundle = Bundle().apply {
                        putString("prompt", banner.prompt)
                    }
                    //  findNavController().navigate(R.id.action_homeFragment_to_imageToImageFragment, bundle)
                },
                onImageToImageClick = {
                    //    findNavController().navigate(R.id.action_homeFragment_to_imageToImageFragment)
                },

                onTemplateClick = { template, categoryName, categoryId ->
                    FirebaseLogUtils.logEvent("home_ai_photos_generate_click", "")
                    Log.e(
                        "HomeThumbUrl",
                        "click title=${template.title} id=${template.id} " +
                            "category=$categoryName($categoryId) " +
                            "thumbnailUrl=${template.thumbnailUrl} " +
                            "mediaUrl=${template.mediaUrl} " +
                            "gifUrl=${template.gifUrl} " +
                            "thumbBlank=${template.thumbnailUrl.isNullOrBlank()} " +
                            "mediaBlank=${template.mediaUrl.isNullOrBlank()}",
                    )

                    val openPreview = {
                        navigateHomeAction(
                            R.id.action_homeFragment_to_previewFragment,
                            Bundle().apply {
                                putString("item_id", template.id)
                                putString("prompt", template.prompt.orEmpty())
                                putString(
                                    "url",
                                    template.thumbnailUrl ?: template.mediaUrl.orEmpty(),
                                )
                                putString("category_name", categoryName)
                                putString("category_id", categoryId)
                                putString("title", template.title)
                                putInt(
                                    TemplateImageRequirements.ARG_IMAGE_COUNT,
                                    TemplateImageRequirements.requiredCount(template.imageCount),
                                )
                            },
                        )
                    }

                    GenerationRewardGate.gateHomePremiumTemplate(
                        activity = activity,
                        isPremiumItem = template.isPro,
                        onContinue = {
                            if (template.isPro && isProVersion.value == true) {
                                openPreview()
                            } else {

                                showAdThenNavigatePreview(activity, openPreview)
                            }
                        },
                        onOpenIap = {
                            val ob = ProPanelHomeObject(
                                template.id,
                                template.prompt.orEmpty(),
                                template.thumbnailUrl ?: template.mediaUrl.orEmpty(),
                                categoryName,
                                template.title.orEmpty(),
                                categoryId,
                                TemplateImageRequirements.requiredCount(template.imageCount),
                            )
                            proPanelHomeObject = ob
                            val theIntent = Intent(activity, IAPActivity::class.java)
                            theIntent.putExtra("isFromProPanel", true)
                            theIntent.putExtra("isFromHome", true)
                            startActivity(theIntent)
                        },
                    )
                },
                onSeeAllClick = { category ->
                    FirebaseLogUtils.logEvent("home_ai_photos_see_all_click", "")
                    if(AiFaceApp.isInterHome) {
                        showAdThenNavigatePreview(activity) {
                            navigateHomeAction(
                                R.id.action_homeFragment_to_seeAllFragment,
                                Bundle().apply {
                                    putString("category_id", category.id)
                                    putString("category_name", category.name)
                                },
                            )
                        }
                    }
                    else{
                        navigateHomeAction(
                            R.id.action_homeFragment_to_seeAllFragment,
                            Bundle().apply {
                                putString("category_id", category.id)
                                putString("category_name", category.name)
                            },
                        )
                    }

                },
                onToolsSeeAllClick = {
                    FirebaseLogUtils.logEvent("home_tools_see_all_click", "")
                },
                onToolClick = { tool ->
                    handleToolsFeatureClick(tool, activity)
                },
                onCarouselSeeAllClick = {
                    Toast.makeText(activity, R.string.coming_soon, Toast.LENGTH_SHORT).show()
                },
                onCarouselItemClick = {
                    Toast.makeText(activity, R.string.coming_soon, Toast.LENGTH_SHORT).show()
                },
                onPromoClick = {
                    FirebaseLogUtils.logEvent("home_promo_eraser_click", "")
                    showInterForGallery(activity, "magicEraser")
                },

            )
        homeAdapter.onItemsRebuilt = {
            binding?.rvHomeImgFeature?.let { recyclerView ->
                recyclerView.post { recyclerView.requestLayout() }
            }
        }
        // Pro badge always visible (status chip matching home design).
        if(isProVersion.value!=true){
            binding?.btnPro?.visibility = View.VISIBLE
        }
        else{
            binding?.btnPro?.visibility = View.GONE
        }
        binding?.btnPro?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_premium_click", "")
            startActivity(Intent(activity, IAPActivity::class.java))
        }
        binding?.btnSetting?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_setting_click", "")
            navigateHomeAction(R.id.actionHomeToSettings)
        }
        binding?.btnHomeEraser?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_click_magic_eraser", "")
            showInterForGallery(activity, "magicEraser")
        }
        binding?.btnHomeEnhance?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_click_enhancer", "")
            navigateToStandaloneAiPreview(
                activity = activity,
                itemId = HomeAiTemplateResolver.enhancerTemplateId(homeItemsList),
                prompt = HomeAiTemplateResolver.enhancerPrompt(homeItemsList),
                url = "enhancer",
                categoryName = getString(R.string.photo_enhancer),
                title = "Enhancer",
            )
        }
        binding?.btnHomeRemoveBg?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_click_bg_remover", "")
            showInterForGallery(activity, "bgRemover")
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.credits.collectLatest { credits ->
                binding?.tvCredits?.text = credits.toString()
            }
        }
        binding?.tvCredits?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_credits_click", "")
            showDailyCheckInDialog()
        }
        // Legacy aging CTAs (gone in layout) — no-op so they cannot launch dropped AI Aging.
        binding?.btnAiAge?.setOnClickListener(null)
        binding?.btnAiImg?.setOnClickListener(null)
        binding?.seeAll?.setSafeClickListener {
            FirebaseLogUtils.logEvent("home_tools_see_all_click", "")
        }
        binding?.rvHomeImgFeature?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeAdapter
            setHasFixedSize(false)
        }

        if (AdsHelper.shouldShowAds()) {
            if (AiFaceApp.isNativeHomeHf && AiFaceApp.isNativeHome) {

                HomeNativeAdParentManager.loadNativeAds(
                    context = activity,
                    adUnitId = BuildConfig.native_home_hf,
                    count = 1,
                    isHighFloor = true,
                    onLoaded = { ads ->

                        homeAdapter.notifyParentNativeAdsChanged()
                    },

                    onLoadFailed = {

                        HomeNativeAdParentManager.loadNativeAds(
                            context = activity,
                            adUnitId = BuildConfig.native_home,
                            count = 2,
                            isHighFloor = false,
                            onLoaded = { ads ->
                                homeAdapter.notifyParentNativeAdsChanged()
                            },

                            onLoadFailed = {
                                homeAdapter.notifyParentNativeAdsChanged()
                            }
                        )
                    }
                )
            } else if (AiFaceApp.isNativeHome) {
                HomeNativeAdParentManager.loadNativeAds(
                    context = activity,
                    adUnitId = BuildConfig.native_home,
                    count = 2,
                    isHighFloor = false,
                    onLoaded = { ads ->
                        homeAdapter.notifyParentNativeAdsChanged()
                    },

                    onLoadFailed = {
                        homeAdapter.notifyParentNativeAdsChanged()
                    }
                )
            }
        }

        if (AdsHelper.shouldShowAds()) {
            if (AiFaceApp.isNativeHomeChildHf && AiFaceApp.isNativeHomeChild) {

                HomeNativeAdManager.loadNativeAds(
                    context = activity,
                    adUnitId = BuildConfig.native_share_hf,
                    count = 1,
                    isHighFloor = true,
                    onLoaded = { ads ->

                        homeAdapter.notifyDataSetChanged()
                    },

                    onLoadFailed = {

                        HomeNativeAdManager.loadNativeAds(
                            context = activity,
                            adUnitId = BuildConfig.native_share,
                            count = 2,
                            isHighFloor = false,
                            onLoaded = { ads ->
                                homeAdapter.notifyDataSetChanged()
                            },

                            onLoadFailed = {

                            }
                        )
                    }
                )
            } else if (AiFaceApp.isNativeHomeChild) {
                HomeNativeAdManager.loadNativeAds(
                    context = activity,
                    adUnitId = BuildConfig.native_share,
                    count = 2,
                    isHighFloor = false,
                    onLoaded = { ads ->
                        homeAdapter.notifyDataSetChanged()
                    },

                    onLoadFailed = {

                    }
                )
            }
        }
    }


    private fun showInitialTemplatesShimmer() {
        updateTemplatesSection(
            listOf(
                HomeItem.CategoryShimmer(),
                HomeItem.CategoryShimmer(),
                HomeItem.CategoryShimmer(),
            ),
        )
    }

    private fun updateTemplatesSection(items: List<HomeItem>) {
        val displayItems = viewModel.buildHomeDisplayList(items)
        homeAdapter.submitItems(displayItems)
        val shouldShow =
            displayItems.any {
                it is HomeItem.CategoryItem ||
                    it is HomeItem.CategoryShimmer ||
                    it is HomeItem.OfflineMessage ||
                    it is HomeItem.ToolsSection ||
                    it is HomeItem.PromoBanner
            }
        binding?.rvHomeImgFeature?.apply {
            visibility = if (shouldShow) View.VISIBLE else View.GONE
            if (shouldShow) {
                post { this@apply.requestLayout() }
            }
        }
    }

    private fun showComingSoon(featureNameRes: Int) {
        val activity = mActivity ?: return
        (activity as? com.aiface.aging.MainActivity)?.showComingSoon(featureNameRes)
            ?: Toast.makeText(
                activity,
                getString(R.string.coming_soon_feature, getString(featureNameRes)),
                Toast.LENGTH_SHORT,
            ).show()
    }

    private fun observeViewModel() {
        viewModel.theHomeItems.observe(viewLifecycleOwner) { items ->
            if (items == null) return@observe

            val hasCachedCategories = homeItemsList.any { it is HomeItem.CategoryItem }
            val hasIncomingCategories = items.any { it is HomeItem.CategoryItem }
            val isIncomingOnlyShimmer =
                items.isNotEmpty() &&
                    items.all { it is HomeItem.CategoryShimmer || it is HomeItem.ImageToImageButton }

            // Keep existing real data on screen when a transient loading emission arrives.
            if (hasCachedCategories && !hasIncomingCategories && isIncomingOnlyShimmer) {
                updateTemplatesSection(homeItemsList)
                return@observe
            }

            homeItemsList.clear()
            homeItemsList.addAll(items)

            if (!isOnline()) {
                val offlineItems =
                    items.filter {
                        it is HomeItem.ImageToImageButton
                    }
                updateTemplatesSection(
                    offlineItems + HomeItem.OfflineMessage(getString(R.string.home_offline_message)),
                )
            } else {
                updateTemplatesSection(homeItemsList)
            }
        }
    }

    private fun setupHeroSlider(activity: FragmentActivity) {
     /*   val adapter =
            HomeHeroSliderAdapter(heroSlides) { slide ->
                handleHeroSlideClick(activity, slide)
            }

        heroPageChangeCallback =
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    applyHeroSlideState(position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    when (state) {
                        ViewPager2.SCROLL_STATE_DRAGGING -> stopHeroAutoScroll()
                        ViewPager2.SCROLL_STATE_IDLE -> startHeroAutoScroll()
                    }
                }
            }

        binding?.heroViewPager?.apply {
            this.adapter = adapter
            offscreenPageLimit = 1
            heroPageChangeCallback?.let { registerOnPageChangeCallback(it) }
            (getChildAt(0) as? RecyclerView)?.apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                isNestedScrollingEnabled = false
            }
            setCurrentItem(0, false)
        }

        applyHeroSlideState(0)
        startHeroAutoScroll()*/
    }

    private fun applyHeroSlideState(index: Int) {
       /* if (index !in heroSlides.indices) return
        currentHeroSlideIndex = index
        updateHeroDots(index)*/
    }

    private fun updateHeroDots(activeIndex: Int) {
        val dots = listOfNotNull(binding?.heroDot0, binding?.heroDot1)
        val density = resources.displayMetrics.density
        dots.forEachIndexed { i, dot ->
            val active = i == activeIndex
            val params = dot.layoutParams
            params.width = ((if (active) 16 else 10) * density).toInt()
            params.height = (4 * density).toInt()
            dot.layoutParams = params
            dot.setBackgroundResource(
                if (active) R.drawable.bg_home_dot_active else R.drawable.bg_home_dot_inactive,
            )
        }
    }

    private fun handleHeroSlideClick(activity: FragmentActivity, slide: HomeHeroSlideItem) {
        FirebaseLogUtils.logEvent(slide.analyticsEvent, "")
        // Hero aging / face-swap entry points removed — keep as no-op.
    }

    private fun startHeroAutoScroll() {
      /*  heroSliderHandler.removeCallbacks(heroAutoScrollRunnable)
        if (heroSlides.size > 1) {
            heroSliderHandler.postDelayed(heroAutoScrollRunnable, heroAutoScrollIntervalMs)
        }*/
    }

    private fun stopHeroAutoScroll() {
//        heroSliderHandler.removeCallbacks(heroAutoScrollRunnable)
    }


    private fun setFeatureRecycler(activity: FragmentActivity) {
        lifecycleScope.launch {
            viewModel.getFeatureList(activity).collectLatest { list ->

                val adapter = HomeFeatureAdapter(list) { item ->
                    handleFeatureClick(item, activity)
                }
                binding?.rvFeature?.layoutManager =
                    LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                binding?.rvFeature?.adapter = adapter
            }
        }
    }

    fun handleFeatureClick(item: HomeFeature, activity: FragmentActivity) {
        when (item.id) {
            6 -> {
                FirebaseLogUtils.logEvent("home_click_enhancer", "")
                navigateToStandaloneAiPreview(
                    activity = activity,
                    itemId = HomeAiTemplateResolver.enhancerTemplateId(homeItemsList),
                    prompt = HomeAiTemplateResolver.enhancerPrompt(
                        homeItemsList,
                        HomeAiTemplateResolver.enhancerTemplateId(homeItemsList),
                    ),
                    url = "enhancer",
                    categoryName = getString(R.string.photo_enhancer),
                    title = "Enhancer",
                )
            }

            7 -> {
                FirebaseLogUtils.logEvent("home_bg_remover_click", "")
                showInterForGallery(activity, "bgRemover")
            }

            else -> {
                // Dropped tools (aging, body, collage, editor, makeup, hair, blend, frames, swap).
            }
        }
    }

    fun handleToolsFeatureClick(item: ToolsFeature, activity: FragmentActivity) {
        when (item.id) {
            6 -> {
                FirebaseLogUtils.logEvent("home_click_enhancer", "")
                navigateToStandaloneAiPreview(
                    activity = activity,
                    itemId = HomeAiTemplateResolver.enhancerTemplateId(homeItemsList),
                    prompt = HomeAiTemplateResolver.enhancerPrompt(
                        homeItemsList,
                        HomeAiTemplateResolver.enhancerTemplateId(homeItemsList),
                    ),
                    url = "enhancer",
                    categoryName = getString(R.string.photo_enhancer),
                    title = "Enhancer",
                )
            }

            7 -> {
                FirebaseLogUtils.logEvent("home_bg_remover_click", "")
                showInterForGallery(activity, "bgRemover")
            }

            else -> {
                // Tools tab gone; ignore any leftover tool ids.
            }
        }
    }

    private fun goToBgRemover(activity: FragmentActivity) {
        TedImagePicker.with(activity, "bgRemover").image().max(1, "cannot select more than 1 video")
            .min(1, "select at least 1 video").bundleExtras(bundleOf())
            .albumType(AlbumType.DROP_DOWN).startMultiImageFragment()
    }

    private fun goToMagicEraser(activity: FragmentActivity) {
        TedImagePicker.with(activity, "magicEraser").image().max(1, "cannot select more than 1 video")
            .min(1, "select at least 1 video").bundleExtras(bundleOf())
            .albumType(AlbumType.DROP_DOWN).startMultiImageFragment()
    }

    fun navigateToAgingFromMain() {
        // AI Aging entry removed from Home / shortcuts.
    }

    fun showInterHome(
        currentActivity: FragmentActivity,
        navDirections: NavDirections,
    ) {
        if (!ClickGuard.tryLock()) return
        currentActivity.lifecycleScope.launch {
            try {
                fun navigateNow() {
                    val navController =
                        runCatching { currentActivity.findNavController(R.id.nav_host_main) }.getOrNull()
                    if (currentActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController?.safeNavigate(navDirections)
                    }
                }
                if (isProVersion.value == false && canPresentHomeInterstitial()) {
                    GlobalLoader.show(currentActivity)
                    delay(1000)
                    navigateNow()
                    if (canPresentHomeInterstitial()) {
                        interstitialHome?.showFullscreenAd(
                            currentActivity,
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    requestPermission.value = false
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                        LogUtils.printLog(
                                            "inter_home shown",
                                            interstitialTrackedUnitId(interstitialHome)
                                        )
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                    ClickGuard.unlock()
                                    LogUtils.printLog(
                                        "inter_home failed to shown",
                                        interstitialTrackedUnitId(interstitialHome)
                                    )
                                    com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                        requestPermission.value = true
                                    }
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                    com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                        requestPermission.value = true
                                    }
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                }
                            },
                        )
                    } else {
                        GlobalLoader.hide(currentActivity)
                        ClickGuard.unlock()
                    }
                } else {
                    requestPermission.value = true
                    ClickGuard.unlock()
                    navigateNow()
                }
            } catch (e: Exception) {
                requestPermission.value = true
                ClickGuard.unlock()
                e.printStackTrace()
            }
        }
    }

    fun showInterForGallery(
        currentActivity: FragmentActivity, which: String
    ) {
        if (!ClickGuard.tryLock()) return
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false && canPresentHomeInterstitial()) {
                    GlobalLoader.show(currentActivity)
                    delay(1000)
                    if (canPresentHomeInterstitial()) {
                        interstitialHome?.showFullscreenAd(
                            currentActivity,
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    requestPermission.value = false
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                        LogUtils.printLog(
                                            "inter_home shown",
                                            interstitialTrackedUnitId(interstitialHome)
                                        )
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                    ClickGuard.unlock()
                                    LogUtils.printLog(
                                        "inter_home failed to shown",
                                        interstitialTrackedUnitId(interstitialHome)
                                    )
                                    com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                        requestPermission.value = true
                                        openGalleryTarget(currentActivity, which)
                                    }
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                    com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                        requestPermission.value = true
                                    }
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                }
                            },
                            forFragment = false,
                            onContinue = {
                                GlobalLoader.hide(currentActivity)
                                openGalleryTarget(currentActivity, which)
                            },
                        )
                    } else {
                        GlobalLoader.hide(currentActivity)
                        ClickGuard.unlock()
                        openGalleryTarget(currentActivity, which)
                    }
                } else {
                    requestPermission.value = true
                    ClickGuard.unlock()
                    openGalleryTarget(currentActivity, which)
                }
            } catch (e: Exception) {
                requestPermission.value = true
                ClickGuard.unlock()
                e.printStackTrace()
                openGalleryTarget(currentActivity, which)
            }
        }
    }

    private fun openGalleryTarget(currentActivity: FragmentActivity, which: String) {
        when (which) {
            "bgRemover" -> goToBgRemover(currentActivity)
            "magicEraser" -> goToMagicEraser(currentActivity)
            else -> {
                // bodyMaker / photoEdit and other dropped gallery targets ignored.
            }
        }
    }

    /*
        fun loadNativeHomeHf(activity: FragmentActivity) {
            if (isProVersion.value == false) {
                try {
                    binding?.clAd?.visibility = View.VISIBLE
                    val adUnitId = BuildConfig.native_home_hf
                    val adLoader =
                        AdLoader.Builder(activity, adUnitId) // ✅ use Activity context
                            .forNativeAd { nativeAd ->
                                nativeHome = nativeAd
                                LogUtils.printLog(
                                    "native permission hf  loaded",
                                    BuildConfig.native_home_hf
                                )
                                showNativeHome(activity)
                                nativeAd?.setOnPaidEventListener { adValue ->
                                    adjustRevenueMMP(adUnitId,adValue.valueMicros / 1_000_000.0,adValue.currencyCode,"","Native")
                                }
                            }
                            .withAdListener(object : AdListener() {
                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    loadNativeHome(activity)
                                    LogUtils.printLog(
                                        "permission native hf failed to load",
                                        BuildConfig.native_home_hf
                                    )
                                }
                            })
                            .build()

                    adLoader.loadAd(AdRequest.Builder().build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                binding?.clAd?.visibility = View.GONE
            }
        }

        fun loadNativeHome(activity: FragmentActivity) {
            if (isProVersion.value == false) {
                try {
                    binding?.clAd?.visibility = View.VISIBLE
                    val adUnitId = BuildConfig.native_home
                    val adLoader =
                        AdLoader.Builder(activity, adUnitId) // ✅ use Activity context
                            .forNativeAd { nativeAd ->
                                nativeHome = nativeAd
                                showNativeHome(activity)
                                LogUtils.printLog("permission native loaded", BuildConfig.native_home)
                                nativeAd?.setOnPaidEventListener { adValue ->
                                    adjustRevenueMMP(adUnitId,adValue.valueMicros / 1_000_000.0,adValue.currencyCode,"","Native")
                                }
                            }
                            .withAdListener(object : AdListener() {
                                override fun onAdFailedToLoad(adError: LoadAdError) {
                                    binding?.clAd?.visibility = View.GONE
                                    LogUtils.printLog(
                                        "permission native failed to load",
                                        BuildConfig.native_home
                                    )
                                }
                            })
                            .build()

                    adLoader.loadAd(AdRequest.Builder().build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                binding?.clAd?.visibility = View.GONE
            }
        }

        private fun showNativeHome(activity: FragmentActivity) {
            try {
                nativeHome?.let {
                    val layoutResId =  R.layout.layout_native_ad_home

                    val adView = LayoutInflater.from(activity)
                        .inflate(layoutResId, null) as NativeAdView

                    populateNativeAdView(it, adView, activity)

                    binding?.nativeAdView?.removeAllViews()
                    binding?.nativeAdView?.addView(adView)
                    binding?.shimmer?.visibility = View.GONE
                    binding?.nativeAdView?.visibility = View.VISIBLE
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        private fun populateNativeAdView(
            nativeAd: NativeAd,
            adView: NativeAdView,
            activity: FragmentActivity
        ) {
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            // adView.iconView = adView.findViewById(R.id.ad_app_icon)
            adView.mediaView = adView.findViewById(R.id.ad_media)

            (adView.headlineView as? TextView)?.text = nativeAd.headline
            (adView.bodyView as? TextView)?.text = nativeAd.body
            (adView.callToActionView as? AppCompatButton)?.text = nativeAd.callToAction
            adView.mediaView?.mediaContent = nativeAd.mediaContent
            //    (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)


            adView.setNativeAd(nativeAd)
        }
    */


    override fun onResume() {
        super.onResume()
        mActivity?.let { activity ->
            if (
                NetworkUtils.isOnline(activity) &&
                !homeItemsList.any { it is HomeItem.CategoryItem }
            ) {
                viewModel.getHomeItems(forceRefresh = true)
            }
        }
        startHeroAutoScroll()
        maybeShowDailyCheckInDialog()
    }

    private fun maybeShowDailyCheckInDialog() {
        if (!isAdded || !AdsHelper.shouldShowAds()) return
        viewLifecycleOwner.lifecycleScope.launch {
            if (checkInViewModel.shouldAutoShowDialog()) {
                showDailyCheckInDialog()
            }
        }
    }

    private fun showDailyCheckInDialog() {
        if (!isAdded) return
        val tag = DailyCheckInDialogFragment.TAG
        if (parentFragmentManager.findFragmentByTag(tag) != null) return
        DailyCheckInDialogFragment.newInstance().show(parentFragmentManager, tag)
    }

    override fun onPause() {
        stopHeroAutoScroll()
        mActivity?.let { BitmapMemoryUtils.trimImageCaches(it) }
        super.onPause()
    }

    override fun onDestroyView() {
        stopHeroAutoScroll()
      /*  heroPageChangeCallback?.let { callback ->
            binding?.heroViewPager?.unregisterOnPageChangeCallback(callback)
        }*/
//        heroPageChangeCallback = null
        previewNavInFlight = false
        mActivity?.let { BitmapMemoryUtils.trimImageCaches(it) }
        binding = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }


    private fun loadAds(activity: FragmentActivity) {
        //            if (AiFaceApp.isNativeHomeHf && AiFaceApp.isNativeHome){
//                loadNativeHomeHf(activity)
//            }else if (AiFaceApp.isNativeHome){
//                loadNativeHome(activity)
//            }else{
//                binding?.clAd?.visibility = View.GONE
//            }

        if (AiFaceApp.isInterHomeHf && AiFaceApp.isInterHome) {
            loadInterHomeHigh(activity) { onLoaded ->
                if (onLoaded) {

                } else {
                    loadInterHome(activity) { onLoaded ->
                        if (onLoaded) {

                        } else {

                        }
                    }
                }
            }

        } else if (AiFaceApp.isInterHome) {
            loadInterHome(activity) { onLoaded ->
                if (onLoaded) {

                } else {

                }
            }
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = requireContext().getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post(block)
        }
    }

    private fun showAdThenNavigatePreview(activity: FragmentActivity, navigateNext: () -> Unit) {
        if (!isAdded || view == null) return
        if (!ClickGuard.tryLock()) return

        // GMA(BG) may deliver interstitial continue off main — never touch Nav/views there.
        val runNavigateOnce = {
            runOnMainThread {
                if (
                    !previewNavInFlight &&
                    isAdded &&
                    view != null &&
                    viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                ) {
                    previewNavInFlight = true
                    try {
                        navigateNext()
                    } catch (e: Exception) {
                        previewNavInFlight = false
                        Log.e("HomeFragment", "preview navigate failed", e)
                    }
                }
                ClickGuard.unlock()
            }
        }

        // Skip ads if user has subscription
        if (isProVersion.value == true) {
            runNavigateOnce()
            return
        }
        if(AiFaceApp.isInterHome) {
            viewLifecycleOwner.lifecycleScope.launch {
                val host = mActivity
                if (host == null || host.isFinishing || host.isDestroyed || !isAdded) {
                    ClickGuard.unlock()
                    return@launch
                }

                val adToShow = interstitialHome
                if (canPresentHomeInterstitial()) {
                    GlobalLoader.show(host)
                    delay(1000)
                    if (!isAdded || view == null || host.isFinishing || host.isDestroyed) {
                        GlobalLoader.hide(host)
                        ClickGuard.unlock()
                        return@launch
                    }
                    if (!canPresentHomeInterstitial()) {
                        GlobalLoader.hide(host)
                        runNavigateOnce()
                        return@launch
                    }

                    adToShow?.showFullscreenAd(
                        activity = host,
                        contentCallback = object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() {
                                GlobalLoader.hide(host)
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                Log.e("AdManager", "Ad failed: ${adError.message}")
                                GlobalLoader.hide(host)
                                runNavigateOnce()
                            }
                        },
                        forFragment = true,
                        onContinue = {
                            Log.d("AdManager", "Ad continue (fragment +500ms)")
                            interstitialHome = null
                            runNavigateOnce()
                        },
                    )
                } else {
                    Log.d("AdManager", "No ad available or cooldown active")
                    runNavigateOnce()
                }
            }
        }
        else{
            runNavigateOnce()
        }
    }

    private fun navigateToStandaloneAiPreview(
        activity: FragmentActivity,
        itemId: String,
        prompt: String,
        url: String,
        categoryName: String,
        title: String,
    ) {
        if(AiFaceApp.isInterHome) {
            showAdThenNavigatePreview(activity) {
                try {
                    navigateHomeAction(
                        R.id.action_homeFragment_to_previewFragment,
                        Bundle().apply {
                            putString("item_id", itemId)
                            putString("prompt", prompt)
                            putString("url", url)
                            putString("category_name", categoryName)
                            putString("title", title)
                        },
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        else{
            try {
                navigateHomeAction(
                    R.id.action_homeFragment_to_previewFragment,
                    Bundle().apply {
                        putString("item_id", itemId)
                        putString("prompt", prompt)
                        putString("url", url)
                        putString("category_name", categoryName)
                        putString("title", title)
                    },
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun navigateHomeAction(actionId: Int, args: Bundle? = null) {
        runOnMainThread {
            if (!isAdded || view == null) {
                previewNavInFlight = false
                return@runOnMainThread
            }
            val ok = safeNavigate(actionId, args)
            if (!ok) {
                previewNavInFlight = false
            }
        }
    }

    private fun showRewardedDialogue(
        activity: FragmentActivity,
        onDismissDone: () -> Unit
    ) {
        try {
            if (!isAdded || activity.isFinishing || activity.isDestroyed) {
                return
            }
            if (rewardDialog == null) {
                rewardDialog = DialogUtils.getDialogue(activity, R.layout.dialog_reward)
            }


            isShowingAd = true
            FirebaseLogUtils.logEvent("pop_up_premium_view", "user view pop-up premium")
            val closeBtn = rewardDialog?.findViewById<ImageView>(R.id.close_dg)
            val watchVideo = rewardDialog?.findViewById<ConstraintLayout>(R.id.watch_video)


            val goPremium = rewardDialog?.findViewById<ConstraintLayout>(R.id.goPremium)

            closeBtn?.setOnClickListener {
                isShowingAd = false
                if (!activity.isFinishing) rewardDialog?.cancel()
            }
            goPremium?.setOnClickListener {
                FirebaseLogUtils.logEvent(
                    "pop_up_premium_get_pro",
                    "user click button get pro on pop-up premium"
                )
                if (!activity.isFinishing) rewardDialog?.cancel()
                val intent = Intent(activity, IAPActivity::class.java)
                startActivity(intent)
            }

            watchVideo?.setOnClickListener {

                mActivity?.let { activity ->
                    if (!NetworkUtils.isOnline(activity)) {
                        Toast.makeText(activity, R.string.no_internet_connection, Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                    if (!activity.isFinishing) rewardDialog?.cancel()
                    isShowingAd = false
                    GenerationRewardGate.loadAndShowRewarded(
                        activity = activity,
                        highFloorId = BuildConfig.reward_home_hf,
                        normalId = BuildConfig.reward_home,
                        isHf = AiFaceApp.isRewardHomeHf,
                        isNormal = AiFaceApp.isRewardHome,
                        onUnlocked = {
                            isRewarded = true
                            onDismissDone()
                        },
                        onUnavailable = {
                            Toast.makeText(
                                activity,
                                "Ad not available, please try again",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                }
            }

            if (!activity.isFinishing) {
                rewardDialog?.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun showRewardedWithAd(
        activity: FragmentActivity,
        onReward: (reward: RewardItem) -> Unit,
        onFailed: () -> Unit,
        onDismissed: () -> Unit
    ) {
        if (isProVersion.value == true || !NetworkUtils.isOnline(activity)) {
            onDismissed(); return
        }

        val ad = rewardedAd ?: return
        val unitId = ad.trackedUnitId()
        rewardedAd = null

        showRewardedNextGen(
            activity = activity,
            ad = ad,
            adUnitId = unitId,
            onReward = onReward,
            onFailed = onFailed,
            onDismissed = onDismissed,
            onAfterDismissLoad = {
                loadRewardedAd(
                    activity,
                    BuildConfig.reward_home_hf,
                    BuildConfig.reward_home,
                    AiFaceApp.isRewardHomeHf,
                    AiFaceApp.isRewardHome
                ) { onLoaded ->
                    // isRewardLoaded = onLoaded
                }
            }
        )
    }


}