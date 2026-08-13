package com.aiface.aging.features.faceswap

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.aiface.aging.data.model.faceswap.FaceSwapCategoryDto
import com.aiface.aging.data.model.faceswap.FaceSwapTemplateDto
import com.aiface.aging.databinding.FragmentFaceSwapBinding
import com.aiface.aging.features.faceswap.adapters.FaceSwapCategoryAdapter
import com.aiface.aging.features.faceswap.adapters.FaceSwapTemplateAdapter
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FaceSwapFragment : Fragment() {

    private var binding: FragmentFaceSwapBinding? = null
    private val viewModel: FaceSwapViewModel by viewModels()
    private var mActivity: FragmentActivity? = null

    private var allTemplates: List<FaceSwapTemplateDto> = emptyList()
    private var categories: List<FaceSwapCategoryDto> = emptyList()
    private var selectedCategoryId: String? = null

    private val categoryAdapter by lazy {
        FaceSwapCategoryAdapter { _, category ->
            selectedCategoryId = category.id
            // Frames-style: swap list in place — no shimmer / hide (avoids blink).
            showTemplatesForCategory(category.id)
            binding?.rvTemplates?.scrollToPosition(0)
        }
    }

    private val templateAdapter by lazy {
        FaceSwapTemplateAdapter { template ->
            openImagePicker(template)
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
    ): View {
        binding = FragmentFaceSwapBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarInsets(applyTop = true, applyBottom = true)
        FirebaseLogUtils.logEvent("faceswap_scr_view", "")
        binding?.btnBack?.setOnClickListener { findNavController().navigateUp() }
        binding?.btnRetry?.setOnClickListener { viewModel.loadCatalog(force = true) }
        binding?.rvCategories?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.rvCategories?.adapter = categoryAdapter
        binding?.rvTemplates?.layoutManager = GridLayoutManager(requireContext(), 2)
        binding?.rvTemplates?.adapter = templateAdapter

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FaceSwapCatalogUiState.Idle,
                is FaceSwapCatalogUiState.Loading -> {
                    binding?.shimmerLayout?.isVisible = true
                    binding?.tvError?.isVisible = false
                    binding?.btnRetry?.isVisible = false
                    binding?.rvCategories?.isVisible = false
                    binding?.rvTemplates?.isVisible = false
                }
                is FaceSwapCatalogUiState.Success -> {
                    binding?.shimmerLayout?.isVisible = false
                    binding?.tvError?.isVisible = false
                    binding?.btnRetry?.isVisible = false
                    binding?.rvCategories?.isVisible = true
                    binding?.rvTemplates?.isVisible = true
                    categories = state.categories
                    allTemplates = state.templates
                    categoryAdapter.submitList(categories) {
                        val preferredId = selectedCategoryId
                        val index = categories.indexOfFirst { it.id == preferredId }.let { found ->
                            if (found >= 0) found else 0
                        }
                        if (categories.isNotEmpty()) {
                            selectedCategoryId = categories[index].id
                            categoryAdapter.selectMode(index)
                            showTemplatesForCategory(categories[index].id)
                        } else {
                            selectedCategoryId = null
                            templateAdapter.submitList(allTemplates)
                        }
                    }
                }
                is FaceSwapCatalogUiState.Empty,
                is FaceSwapCatalogUiState.Error -> {
                    binding?.shimmerLayout?.isVisible = false
                    binding?.rvCategories?.isVisible = false
                    binding?.rvTemplates?.isVisible = false
                    binding?.tvError?.isVisible = true
                    binding?.btnRetry?.isVisible = true
                    val msg = (state as? FaceSwapCatalogUiState.Error)?.message
                        ?: getString(com.aiface.aging.R.string.face_swap_empty)
                    binding?.tvError?.text = msg
                }
            }
        }
        viewModel.loadCatalog()
    }

    private fun showTemplatesForCategory(categoryId: String?) {
        val filtered = viewModel.templatesForCategory(categoryId, allTemplates)
        templateAdapter.submitList(if (filtered.isEmpty()) allTemplates else filtered)
    }

    private fun openImagePicker(template: FaceSwapTemplateDto) {
        val activity = mActivity ?: return
        FirebaseLogUtils.logEvent("faceswap_template_click", "")
        val count = template.resolveRequiredImageCount().coerceAtLeast(1)
        val bundle = bundleOf(
            "face_swap_template" to template,
            "item_id" to template.id,
            "title" to (template.name ?: "Face Swap"),
            "imgCount" to count,
        )
        TedImagePicker.with(activity, "faceswap")
            .max(count, "cannot select more than $count image(s)")
            .min(count, "select at least $count image(s)")
            .bundleExtras(bundle)
            .albumType(AlbumType.DROP_DOWN)
            .startMultiImageFragment()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_TEMPLATE = "face_swap_template"
    }
}
