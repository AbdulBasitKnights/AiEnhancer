package com.aiface.aging.features.mywork

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemHomeAiParentBinding
import com.aiface.aging.databinding.ItemHomeCategoryShimmerBinding
import com.aiface.aging.domain.model.Category
import com.aiface.aging.domain.model.Template
import com.aiface.aging.features.home.CategoryAdapter
import com.aiface.aging.shared.ads.HomeNativeAdManager
import com.aiface.aging.shared.setSafeClickListener

class LibraryCategorySectionsAdapter(
    private val activity: FragmentActivity,
    private val onTemplateClick: (Template, String, String) -> Unit,
    private val onSeeAllClick: (Category) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SHIMMER = 0
        private const val VIEW_TYPE_CATEGORY = 1
        private const val SHIMMER_ROW_COUNT = 3
    }

    private var categories: List<Category> = emptyList()
    private var showShimmer = false

    fun submitCategories(list: List<Category>) {
        showShimmer = false
        categories = list
        notifyDataSetChanged()
    }

    fun showLoadingShimmer() {
        showShimmer = true
        categories = emptyList()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (showShimmer) VIEW_TYPE_SHIMMER else VIEW_TYPE_CATEGORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SHIMMER -> ShimmerViewHolder(
                ItemHomeCategoryShimmerBinding.inflate(inflater, parent, false),
            )
            else -> CategorySectionViewHolder(
                ItemHomeAiParentBinding.inflate(inflater, parent, false),
            )
        }
    }

    override fun getItemCount(): Int {
        return if (showShimmer) SHIMMER_ROW_COUNT else categories.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategorySectionViewHolder -> holder.bind(categories[position])
            is ShimmerViewHolder -> Unit
        }
    }

    inner class CategorySectionViewHolder(
        private val binding: ItemHomeAiParentBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.tvCategoryTitle.text = category.name
            val adapter = CategoryAdapter(
                onTemplateClick = onTemplateClick,
                categoryName = category.name,
                categoryId = category.id,
                context = activity,
            )
            binding.rvTemplates.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                this.adapter = adapter
            }
            adapter.submitList(
                category.templates.take(5),
                HomeNativeAdManager.getNativeAds(),
            )
            binding.btnSeeAll.setSafeClickListener {
                onSeeAllClick(category)
            }
        }
    }

    private class ShimmerViewHolder(
        binding: ItemHomeCategoryShimmerBinding,
    ) : RecyclerView.ViewHolder(binding.root)
}
