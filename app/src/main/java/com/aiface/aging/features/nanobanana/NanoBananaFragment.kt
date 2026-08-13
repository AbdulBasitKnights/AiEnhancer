package com.aiface.aging.features.nanobanana

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentNanobananaBinding
import com.aiface.aging.domain.model.Template
import com.aiface.aging.features.home.HomeViewModel
import com.aiface.aging.features.home.ProPanelHomeObject
import com.aiface.aging.features.home.TemplateImageRequirements
import com.aiface.aging.features.see_all.SeeAllAdapter
import com.aiface.aging.features.see_all.SeeAllCategoryAdapter
import com.aiface.aging.features.see_all.SeeAllViewModel
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.GenerationRewardGate
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.shared.safeNavigate
import com.aiface.aging.utils.FirebaseLogUtils
import dagger.hilt.android.AndroidEntryPoint

/**
 * Bottom-nav tab: same templates catalog UI/flow as See All, without back arrow.
 */
@AndroidEntryPoint
class NanoBananaFragment : Fragment() {

    private var binding: FragmentNanobananaBinding? = null

    private val viewModel: SeeAllViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: SeeAllAdapter

    private var mActivity: FragmentActivity? = null

    private val categoryAdapter by lazy {
        SeeAllCategoryAdapter { _, category ->
            retainedCategoryId = category.id
            viewModel.selectCategory(category.id)
            binding?.rvTemplates?.scrollToPosition(0)
            retainedTemplatesScroll = null
        }
    }

