package com.aiface.aging.features.blender.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BlenderCatalogUiState {
    data object Loading : BlenderCatalogUiState()
    data class Ready(val categories: List<BlenderCategory>) : BlenderCatalogUiState()
    data class Error(val message: String) : BlenderCatalogUiState()
}

@HiltViewModel
class BlenderCatalogViewModel @Inject constructor(
    private val repository: BlendingCatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BlenderCatalogUiState>(BlenderCatalogUiState.Loading)
    val uiState: StateFlow<BlenderCatalogUiState> = _uiState.asStateFlow()

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = BlenderCatalogUiState.Loading
            repository.getCategories(forceRefresh)
                .onSuccess { list ->
                    _uiState.value = if (list.isEmpty()) {
                        BlenderCatalogUiState.Error("No templates")
                    } else {
                        BlenderCatalogUiState.Ready(list)
                    }
                }
                .onFailure {
                    _uiState.value = BlenderCatalogUiState.Error(it.message ?: "Load failed")
                }
        }
    }
}
