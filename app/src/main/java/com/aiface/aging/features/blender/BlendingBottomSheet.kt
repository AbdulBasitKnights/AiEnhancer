package com.aiface.aging.features.blender

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentBlendingBottomSheetBinding
import com.aiface.aging.features.blender.catalog.BlenderCatalogUiState
import com.aiface.aging.features.blender.catalog.BlenderCatalogViewModel
import com.aiface.aging.features.blender.catalog.BlenderHeaderAdapter
import com.aiface.aging.features.blender.catalog.BlenderPackAdapter
import com.aiface.aging.features.editor.model.ModelFramePack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlendingBottomSheet : BottomSheetDialogFragment() {

    private var binding: FragmentBlendingBottomSheetBinding? = null
    private val viewModel: BlenderCatalogViewModel by viewModels()
    private var listener: BlendingBottomSheetPassData? = null

    private val headerAdapter by lazy {
        BlenderHeaderAdapter { _, category ->
            packAdapter.submit(category.packs)
        }
    }

    private val packAdapter by lazy {
        BlenderPackAdapter { pack -> onPackSelected(pack) }
    }

    fun setBlendingListener(passData: BlendingBottomSheetPassData) {
        listener = passData
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BlenderState.isBottomSheet = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBlendingBottomSheetBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.backArrow?.setOnClickListener { dismiss() }
        binding?.rcCategory?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.rcCategory?.adapter = headerAdapter
        binding?.rcPreview?.layoutManager = GridLayoutManager(requireContext(), 2)
        binding?.rcPreview?.adapter = packAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is BlenderCatalogUiState.Ready) {
                        headerAdapter.submit(state.categories, 0)
                        packAdapter.submit(state.categories.first().packs)
                    }
                }
            }
        }
        viewModel.load()
    }

    private fun onPackSelected(pack: ModelFramePack) {
        BlenderState.selectedFrame = pack
        val list = arrayListOf(pack)
        listener?.onSelectedBlendingItem(0, list)
        dismiss()
    }

    override fun onDestroyView() {
        BlenderState.isBottomSheet = false
        binding = null
        super.onDestroyView()
    }
}
