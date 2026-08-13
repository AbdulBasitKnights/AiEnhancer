package com.aiface.aging.features.see_all

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.GenerationRewardGate
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.shared.safeNavigate
import com.aiface.aging.shared.safeNavigateUp
import com.aiface.aging.shared.setSafeClickListener
import com.aiface.aging.databinding.FragmentSeeAllBinding
import com.aiface.aging.domain.model.Template
import com.aiface.aging.features.home.HomeViewModel
import com.aiface.aging.features.home.ProPanelHomeObject
import com.aiface.aging.features.home.TemplateImageRequirements
import com.aiface.aging.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SeeAllFragment : Fragment() {

    private var _binding: FragmentSeeAllBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SeeAllViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: SeeAllAdapter

    private var mActivity: FragmentActivity? = null

    private var nativeSeeAll: NativeAd? = null

    private val categoryAdapter by lazy {
        SeeAllCategoryAdapter { _, category ->
            viewModel.selectCategory(category.id)
            binding.rvTemplates.scrollToPosition(0)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    companion object {
        var isProClosedAfterRewardForSeeAll = MutableLiveData(false)
        private var proPanelHomeObject: ProPanelHomeObject? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSeeAllBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarInsets(applyTop = true, applyBottom = true)

        mActivity?.let { activity ->
            AppUtils.hideHomeBannerAd(activity)
            loadAds(activity)

            val preferredCategoryId = arguments?.getString("category_id").orEmpty()

            setupHeader()
            setupCategoryChips()
            setupRecycler(activity)
            observeCatalog(preferredCategoryId)

            setTemplatesShimmer(true)
            viewModel.loadCatalog(
                preferredCategoryId.takeIf { it.isNotBlank() },
                homeViewModel.catalogCategories(),
            )

            isProClosedAfterRewardForSeeAll.observe(viewLifecycleOwner, Observer {
                if (it) {
                    isProClosedAfterRewardForSeeAll.value = false

                    val bundle =
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
                        }
                    safeNavigate(R.id.action_seeAllFragment_to_previewFragment, bundle)
                }
            })
        }
    }

    private fun setupHeader() {
        binding.tvTitle.text = getString(R.string.templates)
        binding.btnBack.setSafeClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setSafeClickListener
            safeNavigateUp()
        }
    }

    private fun setupCategoryChips() {
        binding.rvCategories.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = categoryAdapter
    }

    private fun setupRecycler(activity: FragmentActivity) {
        adapter = SeeAllAdapter({ template -> openTemplate(template, activity) },requireActivity()

        )
        binding.rvTemplates.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvTemplates.adapter = adapter
    }

    private fun observeCatalog(preferredCategoryId: String) {
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading == true && adapter.currentList.isEmpty()) {
                setTemplatesShimmer(true)
            }
        }
        viewModel.categories.observe(viewLifecycleOwner) { list ->
            categoryAdapter.submitList(list) {
                val selectedId = viewModel.selectedCategoryId() ?: preferredCategoryId
                val index = list.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
                if (list.isNotEmpty()) {
                    categoryAdapter.selectMode(index)
                    binding.rvCategories.post {
                        binding.rvCategories.scrollToPosition(index)
                    }
                }
            }
        }
        viewModel.templates.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list) {
                viewModel.markTemplatesBound()
                setTemplatesShimmer(false)
            }
        }
    }

    /** Overlay only — never hide rvTemplates. */
    private fun setTemplatesShimmer(show: Boolean) {
        val shimmerHost = binding.templatesShimmer
        shimmerHost.isVisible = show
        binding.rvTemplates.isVisible = true
        val shimmer = shimmerHost.getChildAt(0) as? ShimmerFrameLayout
        if (show) shimmer?.startShimmer() else shimmer?.stopShimmer()
    }

    private fun openTemplate(template: Template, activity: FragmentActivity) {
        val category = viewModel.selectedCategory()
        val categoryId = category?.id.orEmpty()
        val categoryName = category?.name.orEmpty()

        val openPreview = {
            val bundle = bundleOf(
                "item_id" to template.id,
                "prompt" to template.prompt.orEmpty(),
                "url" to (template.thumbnailUrl ?: template.mediaUrl.orEmpty()),
                "category_name" to categoryName,
                "category_id" to categoryId,
                "title" to template.title,
                TemplateImageRequirements.ARG_IMAGE_COUNT to
                    TemplateImageRequirements.requiredCount(template.imageCount),
            )
            safeNavigate(R.id.action_seeAllFragment_to_previewFragment, bundle)
            Unit
        }

        GenerationRewardGate.gateHomePremiumTemplate(
            activity = activity,
            isPremiumItem = template.isPro,
            onContinue = openPreview,
            onOpenIap = {
                proPanelHomeObject = ProPanelHomeObject(
                    template.id,
                    template.prompt.orEmpty(),
                    template.thumbnailUrl ?: template.mediaUrl.orEmpty(),
                    categoryName,
                    template.title.orEmpty(),
                    categoryId,
                    TemplateImageRequirements.requiredCount(template.imageCount),
                )
                startActivity(
                    Intent(activity, com.aiface.aging.features.iap.IAPActivity::class.java).apply {
                        putExtra("isFromProPanel", true)
                        putExtra("isFromHome", true)
                    },
                )
            },
        )
    }

    override fun onDestroyView() {
        nativeSeeAll?.destroy()
        nativeSeeAll = null
        super.onDestroyView()
        _binding = null
    }

    private fun loadAds(activity: FragmentActivity) {
        if (AdsHelper.shouldShowAds()) {
            if (AiFaceApp.nativeSeeAllHf && AiFaceApp.nativeSeeAll) {
                startNative(tryHigh = true)
            } else if (AiFaceApp.nativeSeeAll) {
                startNative(tryHigh = false)
            } else {
                binding?.clAd?.visibility = View.GONE
            }
        } else {
            binding?.clAd?.visibility = View.GONE
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }

    private fun startNative(tryHigh: Boolean) {
        try {
            if (!AdsHelper.shouldShowAds()) {
                binding?.clAd?.visibility = View.GONE
                AdShimmerHelper.hideNativeAdSlot(
                    adSlot = binding?.clAd,
                    shimmerWrapper = binding?.shimmer,
                    nativeContainer = binding?.nativeAdView,
                )
                return
            }
            binding?.clAd?.visibility = View.VISIBLE
            AdShimmerHelper.showLayoutNativePlaceholder(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_home_hf,
                normalUnitId = BuildConfig.native_home,
                onLoaded = { ad, unitId ->
                    try {
                        if (!isAdded || view == null || binding == null) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativeSeeAll?.destroy()
                        nativeSeeAll = ad
                        val container = binding?.nativeAdView
                        if (container == null) {
                            ad.destroy()
                            AdShimmerHelper.hideNativeAdSlot(
                                adSlot = binding?.clAd,
                                shimmerWrapper = binding?.shimmer,
                            )
                            return@loadWithFallback
                        }
                        NativeAdDisplayHelper.display(
                            container = container,
                            inflater = layoutInflater,
                            nativeAd = ad,
                            onDestroyPrevious = {},
                            adUnitId = unitId,
                            layoutResId = R.layout.layout_native_ads_without_mediaview_b,
                            shimmer = binding?.shimmer
                        )
                    } catch (t: Throwable) {
                        try {
                            ad.destroy()
                        } catch (_: Throwable) {
                        }
                        AdShimmerHelper.hideNativeAdSlot(
                            adSlot = binding?.clAd,
                            shimmerWrapper = binding?.shimmer,
                            nativeContainer = binding?.nativeAdView,
                        )
                    }
                },
                onFailed = {
                    AdShimmerHelper.hideNativeAdSlot(
                        adSlot = binding?.clAd,
                        shimmerWrapper = binding?.shimmer,
                        nativeContainer = binding?.nativeAdView,
                    )
                }
            )
        } catch (t: Throwable) {
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }

    private var rewardDialog: Dialog? = null
   /* private fun showRewardedDialogue(
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
                    if (rewardedAd != null) {

                        try {
                            showRewardedWithAd(activity, {}, {
                                Toast.makeText(
                                    activity,
                                    "no ad available, please try again",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadRewardedAd(
                                    activity,
                                    BuildConfig.reward_home_hf,
                                    BuildConfig.reward_home,
                                    AiFaceApp.isRewardHomeHf,
                                    AiFaceApp.isRewardHome
                                ) { onLoaded ->
                                    //   isRewardLoaded = onLoaded

                                }

                            }, {
                                isRewarded = true
                                if (!activity.isFinishing) rewardDialog?.cancel()
                                isShowingAd = false
                                rewardDialog?.dismiss()
                                onDismissDone()

                            })
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(activity, "Ad not loaded, Please try again", Toast.LENGTH_SHORT).show()
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

        fun actuallyShow(ad: RewardedAd) {
            activity.lifecycleScope.launch {
                try {
                    GlobalLoader.show(activity)
                    // Optional tiny delay to let loader render
                    delay(300)

                    var rewardGiven = false

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            isShowingAd = true
                            ad?.onPaidEventListener =
                                OnPaidEventListener { adValue ->
                                    adjustRevenueMMP(
                                        ad?.adUnitId,
                                        adValue.valueMicros / 1_000_000.0,
                                        adValue.currencyCode,
                                        "",
                                        "Rewarded Ad"
                                    )
                                }
                            activity.lifecycleScope.launch {
                                delay(1200)
                                GlobalLoader.hide(activity)
                            }

                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            GlobalLoader.hide(activity)
                            rewardedAd = null
                            isShowingAd = false
                            onFailed()
                        }

                        override fun onAdDismissedFullScreenContent() {

                            rewardedAd = null
                            isShowingAd = false
                            GlobalLoader.hide(activity)
                            onDismissed()
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
                    }

                    ad.show(activity) { rewardItem ->
                        rewardGiven = true
                        onReward(rewardItem)
                        // You can also navigate/unlock here, or defer to onDismiss if you prefer
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        rewardedAd?.let { ad ->
            actuallyShow(ad)
            rewardedAd = null
            return
        }
    }*/

}
