package com.aiface.aging.features.faceswap

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aiface.aging.shared.BaseViewModel
import com.aiface.aging.shared.Constants
import com.aiface.aging.shared.CreditManager
import com.aiface.aging.data.model.faceswap.FaceSwapStatusData
import com.aiface.aging.data.model.faceswap.isCompleted
import com.aiface.aging.data.model.faceswap.isInProgress
import com.aiface.aging.data.model.faceswap.isTerminalFailed
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.FaceSwapRepository
import com.aiface.aging.utils.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FaceSwapGenerateUiState {
    data object Idle : FaceSwapGenerateUiState()
    data object Loading : FaceSwapGenerateUiState()
    data class Success(val outputImageUrl: String, val statusData: FaceSwapStatusData? = null) :
        FaceSwapGenerateUiState()
    data class Error(val message: String) : FaceSwapGenerateUiState()
}

@HiltViewModel
class FaceSwapGenerateViewModel @Inject constructor(
    private val repository: FaceSwapRepository,
    private val imageCompressor: ImageCompressor,
    private val creditManager: CreditManager,
) : BaseViewModel() {

    private val _generateState =
        MutableLiveData<FaceSwapGenerateUiState>(FaceSwapGenerateUiState.Idle)
    val generateState: LiveData<FaceSwapGenerateUiState> = _generateState

    val credits = creditManager.creditsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var generateJob: Job? = null
    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            creditManager.ensureFreeCredits()
        }
    }

    fun startSwap(templateId: String, imageUris: List<Uri>) {
        if (_generateState.value is FaceSwapGenerateUiState.Loading) return
        if (templateId.isBlank()) {
            _generateState.value = FaceSwapGenerateUiState.Error("Invalid template")
            return
        }
        val sourceUri = imageUris.firstOrNull()
        if (sourceUri == null) {
            _generateState.value = FaceSwapGenerateUiState.Error("Please select an image")
            return
        }

        cancelPolling()
        generateJob?.cancel()
        _generateState.value = FaceSwapGenerateUiState.Loading

        generateJob = viewModelScope.launch {
            try {
                val part = imageCompressor.toMultipartPart(sourceUri, "source_image")
                when (val result = repository.swapFace(part, templateId).first { it !is Resource.Loading }) {
                    is Resource.Success -> {
                        val data = result.data!!
                        val status = data.status
                        val generationId = data.generationId.orEmpty()

                        when {
                            status.isCompleted() && !data.imageUrl.isNullOrBlank() -> {
                                consumeImageCredit()
                                _generateState.value = FaceSwapGenerateUiState.Success(data.imageUrl)
                            }
                            status.isCompleted() -> {
                                if (generationId.isBlank()) {
                                    _generateState.value =
                                        FaceSwapGenerateUiState.Error("Missing generation id")
                                } else {
                                    startPolling(generationId)
                                }
                            }
                            status.isTerminalFailed() -> {
                                _generateState.value = FaceSwapGenerateUiState.Error(
                                    "Face swap failed. Please try again."
                                )
                            }
                            status.isInProgress() || generationId.isNotBlank() -> {
                                if (generationId.isBlank()) {
                                    _generateState.value =
                                        FaceSwapGenerateUiState.Error("Missing generation id")
                                } else {
                                    startPolling(generationId)
                                }
                            }
                            else -> {
                                if (generationId.isNotBlank()) {
                                    startPolling(generationId)
                                } else {
                                    _generateState.value = FaceSwapGenerateUiState.Error(
                                        "Unexpected generation status: ${status ?: "unknown"}"
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        _generateState.value = FaceSwapGenerateUiState.Error(
                            result.message ?: "Face swap failed"
                        )
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "startSwap failed", e)
                _generateState.value = FaceSwapGenerateUiState.Error(
                    e.message ?: "Image upload failed"
                )
            }
        }
    }

    private fun startPolling(generationId: String) {
        cancelPolling()
        pollJob = viewModelScope.launch {
            repeat(Constants.FACE_SWAP_POLL_MAX_ATTEMPTS) { attempt ->
                if (!isActive) return@launch
                if (attempt > 0) delay(Constants.FACE_SWAP_POLL_DELAY_MS)

                when (val result = repository.getSwapStatus(generationId).first()) {
                    is Resource.Success -> {
                        val data = result.data!!
                        val status = data.status
                        when {
                            status.isCompleted() -> {
                                val output = data.outputImageUrl
                                if (output.isNullOrBlank()) {
                                    _generateState.value = FaceSwapGenerateUiState.Error(
                                        "Generation completed but image is missing"
                                    )
                                } else {
                                    consumeImageCredit()
                                    _generateState.value =
                                        FaceSwapGenerateUiState.Success(output, data)
                                }
                                return@launch
                            }
                            status.isTerminalFailed() -> {
                                _generateState.value = FaceSwapGenerateUiState.Error(
                                    data.errorMessage?.takeIf { it.isNotBlank() }
                                        ?: "Face swap failed. Please try again."
                                )
                                return@launch
                            }
                            status.isInProgress() || status.isNullOrBlank() -> Unit
                            else -> Unit
                        }
                    }
                    is Resource.Error -> {
                        // Transient errors: keep polling unless near timeout
                        if (attempt >= Constants.FACE_SWAP_POLL_MAX_ATTEMPTS - 1) {
                            _generateState.value = FaceSwapGenerateUiState.Error(
                                result.message ?: "Failed to check generation status"
                            )
                            return@launch
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }
            _generateState.value = FaceSwapGenerateUiState.Error(
                "Generation is taking too long. Please try again."
            )
        }
    }

    fun resetState() {
        cancelPolling()
        generateJob?.cancel()
        generateJob = null
        _generateState.value = FaceSwapGenerateUiState.Idle
    }

    private fun consumeImageCredit() = viewModelScope.launch {
        creditManager.consumeImageCredit()
    }

    private fun cancelPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        cancelPolling()
        generateJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val TAG = "FaceSwapGenerateVM"
    }
}
