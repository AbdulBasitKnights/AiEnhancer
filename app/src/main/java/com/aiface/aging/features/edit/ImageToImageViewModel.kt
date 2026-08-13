package com.aiface.aging.features.edit

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aiface.aging.shared.BaseViewModel
import com.aiface.aging.shared.CreditManager
import com.aiface.aging.domain.model.GenerateUiState
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.usecase.GenerateImageUseCase
import com.aiface.aging.domain.usecase.GenerateImageWithImageUseCase
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import kotlin.text.contains

@HiltViewModel
class ImageToImageViewModel @Inject constructor(
    private val generateImageUseCase: GenerateImageUseCase,
    private val imageCompressor: ImageCompressor,
    private val creditManager: CreditManager,
) : BaseViewModel() {

    private val _generateState = MutableLiveData<GenerateUiState>(GenerateUiState.Idle)
    val generateState: LiveData<GenerateUiState> = _generateState

    val credits = creditManager.creditsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            creditManager.ensureFreeCredits()
        }
    }

    /**
     * Unified generation entry point for both flows:
     *
     *  - Template flow  → pass [templateUuid] + [prompt] + [imageOneUri] (user's photo)
     *  - Custom flow    → pass [prompt] + [imageOneUri] (+ optional [imageTwoUri])
     *
     * Images are compressed on IO thread before upload.
     * Guard: duplicate calls while Loading are silently ignored.
     */
    fun generateImage(
        templateUuid: String? = null,
        prompt: String,
        imageOneUri: Uri? = null,
        imageTwoUri: Uri? = null,
    ) {

        if (_generateState.value == GenerateUiState.Loading) return

        _generateState.value = GenerateUiState.Loading
        viewModelScope.launch {
            try {
                Log.d("GenerateRequest", "template_uuid=$templateUuid promptLength=${prompt.length} imageOne=$imageOneUri")
                FirebaseLogUtils.logEvent("generation_request", "")
                val parts = buildMultipartParts(
                    templateUuid = templateUuid,
                    prompt = prompt,
                    imageOneUri = imageOneUri,
                    imageTwoUri = imageTwoUri,
                )

                generateImageUseCase(parts).onEach { result ->
                    when (result) {
                        is Resource.Success -> {
                            val body = result.data
                            val outputUrl = body?.data?.outputUrl
                            val jobId = body?.data?.jobId
                            if (outputUrl.isNullOrBlank() && jobId.isNullOrBlank()) {
                                handleLoading(false)
                                _generateState.value = GenerateUiState.Error(
                                    body?.message ?: "Generation returned no output",
                                )
                                return@onEach
                            }
                            consumeImage()
                            handleLoading(false)
                            _generateState.value = GenerateUiState.Success(body!!)
                        }
                        is Resource.Error -> {
                            handleLoading(false)
                            _generateState.value = GenerateUiState.Error(
                                result.message ?: "Something went wrong"
                            )
                        }
                        is Resource.Loading -> handleLoading(true)
                    }
                }.launchIn(this)

            } catch (e: Exception) {
                handleLoading(false)
                _generateState.value = GenerateUiState.Error(
                    e.message ?: "Image processing failed"
                )
            }
        }
    }

    fun resetState() {
        _generateState.value = GenerateUiState.Idle
    }

    private fun consumeImage() = viewModelScope.launch {
        val success = creditManager.consumeImageCredit()
        if (!success) {
            Log.d("Credits", "Not enough credits to deduct")
        } else {
            Log.d("Credits", "consumed — remaining=${creditManager.getCredits()}")
        }
    }

    // ── Multipart builder ─────────────────────────────────────────────────────

    private suspend fun buildMultipartParts(
        templateUuid: String?,
        prompt: String,
        imageOneUri: Uri?,
        imageTwoUri: Uri?,
    ): List<MultipartBody.Part> = buildList {
        // template_uuid — only when doing template-based generation
        templateUuid?.takeIf { it.isNotBlank() }?.let { uuid ->
            add(MultipartBody.Part.createFormData("template_uuid", uuid))
        }

        // prompt — always required
        add(MultipartBody.Part.createFormData("prompt", prompt))

        // input_img_file_one — primary user photo
        imageOneUri?.let { uri ->
            add(imageCompressor.toMultipartPart(uri, "input_img_file_one"))
        }

        // input_img_file_two — optional second photo; excluded entirely when null
        imageTwoUri?.let { uri ->
            add(imageCompressor.toMultipartPart(uri, "input_img_file_two"))
        }
    }
}


