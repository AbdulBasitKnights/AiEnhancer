package com.aiface.aging.features.blender

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiface.aging.features.bgremover.SubjectSegmentationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PhotoBlenderViewModel @Inject constructor(
    private val bgMaskRepository: BgMaskRepository,
    private val blenderRepository: BlenderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PhotoBlenderUiState>(PhotoBlenderUiState.NeedBase)
    val uiState: StateFlow<PhotoBlenderUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PhotoBlenderEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PhotoBlenderEvent> = _events.asSharedFlow()

    private var baseUri: Uri? = null
    private var characterCutout: Bitmap? = null

    fun onBaseSelected(uri: Uri) {
        baseUri = uri
        _uiState.value = PhotoBlenderUiState.NeedCharacter
        _events.tryEmit(PhotoBlenderEvent.ShowBase(uri))
    }

    fun onCharacterBitmapReady(source: Bitmap) {
        viewModelScope.launch {
            _uiState.value = PhotoBlenderUiState.Masking
            try {
                val cutout = withContext(Dispatchers.Default) {
                    bgMaskRepository.createCharacterMask(source)
                }
                characterCutout = cutout
                val base = baseUri
                if (base == null) {
                    _uiState.value = PhotoBlenderUiState.NeedBase
                    _events.tryEmit(PhotoBlenderEvent.Toast("Pick background photo first"))
                    return@launch
                }
                _uiState.value = PhotoBlenderUiState.Ready(base, cutout)
                _events.tryEmit(PhotoBlenderEvent.ShowCharacterCutout(cutout))
            } catch (e: Throwable) {
                val message = SubjectSegmentationHelper.displayMessage(
                    e,
                    "Could not remove background",
                )
                _uiState.value = PhotoBlenderUiState.Error(message)
                _events.tryEmit(PhotoBlenderEvent.Toast(message))
                // Allow retry character pick if base already set.
                if (baseUri != null) {
                    _uiState.value = PhotoBlenderUiState.NeedCharacter
                }
            }
        }
    }

    fun saveBlend(canvasBitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = PhotoBlenderUiState.Saving
            val path = withContext(Dispatchers.IO) {
                blenderRepository.saveBlend(canvasBitmap)
            }
            if (path.isNullOrBlank()) {
                _uiState.value = PhotoBlenderUiState.Error("Save failed")
                _events.tryEmit(PhotoBlenderEvent.Toast("Save failed"))
                restoreReadyState()
            } else {
                _uiState.value = PhotoBlenderUiState.Saved(path)
                _events.tryEmit(PhotoBlenderEvent.OpenResult(path))
            }
        }
    }

    fun resetToPickBase() {
        baseUri = null
        characterCutout = null
        _uiState.value = PhotoBlenderUiState.NeedBase
    }

    private fun restoreReadyState() {
        val base = baseUri
        val cutout = characterCutout
        if (base != null && cutout != null) {
            _uiState.value = PhotoBlenderUiState.Ready(base, cutout)
        } else if (base != null) {
            _uiState.value = PhotoBlenderUiState.NeedCharacter
        } else {
            _uiState.value = PhotoBlenderUiState.NeedBase
        }
    }
}

sealed class PhotoBlenderEvent {
    data class ShowBase(val uri: Uri) : PhotoBlenderEvent()
    data class ShowCharacterCutout(val bitmap: Bitmap) : PhotoBlenderEvent()
    data class OpenResult(val path: String) : PhotoBlenderEvent()
    data class Toast(val message: String) : PhotoBlenderEvent()
}
