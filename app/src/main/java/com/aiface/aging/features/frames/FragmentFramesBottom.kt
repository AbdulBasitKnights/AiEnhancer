package com.aiface.aging.features.frames

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aiface.aging.databinding.FragmentFramesBottomBinding
import com.aiface.aging.features.blender.catalog.BlenderCatalogUiState
import com.aiface.aging.features.blender.catalog.BlenderHeaderAdapter
import com.aiface.aging.shared.editorui.BottomActionListener
import com.aiface.aging.features.editor.model.ModelFramePack
import com.aiface.aging.features.frames.adapters.FramesPackAdapter
import com.aiface.aging.features.frames.catalog.FramesCatalogViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * In-editor template picker bottom sheet content.
 */
@AndroidEntryPoint
class FragmentFramesBottom : Fragment() {

    private var binding: FragmentFramesBottomBinding? = null
    private val viewModel: FramesCatalogViewModel by viewModels()
    private var updateListener: FrameUpdateListener? = null
    private var actionListener: BottomActionListener? = null

    private val headerAdapter by lazy {
        BlenderHeaderAdapter(darkSurface = true) { _, category ->
            packAdapter.submit(category.packs)
        }
    }

    private val packAdapter by lazy {
        FramesPackAdapter(editorBottom = true) { pack -> onPackSelected(pack) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFramesBottomBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return

        b.btnTickTemplate.setOnClickListener {
            actionListener?.onActionTickClick("frame", null)
        }
        b.btnCrossTemplate.setOnClickListener {
            actionListener?.onActionCancelClick("frame", null)
        }

        b.frameHeadersRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.frameHeadersRecycler.adapter = headerAdapter
        b.framePacksRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.framePacksRecycler.adapter = packAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is BlenderCatalogUiState.Loading -> {
                            b.textLoading.isVisible = true
                            b.frameHeadersRecycler.isVisible = false
                            b.framePacksRecycler.isVisible = false
                        }
                        is BlenderCatalogUiState.Ready -> {
                            b.textLoading.isVisible = false
                            b.frameHeadersRecycler.isVisible = true
                            b.framePacksRecycler.isVisible = true
                            headerAdapter.submit(state.categories, 0)
                            packAdapter.submit(state.categories.firstOrNull()?.packs.orEmpty())
                        }
                        is BlenderCatalogUiState.Error -> {
                            b.textLoading.isVisible = false
                            b.frameHeadersRecycler.isVisible = false
                            b.framePacksRecycler.isVisible = false
                        }
                    }
                }
            }
        }
        viewModel.load()
    }

    private fun onPackSelected(pack: ModelFramePack) {
        updateListener?.onFrameUpdate(pack)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            header: Any? = null,
            listener: FrameUpdateListener? = null,
            pack: ModelFramePack? = null,
            flag: Boolean = false,
            extra: Any? = null,
            actionListener: BottomActionListener? = null,
        ): FragmentFramesBottom {
            return FragmentFramesBottom().apply {
                updateListener = listener
                this.actionListener = actionListener
            }
        }
    }
}
