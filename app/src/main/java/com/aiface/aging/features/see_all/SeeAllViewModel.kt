package com.aiface.aging.features.see_all

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aiface.aging.domain.model.Category
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.model.Template
import com.aiface.aging.domain.usecase.GetCategoriesUseCase
import com.aiface.aging.shared.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeeAllViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
) : BaseViewModel() {

    private val _categories = MutableLiveData<List<Category>>(emptyList())
    val categories: LiveData<List<Category>> = _categories

    private val _templates = MutableLiveData<List<Template>>(emptyList())
    val templates: LiveData<List<Template>> = _templates

    private val _templatesLoading = MutableLiveData(false)
    val templatesLoading: LiveData<Boolean> = _templatesLoading

    private var selectedCategoryId: String? = null

    fun loadCatalog(
        preferredCategoryId: String?,
        seedCategories: List<Category> = emptyList(),
    ) {
        val seed = normalizeCategories(seedCategories)
        if (seed.isNotEmpty()) {
            applyCatalog(seed, preferredCategoryId, showSwitchLoading = false)
            return
        }

        viewModelScope.launch {
            getCategoriesUseCase().onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        handleLoading(false)
                        applyCatalog(
                            normalizeCategories(result.data.orEmpty()),
                            preferredCategoryId,
                            showSwitchLoading = false,
                        )
                    }
                    is Resource.Error -> {
                        handleLoading(false)
                        _templatesLoading.value = false
                        handleError(result.message)
                        _categories.value = emptyList()
                        _templates.value = emptyList()
                    }
                    is Resource.Loading -> {
                        handleLoading(true)
                        _templatesLoading.value = true
                    }
                }
            }.launchIn(this)
        }
    }

    fun selectCategory(categoryId: String?) {
        if (categoryId == selectedCategoryId && !_templates.value.isNullOrEmpty()) {
            markTemplatesBound()
            return
        }
        applySelectedCategory(categoryId, showSwitchLoading = false)
    }

    fun selectedCategory(): Category? =
        _categories.value.orEmpty().firstOrNull { it.id == selectedCategoryId }

    fun selectedCategoryId(): String? = selectedCategoryId

    fun markTemplatesBound() {
        _templatesLoading.value = false
        handleLoading(false)
    }

    /** @deprecated Prefer [loadCatalog] so all categories are available for chips. */
    fun loadCategoryTemplates(categoryId: String) {
        loadCatalog(categoryId)
    }

    private fun applyCatalog(
        list: List<Category>,
        preferredCategoryId: String?,
        showSwitchLoading: Boolean,
    ) {
        _categories.value = list
        val initialId = when {
            !preferredCategoryId.isNullOrBlank() &&
                list.any { it.id == preferredCategoryId } -> preferredCategoryId
            !selectedCategoryId.isNullOrBlank() &&
                list.any { it.id == selectedCategoryId } -> selectedCategoryId
            list.isNotEmpty() -> list.first().id
            else -> null
        }
        applySelectedCategory(initialId, showSwitchLoading)
    }

    private fun applySelectedCategory(categoryId: String?, showSwitchLoading: Boolean) {
        selectedCategoryId = categoryId
        if (showSwitchLoading) {
            _templatesLoading.value = true
        }
        val category = _categories.value.orEmpty().firstOrNull { it.id == categoryId }
        _templates.value = category?.templates.orEmpty()
        if (!showSwitchLoading) {
            _templatesLoading.value = false
        }
    }

    private fun normalizeCategories(source: List<Category>): List<Category> {
        val withTemplates = source.filter { it.templates.isNotEmpty() }
        val active = withTemplates.filter { it.isActive }
        return active.ifEmpty { withTemplates }
    }
}
