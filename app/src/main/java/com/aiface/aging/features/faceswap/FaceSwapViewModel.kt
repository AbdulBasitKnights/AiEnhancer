package com.aiface.aging.features.faceswap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aiface.aging.shared.BaseViewModel
import com.aiface.aging.data.model.faceswap.FaceSwapCategoryDto
import com.aiface.aging.data.model.faceswap.FaceSwapTemplateDto
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.FaceSwapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FaceSwapCatalogUiState {
    data object Idle : FaceSwapCatalogUiState()
    data object Loading : FaceSwapCatalogUiState()
    data class Success(
        val categories: List<FaceSwapCategoryDto>,
        val templates: List<FaceSwapTemplateDto>,
    ) : FaceSwapCatalogUiState()
    data class Error(val message: String) : FaceSwapCatalogUiState()
    data object Empty : FaceSwapCatalogUiState()
}

@HiltViewModel
class FaceSwapViewModel @Inject constructor(
    private val repository: FaceSwapRepository,
) : BaseViewModel() {

    private val _uiState = MutableLiveData<FaceSwapCatalogUiState>(FaceSwapCatalogUiState.Idle)
    val uiState: LiveData<FaceSwapCatalogUiState> = _uiState

    private var catalogJob: Job? = null

    fun loadCatalog(force: Boolean = false) {
        if (catalogJob?.isActive == true && !force) return
        if (_uiState.value is FaceSwapCatalogUiState.Success && !force) return

        if (!force) {
            repository.peekCachedCatalog()?.takeIf {
                it.categories.isNotEmpty() && it.templates.isNotEmpty()
            }?.let { cached ->
                _uiState.value = FaceSwapCatalogUiState.Success(cached.categories, cached.templates)
                return
            }
        }

        catalogJob?.cancel()
        // Keep showing Success while a forced refresh runs; otherwise show shimmer.
        if (_uiState.value !is FaceSwapCatalogUiState.Success) {
            _uiState.value = FaceSwapCatalogUiState.Loading
        }
        catalogJob = viewModelScope.launch {
            try {
                when (val result = repository.getCatalog(forceRefresh = force).first { it !is Resource.Loading }) {
                    is Resource.Success -> {
                        val catalog = result.data
                        if (catalog == null ||
                            catalog.categories.isEmpty() ||
                            catalog.templates.isEmpty()
                        ) {
                            _uiState.value = FaceSwapCatalogUiState.Empty
                        } else {
                            _uiState.value = FaceSwapCatalogUiState.Success(
                                catalog.categories,
                                catalog.templates,
                            )
                        }
                    }
                    is Resource.Error -> {
                        // Prefer cached data over error if Splash preload already succeeded.
                        repository.peekCachedCatalog()?.takeIf {
                            it.categories.isNotEmpty() && it.templates.isNotEmpty()
                        }?.let { cached ->
                            _uiState.value =
                                FaceSwapCatalogUiState.Success(cached.categories, cached.templates)
                            return@launch
                        }
                        _uiState.value = FaceSwapCatalogUiState.Error(
                            result.message ?: "Something went wrong",
                        )
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = FaceSwapCatalogUiState.Error(
                    e.message ?: "Something went wrong",
                )
            }
        }
    }

    fun templatesForCategory(
        categoryId: String?,
        allTemplates: List<FaceSwapTemplateDto>,
    ): List<FaceSwapTemplateDto> {
        if (categoryId.isNullOrBlank()) return emptyList()
        return allTemplates.filter { it.categoryId == categoryId }
    }
}
