package com.aiface.aging.features.text.text

import android.app.Application
import android.content.Context
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
class ViewModelTextFragment @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val application: Application
) : ViewModel() {
    fun loadTextIcons(context: Context) = flow {
        emit(
            listOf(
                ModelDrawableAssets(
                    1,
                    R.drawable.ic_edit_text_new,
                    context.getString(R.string.edit)
                ),
                ModelDrawableAssets(2, R.drawable.ic_font_new, context.getString(R.string.styles)),
                ModelDrawableAssets(3, R.drawable.ic_text_color_new,
                    context.getString(R.string.color)),
                ModelDrawableAssets(4, R.drawable.ic_text_bg_new,
                    context.getString(R.string.text_bg)),
                ModelDrawableAssets(5, R.drawable.ic_text_resize_new,
                    context.getString(R.string.resize)),
                ModelDrawableAssets(6, R.drawable.ic_text_align_left,
                    context.getString(R.string.l_align)),
                ModelDrawableAssets(7, R.drawable.ic_text_align_centre,
                    context.getString(R.string.c_align)),
                ModelDrawableAssets(8, R.drawable.ic_text_align_right,
                    context.getString(R.string.r_align)),
            )
        )
    }.flowOn(ioDispatcher)
}