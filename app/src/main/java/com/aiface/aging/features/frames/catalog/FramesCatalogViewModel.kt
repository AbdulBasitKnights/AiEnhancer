package com.aiface.aging.features.frames.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiface.aging.features.blender.catalog.BlenderCatalogUiState
import com.aiface.aging.features.blender.catalog.BlenderCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FramesCatalogViewModel @Inject constructor(
    private val repository: FramesCatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BlenderCatalogUiState>(BlenderCatalogUiState.Loading)
    val uiState: StateFlow<BlenderCatalogUiState> = _uiState.asStateFlow()

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = BlenderCatalogUiState.Loading
            repository.getCategories(forceRefresh)
                .onSuccess { list ->
                    _uiState.value = if (list.isEmpty()) {
                        BlenderCatalogUiState.Error("No frames")
                    } else {
                        BlenderCatalogUiState.Ready(list)
                    }
                }
                .onFailure {
                    _uiState.value = BlenderCatalogUiState.Error("Load failed")
                }
        }
    }
}
