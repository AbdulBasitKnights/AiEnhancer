package com.aiface.aging.features.edit

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.bumptech.glide.Glide
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.shared.ads.GenerationRewardGate
import com.aiface.aging.shared.ads.canPresentHomeInterstitial
import com.aiface.aging.shared.ads.interstitialHome
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.shared.ads.showRewardedNextGen
import com.aiface.aging.shared.ads.loadInterHome
import com.aiface.aging.shared.ads.loadInterHomeHigh
import com.aiface.aging.shared.ads.loadRewardedAd
import com.aiface.aging.shared.ads.trackedUnitId
import com.aiface.aging.shared.ads.rewardedAd
import com.aiface.aging.databinding.DialogImgGeneratingBinding
import com.aiface.aging.databinding.FragmentEditBinding
import com.aiface.aging.domain.model.GenerateUiState
import com.aiface.aging.features.result.ResultArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.aiface.aging.features.home.HomeFragment.Companion.requestPermission
import com.aiface.aging.features.home.HomeViewModel
import com.aiface.aging.features.home.TemplateImageRequirements
import com.aiface.aging.features.result.ResultFeatureNavigator
import com.aiface.aging.features.result.ResultHostActivity
import com.aiface.aging.features.home.aging.AgingTemplateAdapter
import com.aiface.aging.features.home.aging.AgingTemplateCatalog
import com.aiface.aging.features.home.aging.AgingTemplateOption
import com.aiface.aging.features.home.preview.PreviewFragment
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.shared.ads.nativeLanguage
import com.aiface.aging.shared.ads.nativeLanguageAlt
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditFragment : Fragment() {

    private var binding: FragmentEditBinding? = null
    private var myCredits: Int? = null

    private var isRewardLoaded = false

    private val viewModel: ImageToImageViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private var nativeEditAi: NativeAd? = null
    private var generatingDialog: Dialog? = null

    private var mActivity: FragmentActivity? = null
    private var isAgingFlow = false
    private var isEnhancerFlow = false
    private var selectedAgingTemplate: AgingTemplateOption? = null
    private var agingTemplateAdapter: AgingTemplateAdapter? = null
    private var lastBoundAgingIds: List<String>? = null
    private var selectedItemId: String? = null
    private var selectedPrompt: String? = null
    private var requiredImageCount: Int = 1
    private var imageOneUri: Uri? = null
    private var imageTwoUri: Uri? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity?.let { activity ->

        /* *//*   if (isProVersion.value == true) {
                binding?.ivWatchAd?.visibility = View.GONE
                binding?.watchAd?.visibility = View.GONE
            }*/

//            loadRewardedAds(activity)
            loadAds(activity)
            loadInterAds(activity)
            applyRoots()
            observeState(activity)
            observers(activity)
            clickListeners(activity)
            setupBackNavigation()

            val uriString = arguments?.getString(TemplateImageRequirements.ARG_IMAGE_URI)
                ?: arguments?.getString("imageUri")
            val uriTwoString = arguments?.getString(TemplateImageRequirements.ARG_IMAGE_URI_TWO)
            val url = arguments?.getString("url")
            selectedItemId = arguments?.getString("item_id")
            selectedPrompt = arguments?.getString("prompt").orEmpty()
            requiredImageCount = TemplateImageRequirements.requiredCount(
                arguments?.getInt(TemplateImageRequirements.ARG_IMAGE_COUNT, 1),
            )
            imageOneUri = uriString?.let(Uri::parse)
            imageTwoUri = uriTwoString?.let(Uri::parse)

            if (url == "enhancer" || url == "aging") {
                binding?.edittext?.visibility = View.INVISIBLE
                binding?.tvPrompt?.visibility = View.INVISIBLE
                binding?.imageEditPreview?.visibility = View.INVISIBLE
                binding?.llDualImagePreview?.visibility = View.GONE
                binding?.imageEditPreviewEnhancer?.visibility = View.VISIBLE

                imageOneUri?.let { uri ->
                    binding?.imageEditPreviewEnhancer?.load(uri) {
                        crossfade(true)
                        allowHardware(false)
                    }
                }

                if (url == "aging") {
                    isAgingFlow = true
                    ensureAgingCategoriesLoaded()
                    setupAgingTemplatePicker()
                }

                if (url == "enhancer") {
                    isEnhancerFlow = true
                    setupEnhancerTemplateResolution()
                }
            } else {
                bindSelectedUserImages()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.credits.collect { credits ->
                        myCredits = credits
                        binding?.tvCredits?.text = credits.toString()
                    }
                }
            }

            val prompt = selectedPrompt.orEmpty()
            binding?.edittext?.setText(prompt)

            val templateName = arguments?.getString("category_name")
            binding?.tvTitle?.text = templateName


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
        binding?.edittext?.setText(selectedPrompt)
    }

    private fun setupAgingTemplatePicker() {
        binding?.tvAgingSelectHint?.visibility = View.VISIBLE
        binding?.rvAgingTemplates?.visibility = View.VISIBLE
        binding?.rvAgingTemplates?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val titleResFromArgs = arguments?.getInt(PreviewFragment.ARG_AGING_TITLE_RES)?.takeIf { it != 0 }
        bindAgingTemplates(
            preferredTemplateId = selectedItemId,
            preferredTitleRes = titleResFromArgs,
        )

        homeViewModel.theHomeItems.observe(viewLifecycleOwner) {
            bindAgingTemplates(
                preferredTemplateId = selectedItemId,
                preferredTitleRes = selectedAgingTemplate?.titleRes ?: titleResFromArgs,
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
        } ?: preferredTitleRes?.let { titleRes ->
            templates.indexOfFirst { it.titleRes == titleRes }
        } ?: -1

        val resolvedId = AgingTemplateCatalog.resolveSelectionId(
            options = templates,
            preferredTemplateId = preferredTemplateId,
            preferredTitleRes = preferredTitleRes ?: selectedAgingTemplate?.titleRes,
            preferredIndex = preferredIndex,
        )

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
                selectedPrompt = selected?.prompt.orEmpty()
                binding?.edittext?.setText(selectedPrompt)
                updateAgingGenerateState()
            }.also { adapter ->
                adapter.setSelectedTemplateId(resolvedId)
                selectedAgingTemplate = adapter.getSelected()
                if (selectedAgingTemplate != null) {
                    selectedItemId = selectedAgingTemplate?.templateId
                    selectedPrompt = selectedAgingTemplate?.prompt.orEmpty()
                    binding?.edittext?.setText(selectedPrompt)
                }
            }
        binding?.rvAgingTemplates?.adapter = agingTemplateAdapter
        updateAgingGenerateState()
    }

    private fun updateAgingGenerateState() {
        if (!isAgingFlow) return
        val hasSelection = selectedAgingTemplate != null
        binding?.btnCreate?.alpha = if (hasSelection) 1f else 0.5f
    }

    private fun handleEditBack() {
        val popped = findNavController().navigateUp()
        if (!popped) {
            when {
                requireActivity() is ResultHostActivity -> requireActivity().finish()
                ResultFeatureNavigator.shouldFinishMainToRevealShareHost(requireActivity()) ->
                    requireActivity().finish()
            }
        }
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleEditBack()
            },
        )
    }

    private fun clickListeners(activity: FragmentActivity) {
        binding?.btnBackEdit?.setOnClickListener {
            handleEditBack()
        }


        binding?.btnCreate?.setOnClickListener {
            myCredits?.let { credits ->
                if (isEnhancerFlow) {
                    homeViewModel.ensureCatalogLoaded()
                    resolveEnhancerTemplate()
                    if (!homeViewModel.isCatalogReady() || selectedItemId.isNullOrBlank()) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.result_loading_templates),
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@setOnClickListener
                    }
                }
                if (isAgingFlow && selectedAgingTemplate == null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.aging_select_template_first),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@setOnClickListener
                }

                if (credits < 1 && isProVersion.value == false) {
                    showCreditsDialog()
                    return@setOnClickListener
                }

                if (isProVersion.value == true) {
                    generateTheImage(activity)
                    return@setOnClickListener
                }

                // Non-pro generation: Pro / Watch-ad dialog (rewarded — no inter cooldown).
                if(credits<1){
                    Toast.makeText(requireContext(), "Sorry, No Credits", Toast.LENGTH_SHORT).show()
                }
                else{
                      GenerationRewardGate.showProOrWatchDialog(
                      activity = activity,
                      highFloorId = BuildConfig.reward_home_hf,
                      normalId = BuildConfig.reward_home,
                      isHf = AiFaceApp.isRewardPromptHf,
                      isNormal = AiFaceApp.isRewardPrompt,
                      isFromEdit = true,
                      onUnlocked = { generateTheImage(activity) },
                  )
                }

            }


        }
    }

    private fun observers(activity: FragmentActivity){
        isProClosedForEdit.observe(viewLifecycleOwner, Observer{
            if (it){
                isProClosedForEdit.value = false
                if (rewardedAd != null) {
                    showRewardedWithAd(
                        activity,
                        {

                        }, {
                            showInterAd(activity)
                        }, {
                            interstitialHome = null
                            showInterAd(activity)
                        })
                } else {
                    showInterAd(activity)
                }
            }
        })
    }

       private fun observeState() {
           viewModel.generateState.observe(viewLifecycleOwner) { state ->
               when (state) {
                   is GenerateUiState.Success -> {
                       val bundle =
                           Bundle().apply {
                               putParcelable("generate_response", state.response)
                           }
                       viewModel.resetState()
                       generatingDialog?.dismiss()
                       findNavController().navigate(
                           R.id.action_editFragment_to_resultFragment,
                           bundle,
                       )
                   }

                   is GenerateUiState.Error -> {
                       generatingDialog?.dismiss()
                       viewModel.resetState()
                       findNavController().navigateUp()
                       Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                   }

                   else -> Unit
               }
           }
       }

    fun getFileSize(uri: Uri): Long {
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                it.moveToFirst()
                it.getLong(sizeIndex)
            } ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun observeState(activity: FragmentActivity) {
        viewModel.generateState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GenerateUiState.Success -> {
                    val sourceFeature = when {
                        isEnhancerFlow -> "enhancer"
                        isAgingFlow -> "aging"
                        else -> arguments?.getString("url")
                    }
                    val bundle = ResultArgs.ai(state.response, sourceFeature)
                    viewModel.resetState()
                    generatingDialog?.dismiss()

                    navigateNext(activity, bundle)
                    FirebaseLogUtils.logEvent("generation_success", "")
                    /*    if (isProVersion.value == false && AiFaceApp.isRewardPrompt) {

                            if (rewardedAd != null) {
                                showRewardedWithAd(
                                    activity,
                                    {

                                    }, {
                                        showInterAd(activity, bundle)
                                    }, {
                                        interstitialHome = null
                                        showInterAd(activity, bundle)
                                    })
                            } else {
                                showInterAd(activity, bundle)
                            }

                        } else {
                            showInterAd(activity, bundle)
                        }
    */

                }

                is GenerateUiState.Error -> {
                    FirebaseLogUtils.logEvent("generation_failed", "")
                    viewModel.resetState()
                    generatingDialog?.dismiss()
                    var msg = state.message
                    if (msg.isNullOrEmpty()) {
                        msg = "Something went wrong"
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    Log.d("observeStateGeneration", "${state.message}")

                }

                else -> Unit
            }
        }
    }


    override fun onDestroyView() {
        nativeEditAi?.destroy()
        nativeEditAi = null
        super.onDestroyView()
    }


    private fun showCreditsDialog() {
        Toast.makeText(requireContext(), "Sorry, No Credits", Toast.LENGTH_SHORT).show()
//            CredtisDialogFragment().show(parentFragmentManager, "CreditsDialog")
//        startActivity(Intent(mActivity, IAPActivity::class.java))
    }

    private fun showGeneratingDialog(activity: FragmentActivity) {
        if (generatingDialog == null) {
            val dialogBinding = DialogImgGeneratingBinding.inflate(activity.layoutInflater)

            generatingDialog = Dialog(activity).apply {
                setContentView(dialogBinding.root)
                setCancelable(true)
            }

            generatingDialog?.setCancelable(false)

            Glide.with(activity)
                .asGif()
                .load(R.drawable.video_processing)
                .into(dialogBinding.loadingGif)
        }

        if (!activity.isFinishing) {
            generatingDialog?.apply {
                show()
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                window?.setBackgroundDrawable(
                    Color.TRANSPARENT.toDrawable()
                )
            }
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

    private fun applyRoots() {
        binding?.root?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(
                    0,
                    0,
                    0,
                    systemBars.bottom
                )
                insets
            }
        }
    }

    private fun loadAds(activity: FragmentActivity) {
        if (AdsHelper.shouldShowAds()) {
            if (nativeLanguage == null && nativeLanguageAlt == null) {
                if (AiFaceApp.nativeEditAiHf && AiFaceApp.nativeEditAi) {
                    startNative(tryHigh = true)
                } else if (AiFaceApp.nativeEditAi) {
                    startNative(tryHigh = false)
                } else {
                    binding?.clAd?.visibility = View.INVISIBLE
                }
            }
        } else {
            binding?.clAd?.visibility = View.INVISIBLE
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }

    private fun loadRewardedAds(activity: FragmentActivity) {
        if (AdsHelper.shouldShowAds()) {
            loadRewardedAd(
                activity,
                BuildConfig.reward_home_hf,
                BuildConfig.reward_home,
                AiFaceApp.isRewardPromptHf,
                AiFaceApp.isRewardPrompt
            ) { onLoaded ->
                isRewardLoaded = onLoaded

            }
        }
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
                        nativeEditAi?.destroy()
                        nativeEditAi = ad
                        showNativeUninstall(host)
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

    private fun showNativeUninstall(activity: FragmentActivity) {
        if (isProVersion.value == true) return
        binding?.clAd?.visibility = View.VISIBLE
        if(nativeLanguage!=null|| nativeLanguageAlt!=null){
            AdsHelper.bindNativeAdToContainerSmall(
                nativeLanguage?:nativeLanguageAlt,
                binding?.nativeAdView,
                binding?.shimmerContainerNative?.shimmerContainerNative,
                activity,
                binding?.shimmer,
            )
        }
        else{
            AdsHelper.bindNativeAdToContainerSmall(
                nativeEditAi,
                binding?.nativeAdView,
                binding?.shimmerContainerNative?.shimmerContainerNative,
                activity,
                binding?.shimmer,
            )
        }

    }

    private fun loadInterAds(activity: FragmentActivity) {
        if (AiFaceApp.isInterEditHf && AiFaceApp.isInterEdit) {
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

        } else if (AiFaceApp.isInterEdit) {
            loadInterHome(activity) { onLoaded ->
                if (onLoaded) {

                } else {

                }
            }
        }
    }

    fun showInterAd(
        currentActivity: FragmentActivity
    ) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false && canPresentHomeInterstitial()) {
                    GlobalLoader.show(currentActivity)
                    delay(1000)
                    if (canPresentHomeInterstitial()) {
                        interstitialHome?.showFullscreenAd(
                            activity = currentActivity,
                            contentCallback = object : FullScreenContentCallback() {
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
                                    LogUtils.printLog(
                                        "inter_home failed to shown",
                                        interstitialTrackedUnitId(interstitialHome)
                                    )
                                    com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                        requestPermission.value = true
                                        generateTheImage(currentActivity)
                                    }
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialHome = null
                                }
                            },
                            forFragment = true,
                            onContinue = {
                                GlobalLoader.hide(currentActivity)
                                interstitialHome = null
                                com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                    requestPermission.value = true
                                    generateTheImage(currentActivity)
                                }
                            },
                        )
                    } else {
                        GlobalLoader.hide(currentActivity)
                        requestPermission.value = true
                        generateTheImage(currentActivity)
                    }
                } else {
                    requestPermission.value = true
                    generateTheImage(currentActivity)
                }
            } catch (e: Exception) {
                requestPermission.value = true
                e.printStackTrace()
                generateTheImage(currentActivity)
            }
        }
    }

    private fun navigateNext(activity: FragmentActivity, bundle: Bundle) {
        findNavController().navigate(
            R.id.action_editFragment_to_resultFragment,
            bundle,
        )
    }

    private fun generateTheImage(activity: FragmentActivity){
        val imageUri = imageOneUri ?: return
        if (!validateSelectedImages()) return

        if (isEnhancerFlow) {
            homeViewModel.ensureCatalogLoaded()
            resolveEnhancerTemplate()
            if (!homeViewModel.isCatalogReady() || selectedItemId.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.result_loading_templates),
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
        }
        if (isAgingFlow && selectedAgingTemplate == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.aging_select_template_first),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        // template_uuid from the selected template's UUID-string id
        val templateUuid = selectedItemId.takeUnless { it.isNullOrEmpty() }

        if (templateUuid == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.missing_template_reference),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val prompt = selectedPrompt.orEmpty()

        viewModel.generateImage(
            templateUuid = templateUuid,
            prompt = prompt,
            imageOneUri = imageUri,
            imageTwoUri = imageTwoUri.takeIf { requiredImageCount >= 2 },
        )

        binding?.btnCreate?.isEnabled = false
        showGeneratingDialog(activity)
    }

    private fun bindSelectedUserImages() {
        val dual = requiredImageCount >= 2 && imageTwoUri != null
        if (dual) {
            binding?.imageEditPreview?.visibility = View.GONE
            binding?.llDualImagePreview?.visibility = View.VISIBLE
            imageOneUri?.let { uri ->
                binding?.imageEditPreviewOne?.load(uri) {
                    crossfade(true)
                    allowHardware(false)
                }
            }
            imageTwoUri?.let { uri ->
                binding?.imageEditPreviewTwo?.load(uri) {
                    crossfade(true)
                    allowHardware(false)
                }
            }
        } else {
            binding?.llDualImagePreview?.visibility = View.GONE
            binding?.imageEditPreview?.visibility = View.VISIBLE
            imageOneUri?.let { uri ->
                binding?.imageEditPreview?.load(uri) {
                    crossfade(true)
                    allowHardware(false)
                }
            }
        }
    }

    private fun validateSelectedImages(): Boolean {
        val maxSizeInBytes = 5 * 1024 * 1024 // 5 MB
        val uris = buildList {
            imageOneUri?.let(::add)
            if (requiredImageCount >= 2) {
                imageTwoUri?.let(::add)
            }
        }
        if (uris.isEmpty()) return false
        if (requiredImageCount >= 2 && imageTwoUri == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.select_two_images_required),
                Toast.LENGTH_SHORT,
            ).show()
            return false
        }
        if (uris.any { getFileSize(it) > maxSizeInBytes }) {
            Toast.makeText(
                requireContext(),
                "Maximum image size is 5mb",
                Toast.LENGTH_SHORT,
            ).show()
            return false
        }
        return true
    }

    fun showRewardedWithAd(
        activity: FragmentActivity,
        onReward: () -> Unit,
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
            onReward = { onReward() },
            onFailed = onFailed,
            onDismissed = onDismissed
        )
    }

    companion object{
        var isProClosedForEdit = MutableLiveData(false)
    }

}
