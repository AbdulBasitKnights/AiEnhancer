package com.aiface.aging.features.faceswap

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.bumptech.glide.Glide
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.GenerationRewardGate
import com.aiface.aging.shared.ads.loadInterHome
import com.aiface.aging.shared.ads.loadRewardedAd
import com.aiface.aging.shared.ads.showRewardedAd
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.data.model.faceswap.FaceSwapTemplateDto
import com.aiface.aging.databinding.DialogImgGeneratingBinding
import com.aiface.aging.databinding.FragmentFaceswapGenerateBinding
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FaceSwapGenerateFragment : Fragment() {

    private var _binding: FragmentFaceswapGenerateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FaceSwapGenerateViewModel by viewModels()
    private var mActivity: FragmentActivity? = null
    private var myCredits: Int? = null
    private var generatingDialog: Dialog? = null
    private var generationStarted = false
    private var rewardDialog: Dialog? = null

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
    ): View {
        _binding = FragmentFaceswapGenerateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FirebaseLogUtils.logEvent("face_swap_generate_scr_view", "user view face swap generate")

        mActivity?.let { activity ->
            loadAds(activity)
            loadRewardedAds(activity)
            loadInterAds(activity)
            binding.edittext.visibility = View.GONE
            binding.tvPrompt.visibility = View.GONE
            binding.imageEditPreview.visibility = View.GONE
            binding.imageEditPreviewSecond.visibility = View.GONE
            binding.imageEditPreviewEnhancer.visibility = View.VISIBLE

            applyRoots()
            observeState()
            clickListeners(activity)
            setupBackGuard()

            val template = readTemplate()

            binding.imageEditPreviewEnhancer.load(template?.displayPreviewUrl()) {
                crossfade(true)
                placeholder(R.drawable.placeholder_icon)
            }

            val selectedUris = getSelectedImageUris()
            if (selectedUris.isNotEmpty()) {
                binding.userImageContainer.visibility = View.VISIBLE
                binding.imageUserSelected.load(selectedUris.first()) {
                    crossfade(true)
                    allowHardware(false)
                }
            } else {
                binding.userImageContainer.visibility = View.GONE
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.credits.collect { credits ->
                        myCredits = credits
                        binding.tvCredits.text = credits.toString()
                    }
                }
            }
        }
    }

    private fun loadInterAds(activity: FragmentActivity) {
        if (AiFaceApp.isInterEdit) {
            loadInterHome(activity) { _ -> }
        }
    }

    private fun loadRewardedAds(activity: FragmentActivity) {
        if (isProVersion.value == false) {
            loadRewardedAd(
                activity,
                BuildConfig.reward_home_hf,
                BuildConfig.reward_home,
                AiFaceApp.isRewardFaceSwapGenerateHf,
                AiFaceApp.isRewardFaceSwapGenerate,
            ) { _ -> }
        }
    }

    private fun setupBackGuard() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.generateState.value is FaceSwapGenerateUiState.Loading) {
                        return
                    }
                    findNavController().navigateUp()
                }
            },
        )
    }

    private fun clickListeners(activity: FragmentActivity) {
        binding.btnBackEdit.setOnClickListener {
            if (viewModel.generateState.value is FaceSwapGenerateUiState.Loading) return@setOnClickListener
            findNavController().navigateUp()
        }

        binding.btnCreate.setOnClickListener {
            handleCreateClick(activity)
        }
    }

    private fun handleCreateClick(activity: FragmentActivity) {
        if (generationStarted) return
        val credits = myCredits ?: return
        if (credits < 1 && isProVersion.value == false) {
            showCreditsDialog()
            return
        }
        if (isProVersion.value == false) {
            GenerationRewardGate.showProOrWatchDialog(
                activity = activity,
                highFloorId = BuildConfig.reward_home_hf,
                normalId = BuildConfig.reward_home,
                isHf = AiFaceApp.isRewardFaceSwapGenerateHf || AiFaceApp.isRewardPromptHf,
                isNormal = AiFaceApp.isRewardFaceSwapGenerate || AiFaceApp.isRewardPrompt,
                isFromEdit = true,
                onUnlocked = { startGeneration(activity) },
            )
        } else {
            startGeneration(activity)
        }
    }

    private fun showRewardedDialogue(activity: FragmentActivity) {
        if (rewardDialog == null) {
            rewardDialog = DialogueUtils.getDialogue(activity, R.layout.dialog_reward)
        }

        FirebaseLogUtils.logEvent("reward_dialog_view", "user view pop-up premium")
        val closeBtn = rewardDialog?.findViewById<ImageView>(R.id.close_dg)
        val watchVideo = rewardDialog?.findViewById<ConstraintLayout>(R.id.watch_video)
        val goPremium = rewardDialog?.findViewById<ConstraintLayout>(R.id.goPremium)

        closeBtn?.setOnClickListener {
            if (!activity.isFinishing) rewardDialog?.cancel()
        }

        goPremium?.setOnClickListener {
            FirebaseLogUtils.logEvent(
                "reward_dialog_get_pro_click",
                "user click button get pro on pop-up premium",
            )
            val intt = Intent(activity, IAPActivity::class.java)
            intt.putExtra("isFromEdit", true)
            startActivity(intt)
        }

        watchVideo?.setOnClickListener {
            FirebaseLogUtils.logEvent(
                "reward_dialog_watch_ad_click",
                "user click button unlock on pop-up premium",
            )
            if (!activity.isFinishing) rewardDialog?.cancel()
            if (NetworkUtils.isOnline(activity)) {
                if (isProVersion.value == false) {
                    showRewardedAd(
                        activity,
                        BuildConfig.reward_home_hf,
                        BuildConfig.reward_home,
                        AiFaceApp.isRewardPromptHf,
                        AiFaceApp.isRewardPrompt,
                        onFailed = { showInterAd(activity) },
                        onDismissed = { startGeneration(activity) },
                    )
                } else {
                    startGeneration(activity)
                }
            } else {
                ToastUtils.showInternetWarningToast(activity)
            }
        }
        if (!activity.isFinishing) {
            rewardDialog?.show()
        }
    }

    /** No-op inter path — continue straight into generation. */
    fun showInterAd(currentActivity: FragmentActivity) {
        startGeneration(currentActivity)
    }

    private fun observeState() {
        viewModel.generateState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FaceSwapGenerateUiState.Success -> {
                    FirebaseLogUtils.logEvent(
                        "face_swap_generation_success",
                        "face swap generation succeeded",
                    )
                    val bundle = bundleOf(
                        "output_image_url" to state.outputImageUrl,
                        FaceSwapFragment.ARG_TEMPLATE to readTemplate(),
                    )
                    viewModel.resetState()
                    generationStarted = false
                    generatingDialog?.dismiss()
                    binding.btnCreate.isEnabled = true
                    findNavController().navigate(R.id.action_faceSwapGenerate_to_result, bundle)
                }
                is FaceSwapGenerateUiState.Error -> {
                    val msg = state.message.ifBlank { "Something went wrong" }
                    FirebaseLogUtils.logEvent("face_swap_generation_failed", msg)
                    viewModel.resetState()
                    generationStarted = false
                    generatingDialog?.dismiss()
                    binding.btnCreate.isEnabled = true
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                is FaceSwapGenerateUiState.Loading -> {
                    binding.btnCreate.isEnabled = false
                }
                FaceSwapGenerateUiState.Idle -> {
                    binding.btnCreate.isEnabled = true
                }
            }
        }
    }

    private fun startGeneration(activity: FragmentActivity) {
        if (generationStarted) return
        val imageUris = getSelectedImageUris()
        if (imageUris.isEmpty()) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }
        if (!validateImageSizes(imageUris)) return

        val templateId = readTemplate()?.id
            ?: arguments?.getString("item_id").orEmpty()
        if (templateId.isBlank()) {
            Toast.makeText(requireContext(), "Invalid template", Toast.LENGTH_SHORT).show()
            return
        }

        generationStarted = true
        showGeneratingDialog(activity)
        viewModel.startSwap(templateId, imageUris)
    }

    private fun showCreditsDialog() {
        startActivity(Intent(mActivity, IAPActivity::class.java))
    }

    private fun showGeneratingDialog(activity: FragmentActivity) {
        if (generatingDialog == null) {
            val dialogBinding = DialogImgGeneratingBinding.inflate(activity.layoutInflater)
            generatingDialog = Dialog(activity).apply {
                setContentView(dialogBinding.root)
                setCancelable(false)
            }
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
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
        }
    }

    private fun getSelectedImageUris(): List<Uri> {
        arguments?.getStringArrayList(ARG_IMAGE_URIS)
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it.map(Uri::parse) }

        return arguments?.getString(ARG_IMAGE_URI)
            ?.let { listOf(Uri.parse(it)) }
            ?: emptyList()
    }

    private fun validateImageSizes(imageUris: List<Uri>): Boolean {
        val maxSizeInBytes = 5 * 1024 * 1024
        val oversized = imageUris.firstOrNull { getFileSize(it) > maxSizeInBytes }
        if (oversized != null) {
            Toast.makeText(requireContext(), "Maximum image size is 5mb", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                it.moveToFirst()
                it.getLong(sizeIndex)
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun readTemplate(): FaceSwapTemplateDto? {
        val bundle = arguments ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(FaceSwapFragment.ARG_TEMPLATE, FaceSwapTemplateDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(FaceSwapFragment.ARG_TEMPLATE)
        }
    }

    private fun applyRoots() {
        binding.root.applySystemBarInsets(applyTop = true, applyBottom = true)
    }

    /** Next-Gen / pro path: hide native slot (no gms AdLoader). */
    private fun loadAds(activity: FragmentActivity) {
        _binding?.clAd?.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        mActivity?.let { activity ->
            loadAds(activity)
        }
    }

    override fun onDestroyView() {
        generatingDialog?.dismiss()
        generatingDialog = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_IMAGE_URI = "imageUri"
        private const val ARG_IMAGE_URIS = "imageUris"
        var isProClosedForFaceSwap = MutableLiveData(false)
    }
}
