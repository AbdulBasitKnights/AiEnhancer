package com.aiface.aging.features.cropper

import android.app.Application
import androidx.lifecycle.ViewModel
import com.aiface.aging.R
import com.aiface.aging.di.IoDispatcher
import com.aiface.aging.shared.editorui.ModelRatio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class ViewModelCropper @Inject constructor(
    private val application: Application,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    fun loadRatioIcons() = flow {
        emit(
            listOf(
                ModelRatio(
                    1,
                    R.drawable.rat_custom,
                    "Custom",
                    null,
                    background = R.drawable.rat_custom
                ),
                ModelRatio(
                    2,
                    R.drawable.rat_insta_1_1,
                    "1:1",
                    "1:1",
                    background = R.drawable.rat_bg_1_1
                ),
                ModelRatio(
                    3,
                    R.drawable.rat_insta_9_16,
                    "9:16",
                    "9:16",
                    background = R.drawable.rat_bg_9_16
                ),
                ModelRatio(
                    4,
                    R.drawable.rat_fb_4_5,
                    "4:5",
                    "4:5",
                    background = R.drawable.rat_bg_4_5
                ),
                ModelRatio(
                    5,
                    R.drawable.rat_fb_5_4,
                    "5:4",
                    "5:4",
                    background = R.drawable.rat_bg_5_4
                ),
                ModelRatio(
                    6,
                    R.drawable.rat_fb_16_9,
                    "16:9",
                    "16:9",
                    background = R.drawable.rat_bg_16_9
                ),
                ModelRatio(
                    7,
                    R.drawable.rat_x_4_3,
                    "4:3",
                    "4:3",
                    background = R.drawable.rat_bg_4_3
                ),
                ModelRatio(
                    8,
                    R.drawable.rat_x_16_9,
                    "16:9",
                    "16:9",
                    background = R.drawable.rat_bg_16_9
                ),
                ModelRatio(
                    9,
                    R.drawable.rat_x_3_1,
                    "3:1",
                    "3:1",
                    background = R.drawable.rat_bg_3_1
                ),
                ModelRatio(
                    10,
                    R.drawable.rat_whatsapp_3_4,
                    "3:4",
                    "3:4",
                    background = R.drawable.rat_bg_3_4
                ),
                ModelRatio(
                    11,
                    R.drawable.rat_pin_3_2,
                    "3:2",
                    "3:2",
                    background = R.drawable.rat_bg_3_2
                ),
                ModelRatio(
                    12,
                    R.drawable.rat_pin_2_3,
                    "2:3",
                    "2:3",
                    background = R.drawable.rat_bg_2_3
                ),
                ModelRatio(
                    13,
                    R.drawable.rat_linked_16_9,
                    "16:9",
                    "16:9",
                    background = R.drawable.rat_bg_16_9
                ),
                ModelRatio(
                    14,
                    R.drawable.rat_linked_9_16,
                    "9:16",
                    "9:16",
                    background = R.drawable.rat_bg_9_16
                ),
                ModelRatio(
                    15,
                    R.drawable.rat_youtube_3_1,
                    "3:1",
                    "3:1",
                    background = R.drawable.rat_bg_3_1
                ),
            )
        )
    }.flowOn(ioDispatcher)
}