package com.aiface.aging.ads_nextgen

sealed class AdUiState {
    data object Idle : AdUiState()
    data class Loading(val format: AdFormat, val mode: AdLoadMode) : AdUiState()
    data class Ready(val format: AdFormat, val mode: AdLoadMode, val cachedCount: Int = 1) : AdUiState()
    data class Error(val format: AdFormat, val mode: AdLoadMode, val message: String) : AdUiState()
    data class Showing(val format: AdFormat) : AdUiState()
}

data class PreloadBufferStatus(
    val format: AdFormat,
    val availableCount: Int,
    val isPreloading: Boolean
)
