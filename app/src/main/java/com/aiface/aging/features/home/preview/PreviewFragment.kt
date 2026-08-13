package com.aiface.aging.features.home.preview

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.databinding.FragmentPreviewBinding
import com.aiface.aging.domain.model.Template
import com.aiface.aging.features.home.HomeViewModel
import com.aiface.aging.features.home.TemplateImageRequirements
import com.aiface.aging.features.home.aging.AgingTemplateAdapter
import com.aiface.aging.features.home.aging.AgingTemplateCatalog
import com.aiface.aging.features.home.aging.AgingTemplateOption
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.features.result.ResultFeatureNavigator
import com.aiface.aging.features.result.ResultHostActivity
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.shared.safeFinish
import com.aiface.aging.shared.safeNavigateUp
import com.aiface.aging.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreviewFragment : Fragment() {

    private var binding: FragmentPreviewBinding? = null
    private var mActivity: FragmentActivity? = null
    private var nativePreviewAi: NativeAd? = null
    private val homeViewModel: HomeViewModel by activityViewModels()

    private var isAgingFlow = false
    private var isEnhancerFlow = false
    private var isCatalogPagerFlow = false
    private var selectedAgingTemplate: AgingTemplateOption? = null
    private var agingTemplateAdapter: AgingTemplateAdapter? = null
    private var lastBoundAgingIds: List<String>? = null
    private var selectedPrompt: String? = null
    private var selectedItemId: String? = null
    private var selectedThumbUrl: String? = null
    private var selectedTitle: String? = null
    /** Required user photos for generate (1 or 2 from template image_count). */
    private var selectedImageCount: Int = 1

    companion object {
        const val ARG_AGING_TITLE_RES = "aging_title_res"
    }

    private val pagerAdapter by lazy {
        PreviewTemplatePagerAdapter { template ->
            selectTemplate(template)
            mActivity?.let { openImagePickerIfReady(it) }
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        view.applySystemBarInsets(applyTop = true, applyBottom = false)
        mActivity?.let { activity ->
            AppUtils.hideHomeBannerAd(activity)
            loadAds(activity)

            val title = arguments?.getString("title")
            val prompt = arguments?.getString("prompt")
            val categoryName = arguments?.getString("category_name")
            val categoryId = arguments?.getString("category_id")
            val imageUrl = arguments?.getString("url")

            selectedPrompt = prompt
            selectedItemId = arguments?.getString("item_id")
            selectedThumbUrl = imageUrl
            selectedTitle = title
            selectedImageCount = TemplateImageRequirements.requiredCount(
                arguments?.getInt(TemplateImageRequirements.ARG_IMAGE_COUNT, 1),
            )

            Log.d("xxxx", categoryName.toString())
            Log.d("xxxx", prompt.toString())

            binding?.btnBack?.setOnClickListener { handlePreviewBack() }
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() = handlePreviewBack()
                },
            )

            when (imageUrl) {
                "enhancer", "aging" -> setupSpecialFlow(activity, imageUrl)
                else -> setupCatalogPagerFlow(activity, categoryId, categoryName)
            }
        }
    }

    private fun setupSpecialFlow(activity: FragmentActivity, imageUrl: String) {
        isCatalogPagerFlow = false
        binding?.vpTemplates?.isVisible = false
        binding?.legacyPreviewContainer?.isVisible = true
        binding?.btnBack?.imageTintList =
            android.content.res.ColorStateList.valueOf(
                requireContext().getColor(R.color.icon_primary),
            )
        binding?.btnBack?.setBackgroundResource(0)

        val featureTitle =
            if (imageUrl == "enhancer") {
                getString(R.string.photo_enhancer)
            } else {
                getString(R.string.aging)
            }
        binding?.tvPrompt?.visibility = View.GONE
        binding?.tvHeaderTemplateCategory?.text = featureTitle

        binding?.imagePreview?.let {
            Glide.with(activity)
                .load(imageUrl)
                .placeholder(R.drawable.im_upload_files)
                .into(it)
        }

        if (imageUrl == "enhancer") {
            isEnhancerFlow = true
            setupEnhancerTemplateResolution()
        }

        if (imageUrl == "aging") {
            isAgingFlow = true
            ensureAgingCategoriesLoaded()
            setupAgingTemplatePicker(activity)
            updateAgingUploadState()
        }

        binding?.imagePreview?.setOnClickListener {
            openImagePickerIfReady(activity)
        }
        binding?.btnTryTemplate?.setOnClickListener {
            openImagePickerIfReady(activity)
        }
    }

    private fun setupCatalogPagerFlow(
        activity: FragmentActivity,
        categoryId: String?,
        categoryName: String?,
    ) {
        isCatalogPagerFlow = true
        binding?.legacyPreviewContainer?.isVisible = false
        binding?.vpTemplates?.isVisible = true
        binding?.btnBack?.imageTintList =
            android.content.res.ColorStateList.valueOf(
                requireContext().getColor(R.color.white),
            )

        binding?.vpTemplates?.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding?.vpTemplates?.adapter = pagerAdapter
        binding?.vpTemplates?.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    pagerAdapter.getItem(position)?.let { selectTemplate(it) }
                }
            },
        )

        homeViewModel.ensureCatalogLoaded()
        bindCatalogTemplates(categoryId, categoryName)
        homeViewModel.theHomeItems.observe(viewLifecycleOwner) {
            bindCatalogTemplates(categoryId, categoryName)
        }
    }

    private var catalogBoundOnce = false

    private fun bindCatalogTemplates(categoryId: String?, categoryName: String?) {
        if (!isCatalogPagerFlow) return
        var templates = homeViewModel.templatesForCategory(categoryId, categoryName)
        if (templates.isEmpty()) {
            // Fallback: single item from navigation args so CTA still works.
            val id = selectedItemId
            if (!id.isNullOrBlank()) {
                templates = listOf(
                    Template(
                        id = id,
                        categoryId = categoryId.orEmpty(),
                        title = selectedTitle,
                        generationType = "",
                        isActive = true,
                        priority = 0,
                        isPro = false,
                        imageCount = selectedImageCount,
                        vendorTemplateId = null,
                        mediaUrl = selectedThumbUrl,
                        gifUrl = null,
                        thumbnailUrl = selectedThumbUrl,
                        prompt = selectedPrompt,
                        negativePrompt = null,
                    ),
                )
            }
        }
        if (templates.isEmpty()) return

        val startId = selectedItemId
        val startIndex = templates.indexOfFirst { it.id == startId }.coerceAtLeast(0)
        val firstBind = !catalogBoundOnce
        pagerAdapter.submit(templates)
        if (firstBind) {
            catalogBoundOnce = true
            binding?.vpTemplates?.setCurrentItem(startIndex, false)
            selectTemplate(templates[startIndex])
        }
    }

    private fun selectTemplate(template: Template) {
        selectedItemId = template.id
        selectedPrompt = template.prompt
        selectedTitle = template.title
        selectedThumbUrl = template.thumbnailUrl ?: template.mediaUrl
        selectedImageCount = TemplateImageRequirements.requiredCount(template.imageCount)
    }

    private fun handlePreviewBack() {
        if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
        try {
            val activity = requireActivity()
            when {
                activity is ResultHostActivity -> safeNavigateUp()
                ResultFeatureNavigator.shouldFinishMainToRevealShareHost(activity) -> activity.safeFinish()
                else -> safeNavigateUp()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureAgingCategoriesLoaded() {
        homeViewModel.ensureCatalogLoaded()
    }

    private fun setupEnhancerTemplateResolution() {
        homeViewModel.ensureCatalogLoaded()
        resolveEnhancerTemplate()
        homeViewModel.theHomeItems.observe(viewLifecycleOwner) {
            resolveEnhancerTemplate()
        }
    }

    private fun resolveEnhancerTemplate() {
        if (!isEnhancerFlow || !homeViewModel.isCatalogReady()) return
        selectedItemId = homeViewModel.enhancerTemplateId()
        selectedPrompt = homeViewModel.enhancerPrompt(selectedItemId)
    }

    private fun setupAgingTemplatePicker(activity: FragmentActivity) {
        binding?.tvAgingSelectHint?.visibility = View.VISIBLE
        binding?.rvAgingTemplates?.visibility = View.VISIBLE
        binding?.rvAgingTemplates?.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        bindAgingTemplates(
            preferredTemplateId = selectedItemId ?: arguments?.getString("item_id"),
            preferredTitleRes = selectedAgingTemplate?.titleRes
                ?: arguments?.getInt(ARG_AGING_TITLE_RES)?.takeIf { it != 0 },
        )

        homeViewModel.theHomeItems.observe(viewLifecycleOwner) {
            bindAgingTemplates(
                preferredTemplateId = selectedItemId,
                preferredTitleRes = selectedAgingTemplate?.titleRes,
            )
        }
    }

    private fun bindAgingTemplates(
        preferredTemplateId: String?,
        preferredTitleRes: Int? = null,
    ) {
        val templates = homeViewModel.getAgingTemplateOptions()
        val preferredIndex = selectedAgingTemplate?.titleRes?.let { titleRes ->
            templates.indexOfFirst { it.titleRes == titleRes }
        } ?: -1
        val resolvedId = AgingTemplateCatalog.resolveSelectionId(
            options = templates,
            preferredTemplateId = preferredTemplateId,
            preferredTitleRes = preferredTitleRes ?: selectedAgingTemplate?.titleRes,
            preferredIndex = preferredIndex,
        )

        // Skip rebuild when list + selection unchanged (e.g. sticky LiveData re-emit).
        val newIds = templates.map { it.templateId }
        if (
            agingTemplateAdapter != null &&
            lastBoundAgingIds == newIds &&
            selectedAgingTemplate?.templateId == resolvedId
        ) {
            return
        }
        lastBoundAgingIds = newIds

        agingTemplateAdapter =
            AgingTemplateAdapter(templates) { selected ->
                selectedAgingTemplate = selected
                selectedItemId = selected?.templateId
                selectedPrompt = selected?.prompt
                updateAgingUploadState()
            }.also { adapter ->
                adapter.setSelectedTemplateId(resolvedId)
                selectedAgingTemplate = adapter.getSelected()
                selectedItemId = selectedAgingTemplate?.templateId ?: selectedItemId
                selectedPrompt = selectedAgingTemplate?.prompt ?: selectedPrompt
            }
        binding?.rvAgingTemplates?.adapter = agingTemplateAdapter
        updateAgingUploadState()
    }

    private fun updateAgingUploadState() {
        if (!isAgingFlow) return
        val hasSelection = selectedAgingTemplate != null
        binding?.btnTryTemplate?.alpha = if (hasSelection) 1f else 0.5f
    }

    private fun openImagePickerIfReady(activity: FragmentActivity) {
        if (isEnhancerFlow) {
            homeViewModel.ensureCatalogLoaded()
            resolveEnhancerTemplate()
            if (!homeViewModel.isCatalogReady() || selectedItemId.isNullOrBlank()) {
                Toast.makeText(
                    activity,
                    getString(R.string.result_loading_templates),
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
        }
        if (isAgingFlow && selectedAgingTemplate == null) {
            Toast.makeText(
                activity,
                getString(R.string.aging_select_template_first),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        goToImagePicker(activity, createImagePickerBundle())
    }

    private fun createImagePickerBundle(): Bundle {
        return bundleOf(
            "item_id" to selectedItemId,
            "prompt" to selectedPrompt,
            "category_name" to arguments?.getString("category_name"),
            "category_id" to arguments?.getString("category_id"),
            "url" to (selectedThumbUrl ?: arguments?.getString("url")),
            "title" to selectedTitle,
            ARG_AGING_TITLE_RES to (selectedAgingTemplate?.titleRes ?: 0),
            TemplateImageRequirements.ARG_IMAGE_COUNT to selectedImageCount,
        )
    }

    private fun loadAds(activity: FragmentActivity) {
        if (AiFaceApp.isNativePreviewAi) {
            startNative(tryHigh = true)
        } else {
            binding?.clAd?.visibility = View.GONE
        }
    }

    private fun goToImagePicker(activity: FragmentActivity, bundle: Bundle) {
        val count = selectedImageCount.coerceIn(1, 2)
        val noun = if (count == 1) "image" else "images"
        TedImagePicker.with(activity, "ai")
            .image()
            .max(count, "cannot select more than $count $noun")
            .min(count, "select at least $count $noun")
            .bundleExtras(bundle)
            .albumType(AlbumType.DROP_DOWN)
            .startMultiImageFragment()
    }

    private fun startNative(tryHigh: Boolean) {
        try {
            if (!AdsHelper.shouldShowAds()) {
                binding?.clAd?.visibility = View.INVISIBLE
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
                onLoaded = { ad, _ ->
                    try {
                        if (!isAdded || view == null || binding == null) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        val host = activity ?: run {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativePreviewAi?.destroy()
                        nativePreviewAi = ad
                        showNativeHome(host)
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

    private fun showNativeHome(activity: FragmentActivity) {
        binding?.clAd?.visibility = View.VISIBLE
        AdsHelper.bindNativeAdToContainerSmallReel(
            nativePreviewAi,
            binding?.nativeAdView,
            binding?.shimmerContainerNative?.shimmerContainerNative,
            activity,
            binding?.shimmer,
        )
    }

    override fun onDestroyView() {
        nativePreviewAi?.destroy()
        nativePreviewAi = null
        binding = null
        super.onDestroyView()
    }
}
