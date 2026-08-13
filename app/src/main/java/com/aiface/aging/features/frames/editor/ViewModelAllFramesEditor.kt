package com.aiface.aging.features.frames.editor

import android.app.Application
import android.content.Context
import com.aiface.aging.R
import androidx.lifecycle.ViewModel
import com.aiface.aging.di.IoDispatcher
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class ViewModelAllFramesEditor @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val application: Application
) : ViewModel() {

    fun loadBottomIcons(context: Context) = flow {
        emit(
            listOf(
                ModelDrawableAssets(
                    1, R.drawable.frameicon,
                    context.getString(R.string.templates)
                ),
                ModelDrawableAssets(3, R.drawable.ic_text_new, context.getString(R.string.text)),
                ModelDrawableAssets(4, R.drawable.flip, context.getString(R.string.flip)),
            )
        )
    }.flowOn(ioDispatcher)

}