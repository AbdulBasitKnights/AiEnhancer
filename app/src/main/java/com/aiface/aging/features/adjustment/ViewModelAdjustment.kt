package com.aiface.aging.features.adjustment

import android.app.Application
import androidx.lifecycle.ViewModel
import com.aiface.aging.R

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.aiface.aging.di.IoDispatcher
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import javax.inject.Inject

@HiltViewModel
class ViewModelAdjustment @Inject constructor(
    val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {



    //Filters Progress
    val minBright = -3.0f
    val maxBright = 3f
    var currentBright = 0.0f
    val sectionsBright = 2

    val minHue = 1.0f
    val maxHue = 100f
    var currentHue = 1.0f
    val sectionsHue = 1

    val minContrast = 0.5f
    val maxContrast = 1.5f
    var currentContrast = 1.0f
    val sectionsContrast = 2

    val minHighlightShadow = 0.0f
    val maxHighlightShadow = 1.0f
    var currentHighlightShadow = 0.0f
    val sectionsHighlightShadow = 1

    val minSaturation = 0.0f
    val maxSaturation = 2.0f
    var currentSaturation = 1.0f
    val sectionsSaturation = 2

    val minSharpness = -2.0f
    val maxSharpness = 2.0f
    var currentSharpness = 0.0f
    val sectionsSharpness = 2

    val minExposure = -2.0f
    val maxExposure = 2.0f
    var currentExposure = 0.0f
    val sectionsExposure = 2

    fun loadAdjustmentIcons(highlight : String?,exposure:String?,hue:String?,contrast:String?,saturation:String?,sharpness:String?,brightness:String?) = flow {
        emit(
            listOf(
                ModelDrawableAssets(1, R.drawable.ic_brightness_new, brightness),
                ModelDrawableAssets(2, R.drawable.ic_hue_new, hue),
                ModelDrawableAssets(3, R.drawable.ic_contrast_new, contrast),
                ModelDrawableAssets(4, R.drawable.ic_highlights_new, highlight),
                ModelDrawableAssets(5, R.drawable.ic_saturation_new, saturation),
                ModelDrawableAssets(6, R.drawable.ic_sharpness_new, sharpness),
                ModelDrawableAssets(7, R.drawable.ic_exposure_new, exposure),
            )
        )
    }.flowOn(ioDispatcher)
}