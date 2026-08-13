package com.aiface.aging.features.blender

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiface.aging.features.blender.model.BlendEditorOptionsModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlenderEditorViewModel @Inject constructor(
    private val blenderEditorRepo: BlenderEditorRepo
) : ViewModel() {

    private val blendEditorOptionsList = MutableLiveData<ArrayList<BlendEditorOptionsModel>>()
    private val blendShapeStylesList = MutableLiveData<ArrayList<String>>()

    fun observeBlendEditorOptions(): LiveData<ArrayList<BlendEditorOptionsModel>> = blendEditorOptionsList
    fun observeBlendsShapeStyles(): LiveData<ArrayList<String>> = blendShapeStylesList

    fun getAllBlendEditorOptions() {
        viewModelScope.launch {
            blendEditorOptionsList.postValue(blenderEditorRepo.getAllBlendEditorOptions())
        }
    }

    fun getAllBlendShapeStyles(context: Context) {
        viewModelScope.launch {
            blendShapeStylesList.postValue(blenderEditorRepo.getAllBlendShapeStyles(context))
        }
    }
}
