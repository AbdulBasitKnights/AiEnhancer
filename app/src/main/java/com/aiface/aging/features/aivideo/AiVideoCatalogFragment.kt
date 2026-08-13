package com.aiface.aging.features.aivideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentAiVideoCatalogBinding
import com.aiface.aging.domain.model.Category
import com.aiface.aging.domain.model.Template
import com.aiface.aging.features.home.HomeViewModel
import com.aiface.aging.features.home.TemplateImageRequirements
import com.aiface.aging.features.mywork.LibraryCategorySectionsAdapter
import com.aiface.aging.shared.safeNavigate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiVideoCatalogFragment : Fragment() {

    private var binding: FragmentAiVideoCatalogBinding? = null
    private val homeViewModel: HomeViewModel by activityViewModels()
    private var categoriesAdapter: LibraryCategorySectionsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAiVideoCatalogBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as? FragmentActivity ?: return
        setupRecycler(activity)
        observeCatalog()
        homeViewModel.getHomeItems()
    }

    override fun onDestroyView() {
        binding = null
        categoriesAdapter = null
        super.onDestroyView()
    }

    private fun isViewReady(): Boolean = isAdded && view != null && binding != null

    private fun setupRecycler(activity: FragmentActivity) {
        categoriesAdapter = LibraryCategorySectionsAdapter(
            activity = activity,
            onTemplateClick = { template, categoryName, categoryId ->
                navigateToTemplatePreview(template, categoryName, categoryId)
            },
            onSeeAllClick = { category -> navigateToSeeAll(category) },
        )
        binding?.rvCategories?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoriesAdapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun observeCatalog() {
        homeViewModel.videoCatalogCategories.observe(viewLifecycleOwner) { categories ->
            if (!isViewReady()) return@observe
            refreshContent(categories.orEmpty(), homeViewModel.loading.value == true)
        }
        homeViewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (!isViewReady()) return@observe
            val categories = homeViewModel.videoCatalogCategories.value.orEmpty()
            refreshContent(categories, loading == true && categories.isEmpty())
        }
    }

    private fun refreshContent(categories: List<Category>, showLoading: Boolean) {
        val binding = binding ?: return
        when {
            showLoading -> {
                binding.rvCategories.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                categoriesAdapter?.showLoadingShimmer()
            }
            categories.isEmpty() -> {
                binding.rvCategories.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
            else -> {
                binding.rvCategories.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                categoriesAdapter?.submitCategories(categories)
            }
        }
    }

    private fun navigateToTemplatePreview(
        template: Template,
        categoryName: String,
        categoryId: String,
    ) {
        if (!isAdded) return
        safeNavigate(
            R.id.action_homeFragment_to_previewFragment,
            Bundle().apply {
                putString("item_id", template.id)
                putString("prompt", template.prompt.orEmpty())
                putString("url", template.thumbnailUrl ?: template.mediaUrl.orEmpty())
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

    private fun navigateToSeeAll(category: Category) {
        if (!isAdded) return
        safeNavigate(
            R.id.action_homeFragment_to_seeAllFragment,
            Bundle().apply {
                putString("category_id", category.id)
                putString("category_name", category.name)
            },
        )
    }
}
