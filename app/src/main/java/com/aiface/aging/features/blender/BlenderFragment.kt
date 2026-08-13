package com.aiface.aging.features.blender

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
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentBlenderBinding
import com.aiface.aging.features.blender.catalog.BlenderCatalogUiState
import com.aiface.aging.features.blender.catalog.BlenderCatalogViewModel
import com.aiface.aging.features.blender.catalog.BlenderHeaderAdapter
import com.aiface.aging.features.blender.catalog.BlenderPackAdapter
import com.aiface.aging.features.blender.editor.BlendActivity
import com.aiface.aging.features.editor.model.ModelFramePack
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.shared.ads.showPickerInterstitial
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.ImageUtils
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlenderFragment : Fragment() {

    private var binding: FragmentBlenderBinding? = null
    private val viewModel: BlenderCatalogViewModel by viewModels()

    private val headerAdapter by lazy {
        BlenderHeaderAdapter { _, category ->
            packAdapter.submit(category.packs)
        }
    }

    private val packAdapter by lazy {
        BlenderPackAdapter { pack ->
            mActivity()?.let { openTemplate(pack, it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBlenderBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarInsets(applyTop = true, applyBottom = true)
        FirebaseLogUtils.logEvent("blender_scr_view", "user view blender screen")
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
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
        viewModel.load()
    }

    private fun render(state: BlenderCatalogUiState) {
        val b = binding ?: return
        when (state) {
            is BlenderCatalogUiState.Loading -> {
                b.shimmerLayout?.isVisible = true
                b.frameHeadersRecycler.isVisible = false
                b.framePacksRecycler.isVisible = false
                b.loadingAnim?.isVisible = true
                b.noInternetAnim?.isVisible = false
            }
            is BlenderCatalogUiState.Ready -> {
                b.shimmerLayout?.isVisible = false
                b.loadingAnim?.isVisible = false
                b.noInternetAnim?.isVisible = false
                b.frameHeadersRecycler.isVisible = true
                b.framePacksRecycler.isVisible = true
                headerAdapter.submit(state.categories, 0)
                packAdapter.submit(state.categories.first().packs)
            }
            is BlenderCatalogUiState.Error -> {
                b.shimmerLayout?.isVisible = false
                b.loadingAnim?.isVisible = false
                b.frameHeadersRecycler.isVisible = false
                b.framePacksRecycler.isVisible = false
                b.noInternetAnim?.isVisible = true
                mActivity()?.let { ToastUtils.showToast(it, "Failed to load data") }
            }
        }
    }

    private fun openTemplate(pack: ModelFramePack, activity: FragmentActivity) {
        if (!NetworkUtils.isOnline(activity)) {
            ToastUtils.showInternetWarningToast(activity)
            return
        }
        FirebaseLogUtils.logEvent("blender_select_template_click", "user selected blender template")
        showPickerInterstitial(activity) {
            val bundle = Bundle().apply { putString("frameType", pack.file) }
            BlenderState.selectedFrame = pack
            val cached = ImageUtils.isImageCached(requireContext(), pack.file)
            if (cached || pack.file.isNullOrBlank().not()) {
                openImagePicker(activity, bundle)
            }
        }
    }

    private fun openImagePicker(activity: FragmentActivity, bundle: Bundle) {
        FirebaseLogUtils.logEvent("blender_open_editor", "user opened blender image picker")
        TedImagePicker.with(activity, "blender")
            .destinationIntent(Intent(activity, BlendActivity::class.java))
            .max(1, "cannot select more than 1 image")
            .min(1, "select at least 1 image")
            .bundleExtras(bundle)
            .albumType(AlbumType.DROP_DOWN)
            .startMultiImageFragment()
    }

    private fun mActivity(): FragmentActivity? = activity

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
