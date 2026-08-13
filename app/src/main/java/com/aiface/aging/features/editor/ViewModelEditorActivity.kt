package com.aiface.aging.features.editor

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
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
class ViewModelEditorActivity @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val application: Application
) : ViewModel() {

    var userImageBitmap: Bitmap? = null

    fun loadBottomIcons(context: Context) = flow {
        emit(
            listOf(
                ModelDrawableAssets(1, R.drawable.ic_colors_new, context.getString(R.string.filters)),
             //   ModelDrawableAssets(2,R.drawable.ic_eraser__, context.getString(R.string.ai_magic)),
                ModelDrawableAssets(3, R.drawable.ic_adjustment_new, context.getString(R.string.adjust)),
              //  ModelDrawableAssets(4, R.drawable.ic_sticker_new,context.getString(R.string.sticker)),
                ModelDrawableAssets(5, R.drawable.ic_text_new, context.getString(R.string.text)),
                ModelDrawableAssets(6, R.drawable.ic_crop_new, context.getString(R.string.crop)),
                ModelDrawableAssets(7, R.drawable.ic_rotate_new, context.getString(R.string.rotate))
            )
        )
    }.flowOn(ioDispatcher)

    fun getIcon(context: Context) = flow {
        emit(
            listOf(
                ModelDrawableAssets(4, R.drawable.hair_style_shape_,context.getString(R.string.hair_style))
            )
        )
    }.flowOn(ioDispatcher)

}