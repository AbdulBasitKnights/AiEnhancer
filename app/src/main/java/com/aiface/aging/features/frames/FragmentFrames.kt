package com.aiface.aging.features.frames

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.aiface.aging.databinding.FragmentFramesBinding
import com.aiface.aging.features.blender.catalog.BlenderCatalogUiState
import com.aiface.aging.features.blender.catalog.BlenderHeaderAdapter
import com.aiface.aging.features.editor.model.ModelFramePack
import com.aiface.aging.features.frames.adapters.FramesPackAdapter
import com.aiface.aging.features.frames.catalog.FramesCatalogViewModel
import com.aiface.aging.features.frames.editor.AllFramesEditorActivity
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.features.imgpicker.util.Extras
import com.aiface.aging.shared.ads.showPickerInterstitial
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentFrames : Fragment() {

    private var binding: FragmentFramesBinding? = null
    private val viewModel: FramesCatalogViewModel by viewModels()

    private val headerAdapter by lazy {
        BlenderHeaderAdapter { _, category ->
            packAdapter.submit(category.packs)
        }
    }

    private val packAdapter by lazy {
        FramesPackAdapter { pack ->
            activity?.let { openTemplate(pack, it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFramesBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarInsets(applyTop = true, applyBottom = true)
        FirebaseLogUtils.logEvent("frames_scr_view", "user view frames screen")
        val activity = requireActivity()

        binding?.btnBackEdit?.setOnClickListener {
            findNavController().navigateUp()
        }

        binding?.frameHeadersRecycler?.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding?.frameHeadersRecycler?.adapter = headerAdapter
        binding?.framePacksRecycler?.layoutManager = GridLayoutManager(activity, 2)
        binding?.framePacksRecycler?.adapter = packAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
        viewModel.load()
    }

    private fun render(state: BlenderCatalogUiState) {
        val b = binding ?: return
        when (state) {
            is BlenderCatalogUiState.Loading -> {
                b.shimmerLayout?.isVisible = true
                b.frameHeadersRecycler?.isVisible = false
                b.framePacksRecycler?.isVisible = false
                b.loadingAnim?.isVisible = true
                b.noInternetAnim?.isVisible = false
            }
            is BlenderCatalogUiState.Ready -> {
                b.shimmerLayout?.isVisible = false
                b.loadingAnim?.isVisible = false
                b.noInternetAnim?.isVisible = false
                b.frameHeadersRecycler?.isVisible = true
                b.framePacksRecycler?.isVisible = true
                headerAdapter.submit(state.categories, 0)
                packAdapter.submit(state.categories.firstOrNull()?.packs.orEmpty())
            }
            is BlenderCatalogUiState.Error -> {
                b.shimmerLayout?.isVisible = false
                b.loadingAnim?.isVisible = false
                b.frameHeadersRecycler?.isVisible = false
                b.framePacksRecycler?.isVisible = false
                b.noInternetAnim?.isVisible = true
                activity?.let { ToastUtils.showToast(it, "Failed to load data") }
            }
        }
    }

    private fun openTemplate(pack: ModelFramePack, activity: FragmentActivity) {
        if (!NetworkUtils.isOnline(activity)) {
            ToastUtils.showInternetWarningToast(activity)
            return
        }
        FirebaseLogUtils.logEvent("frames_select_template_click", "")
        showPickerInterstitial(activity) {
            val imageCount = resolveImageCount(pack)
            val bundle = Bundle().apply {
                putParcelable(Extras.MODEL_FRAME_PACK, pack)
            }
            TedImagePicker.with(activity, "frames")
                .destinationIntent(Intent(activity, AllFramesEditorActivity::class.java))
                .max(imageCount, "cannot select more than $imageCount image(s)")
                .min(imageCount, "select at least $imageCount image(s)")
                .bundleExtras(bundle)
                .albumType(AlbumType.DROP_DOWN)
                .startMultiImageFragment()
        }
    }

    private fun resolveImageCount(pack: ModelFramePack): Int {
        var count = 0
        if (!pack.constraintSet1.isNullOrBlank()) count++
        if (!pack.constraintSet2.isNullOrBlank()) count++
        if (!pack.constraintSet3.isNullOrBlank()) count++
        return count.coerceAtLeast(1)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
