package com.aiface.aging.features.rotate

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
class ViewModelRotate @Inject constructor(
    private val application: Application,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    fun loadRotateIcons(context: Context) = flow {
        emit(
            listOf(
                ModelDrawableAssets(
                    1, R.drawable.ic_rotate_left_new,
                    context.resources.getString(R.string.left)
                ),
                ModelDrawableAssets(2, R.drawable.ic_rotate_right_new,
                    context.resources.getString(R.string.right)),
                ModelDrawableAssets(3, R.drawable.ic_flip_horizontal_new,
                    context.resources.getString(R.string.horizontal)),
                ModelDrawableAssets(3, R.drawable.ic_flip_vertical_new,
                    context.resources.getString(R.string.vertical)),
            )
        )
    }.flowOn(ioDispatcher)
}