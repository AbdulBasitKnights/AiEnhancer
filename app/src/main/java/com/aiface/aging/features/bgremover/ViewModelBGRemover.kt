package com.aiface.aging.features.bgremover

import android.app.Application
import androidx.lifecycle.ViewModel
import com.aiface.aging.R
import com.aiface.aging.di.IoDispatcher
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class ViewModelBGRemover @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val application: Application,
) : ViewModel() {






    fun loadBottomIcons() = flow {
        emit(
            listOf(
                ModelDrawableAssets(3, R.drawable.ic_text_new, "Write"),
                ModelDrawableAssets(2, R.drawable.ic_bnv_bg_remover, "AI Eraser"),
                ModelDrawableAssets(5, R.drawable.ic_adjustment_new, "Adjust"),
                ModelDrawableAssets(6, R.drawable.ic_colors_new, "Filters"),
                ModelDrawableAssets(7, R.drawable.ic_mirror_new, "Flip"),
            ),
        )
    }.flowOn(ioDispatcher)



}