package com.aiface.aging.domain.model

import androidx.annotation.Keep
import com.aiface.aging.data.model.NewGenerateResponse
@Keep
sealed class GenerateUiState {
    data object Idle : GenerateUiState()
    data object Loading : GenerateUiState()
    data class Success(val response: NewGenerateResponse) : GenerateUiState()
    data class Error(val message: String) : GenerateUiState()
}

