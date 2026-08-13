package com.aiface.aging.features.home

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aiface.aging.R
import com.aiface.aging.data.local.PreferenceManager
import com.aiface.aging.domain.model.CategoryCatalog
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.usecase.GetCategoriesUseCase
import com.aiface.aging.features.home.aging.AgingTemplateCatalog
import com.aiface.aging.features.home.aging.AgingTemplateOption
import com.aiface.aging.features.tools.ToolsFeature
import com.aiface.aging.shared.BaseViewModel
import com.aiface.aging.shared.CreditManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val preferenceManager: PreferenceManager,
    private val creditManager: CreditManager,
) : BaseViewModel() {

    private val _homeItems = MutableLiveData<List<HomeItem>>()
    val theHomeItems: LiveData<List<HomeItem>> = _homeItems

    private val _videoCatalogCategories = MutableLiveData<List<com.aiface.aging.domain.model.Category>>(emptyList())
    val videoCatalogCategories: LiveData<List<com.aiface.aging.domain.model.Category>> = _videoCatalogCategories

    private val _userToken = MutableLiveData<String?>()
    val userToken: LiveData<String?> = _userToken

    val credits = creditManager.creditsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0,
        )

    private var homeItemsJob: Job? = null

    private fun hasLoadedCategories(): Boolean {
        return _homeItems.value?.any { it is HomeItem.CategoryItem } == true
    }

    fun getAgingTemplateOptions(): List<AgingTemplateOption> {
        return AgingTemplateCatalog.resolve(_homeItems.value.orEmpty())
    }

    fun hasAgingCategory(): Boolean {
        return HomeAiTemplateResolver.agingCategory(_homeItems.value.orEmpty()) != null
    }

    fun catalogCategories(): List<com.aiface.aging.domain.model.Category> {
        return _homeItems.value
            .orEmpty()
            .filterIsInstance<HomeItem.CategoryItem>()
            .map { it.category }
            .filter { it.templates.isNotEmpty() }
    }

    fun templatesForCategory(categoryId: String?, categoryName: String?): List<com.aiface.aging.domain.model.Template> {
        val categories = catalogCategories()
        val match = when {
            !categoryId.isNullOrBlank() -> categories.find { it.id == categoryId }
            !categoryName.isNullOrBlank() ->
                categories.find { it.name.equals(categoryName, ignoreCase = true) }
            else -> null
        }
        return match?.templates.orEmpty()
    }

    fun ensureCatalogLoaded() {
        if (!hasLoadedCategories()) {
            getHomeItems()
        }
    }

    fun isCatalogReady(): Boolean = hasLoadedCategories()

    fun enhancerTemplateId(): String {
        return HomeAiTemplateResolver.enhancerTemplateId(_homeItems.value.orEmpty())
    }

    fun enhancerPrompt(templateId: String? = null): String {
        return HomeAiTemplateResolver.enhancerPrompt(_homeItems.value.orEmpty(), templateId)
    }

    fun getHomeItems(forceRefresh: Boolean = false) {
        if (!forceRefresh && hasLoadedCategories()) return

        homeItemsJob?.cancel()
        homeItemsJob =
            viewModelScope.launch {
                getCategoriesUseCase().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            Log.e("HomeViewModel", "getHomeItems: ${result.data}")
                            handleLoading(false)
                            val categories = result.data.orEmpty()
                            val photoCategories = CategoryCatalog.photoCategories(categories)
                            _videoCatalogCategories.postValue(CategoryCatalog.videoCategories(categories))
                            if (photoCategories.isEmpty()) {
                                if (!hasLoadedCategories()) {
                                    postCatalogMessage(
                                        appContext.getString(R.string.home_empty_templates_message),
                                    )
                                }
                            } else {
                                _homeItems.postValue(
                                    photoCategories.map { HomeItem.CategoryItem(it) },
                                )
                            }
                        }

                        is Resource.Error -> {
                            Log.e("HomeViewModel", "getHomeItems: ${result.message}")
                            handleLoading(false)
                            handleError(result.message)
                            if (hasLoadedCategories()) return@collect
                            // TokenAuthenticator already refreshed+retried once on 401.
                            val message = if (isAuthError(result.message)) {
                                appContext.getString(R.string.no_data_currently_available)
                            } else {
                                appContext.getString(R.string.home_load_failed_message)
                            }
                            postCatalogMessage(message)
                        }

                        is Resource.Loading -> {
                            Log.e("HomeViewModel", "getHomeItems: Loading...")
                            handleLoading(true)
                            if (!hasLoadedCategories()) {
                                postCategoryShimmer()
                            }
                        }
                    }
                }
            }
    }

    fun clearHomeCache() {
        homeItemsJob?.cancel()
        homeItemsJob = null
        _homeItems.value = emptyList()
        _videoCatalogCategories.value = emptyList()
    }

    private fun postCategoryShimmer() {
        val staticItems =
            (_homeItems.value ?: emptyList()).filterIsInstance<HomeItem.ImageToImageButton>()
        _homeItems.postValue(
            staticItems +
                listOf(
                    HomeItem.CategoryShimmer(),
                    HomeItem.CategoryShimmer(),
                    HomeItem.CategoryShimmer(),
                ),
        )
    }

    private fun postCatalogMessage(message: String) {
        val staticItems =
            (_homeItems.value ?: emptyList()).filterIsInstance<HomeItem.ImageToImageButton>()
        _homeItems.postValue(staticItems + HomeItem.OfflineMessage(message))
    }

    private fun isAuthError(message: String?): Boolean {
        val m = message?.lowercase().orEmpty()
        return m.contains("401") ||
            m.contains("403") ||
            m.contains("unauthorized") ||
            m.contains("authentication") ||
            m.contains("invalid token") ||
            m.contains("not authenticated")
    }

    fun observeUserToken() {
        viewModelScope.launch {
            preferenceManager.userToken.collect { token ->
                _userToken.value = token
            }
        }
    }

    fun saveToken(token: String) {
        viewModelScope.launch {
            preferenceManager.saveUserToken(token)
        }
    }

    fun getFeatureList(context: FragmentActivity): Flow<List<HomeFeature>> = flow {
        emit(buildHomeFeatureList(context, includeAllTools = false))
    }

    fun getToolsFeatureList(context: FragmentActivity): Flow<List<ToolsFeature>> = flow {
        emit(buildToolsFeatureList(context, includeAllTools = true))
    }

    fun buildHomeToolsPreviewList(context: FragmentActivity): List<ToolsFeature> {
        return emptyList()
    }

    fun buildHomeDisplayList(items: List<HomeItem>): List<HomeItem> {
        val contentItems =
            items.filterNot {
                it is HomeItem.ToolsSection ||
                    it is HomeItem.CarouselSection ||
                    it is HomeItem.PromoBanner
            }
        val promo = eraserPromoBanner()
        val result = mutableListOf<HomeItem>()
        var categoryCount = 0

        contentItems.forEach { item ->
            result.add(item)
            if (item is HomeItem.CategoryItem) {
                categoryCount++
                if (categoryCount == 2) {
                    result.add(promo)
                }
            }
        }
        return result
    }

    private fun eraserPromoBanner(): HomeItem.PromoBanner {
        return HomeItem.PromoBanner(
            titleRes = R.string.home_promo_eraser_title,
            subtitleRes = R.string.home_promo_eraser_subtitle,
            ctaRes = R.string.home_promo_eraser_cta,
            imageRes = R.drawable.bg_remover_home,
        )
    }

    private fun buildHomeFeatureList(
        context: FragmentActivity,
        includeAllTools: Boolean,
    ): List<HomeFeature> {
        // Home primary actions live as circle buttons; keep list empty so the chip strip stays unused.
        return emptyList()
    }

    private fun buildToolsFeatureList(
        context: FragmentActivity,
        includeAllTools: Boolean,
    ): List<ToolsFeature> {
        // Tools tab removed — keep empty so nothing can re-surface dropped tools.
        return emptyList()
    }

    private fun buildToolsHomeList(
        context: FragmentActivity,
        includeAllTools: Boolean,
    ): List<ToolsFeature> {
        return emptyList()
    }
}