    companion object {
        var isProClosedAfterRewardForNano = MutableLiveData(false)
        private var proPanelHomeObject: ProPanelHomeObject? = null

        /** Survives fragment recreate when leaving for preview / pager destroy. */
        private var retainedCategoryId: String? = null
        private var retainedTemplatesScroll: Parcelable? = null
        private var retainedCategoriesScroll: Parcelable? = null
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
        binding = FragmentNanobananaBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarInsets(applyTop = true, applyBottom = false)
        FirebaseLogUtils.logEvent("nanobanana_tab_view", "")

        mActivity?.let { activity ->
            binding?.tvTitle?.text = getString(R.string.ai_nanobanana)
            setupCategoryChips()
            setupRecycler(activity)
            observeCatalog()

            // Only shimmer when catalog empty — seed may already be ready.
            val hasSeed = homeViewModel.catalogCategories().isNotEmpty()
            setTemplatesShimmer(!hasSeed)
            homeViewModel.ensureCatalogLoaded()
            viewModel.loadCatalog(
                preferredCategoryId = preferredCategoryId(),
                seedCategories = homeViewModel.catalogCategories(),
            )

            isProClosedAfterRewardForNano.observe(viewLifecycleOwner, Observer {
                if (it) {
                    isProClosedAfterRewardForNano.value = false
                    safeNavigate(
                        R.id.action_homeFragment_to_previewFragment,
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
                        },
                    )
                }
            })
        }
    }

    private fun preferredCategoryId(): String? =
        viewModel.selectedCategoryId() ?: retainedCategoryId

    private fun setupCategoryChips() {
        binding?.rvCategories?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.rvCategories?.adapter = categoryAdapter
    }

    private fun setupRecycler(activity: FragmentActivity) {
        adapter = SeeAllAdapter({ template -> openTemplate(template) }, requireActivity())
        binding?.rvTemplates?.layoutManager = GridLayoutManager(requireContext(), 2)
        binding?.rvTemplates?.adapter = adapter
    }

    private fun observeCatalog() {
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading == true && adapter.currentList.isEmpty()) {
                setTemplatesShimmer(true)
            }
            updateEmptyState()
        }
        viewModel.templatesLoading.observe(viewLifecycleOwner) {
            updateEmptyState()
        }
        viewModel.error.observe(viewLifecycleOwner) {
            setTemplatesShimmer(false)
            updateEmptyState()
        }
        viewModel.categories.observe(viewLifecycleOwner) { list ->
            categoryAdapter.submitList(list) {
                val selectedId = viewModel.selectedCategoryId() ?: retainedCategoryId
                val index = list.indexOfFirst { it.id == selectedId }.let { found ->
                    if (found >= 0) found else 0
                }
                if (list.isNotEmpty()) {
                    retainedCategoryId = list[index].id
                    categoryAdapter.selectMode(index)
                    binding?.rvCategories?.post {
                        val catsState = retainedCategoriesScroll
                        if (catsState != null) {
                            binding?.rvCategories?.layoutManager?.onRestoreInstanceState(catsState)
                            retainedCategoriesScroll = null
                        } else {
                            binding?.rvCategories?.scrollToPosition(index)
                        }
                    }
                }
                updateEmptyState()
            }
        }
        viewModel.templates.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list) {
                viewModel.markTemplatesBound()
                setTemplatesShimmer(false)
                binding?.rvTemplates?.post {
                    retainedTemplatesScroll?.let { state ->
                        binding?.rvTemplates?.layoutManager?.onRestoreInstanceState(state)
                        retainedTemplatesScroll = null
                    }
                }
                updateEmptyState()
            }
        }
        homeViewModel.theHomeItems.observe(viewLifecycleOwner) {
            if (viewModel.categories.value.isNullOrEmpty()) {
                val seed = homeViewModel.catalogCategories()
                if (seed.isNotEmpty()) {
                    viewModel.loadCatalog(preferredCategoryId(), seed)
                } else if (homeViewModel.isCatalogReady()) {
                    // Home finished with no categories — try direct fetch / show empty.
                    viewModel.loadCatalog(preferredCategoryId(), emptyList())
                }
            }
            updateEmptyState()
        }
    }

    /** Overlay only — never hide rvTemplates (hiding cancels image loads). */
    private fun setTemplatesShimmer(show: Boolean) {
        val shimmerHost = binding?.templatesShimmer
        shimmerHost?.isVisible = show
        binding?.rvTemplates?.isVisible = true
        val shimmer = shimmerHost?.getChildAt(0) as? ShimmerFrameLayout
        if (show) shimmer?.startShimmer() else shimmer?.stopShimmer()
        if (show) binding?.tvEmpty?.isVisible = false
    }

    private fun updateEmptyState() {
        val b = binding ?: return
        val loading =
            viewModel.loading.value == true ||
                viewModel.templatesLoading.value == true ||
                b.templatesShimmer.isVisible
        val hasData =
            !viewModel.categories.value.isNullOrEmpty() ||
                !viewModel.templates.value.isNullOrEmpty() ||
                adapter.currentList.isNotEmpty()
        val showEmpty = !loading && !hasData
        b.tvEmpty.isVisible = showEmpty
        b.rvCategories.isVisible = hasData
    }

    private fun openTemplate(template: Template) {
        val category = viewModel.selectedCategory()
        val categoryId = category?.id.orEmpty()
        val categoryName = category?.name.orEmpty()
        FirebaseLogUtils.logEvent("nanobanana_template_click", "")

        // Snapshot before navigate — view may be destroyed under MainFragment.
        retainedCategoryId = categoryId.ifBlank { retainedCategoryId }
        binding?.let { b ->
            retainedTemplatesScroll = b.rvTemplates.layoutManager?.onSaveInstanceState()
            retainedCategoriesScroll = b.rvCategories.layoutManager?.onSaveInstanceState()
        }

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

        val host = activity as? FragmentActivity ?: return
        GenerationRewardGate.gateHomePremiumTemplate(
            activity = host,
            isPremiumItem = template.isPro,
            onContinue = {
                safeNavigate(R.id.action_homeFragment_to_previewFragment, bundle)
            },
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
                    android.content.Intent(
                        host,
                        com.aiface.aging.features.iap.IAPActivity::class.java,
                    ).apply {
                        putExtra("isFromProPanel", true)
                        putExtra("isFromHome", true)
                    },
                )
            },
        )
    }

    override fun onDestroyView() {
        binding?.let { b ->
            retainedCategoryId = viewModel.selectedCategoryId() ?: retainedCategoryId
            retainedTemplatesScroll =
                b.rvTemplates.layoutManager?.onSaveInstanceState() ?: retainedTemplatesScroll
            retainedCategoriesScroll =
                b.rvCategories.layoutManager?.onSaveInstanceState() ?: retainedCategoriesScroll
        }
        super.onDestroyView()
    }
}
