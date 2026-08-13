package com.aiface.aging.features.collage.static_

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import com.aiface.aging.R
import com.aiface.aging.di.IoDispatcher
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.shared.editorui.ModelRatio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class ViewModelCollageEditor @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val application: Application
) :
    ViewModel() {

    var mSpace: Float

    val MAX_SPACE: Float by lazy {
        pxFromDp(30f)
    }
    val MAX_CORNER: Float by lazy {
        pxFromDp(60f)
    }
    val DEFAULT_SPACE: Float by lazy {
        pxFromDp(2f)
    }

    init {
        mSpace = DEFAULT_SPACE
    }

    fun pxFromDp(dp: Float): Float {
        return dp * application.applicationContext.resources.displayMetrics.density
    }

    fun loadBottomIcons(context: Context) = flow {
        emit(
            listOf(
                ModelDrawableAssets(1,R.drawable.ic_collage__new, context.resources.getString(R.string.collage)),
              //  ModelDrawableAssets(4, R.drawable.ic_sticker_new, context.resources.getString(R.string.sticker)),
                ModelDrawableAssets(5, R.drawable.ic_text_new, context.resources.getString(R.string.text)),
                ModelDrawableAssets(2, R.drawable.ic_backgrounds_new, context.resources.getString(R.string.background)),
                ModelDrawableAssets(3, R.drawable.ic_border_new, context.resources.getString(R.string.border)),
                ModelDrawableAssets(6, R.drawable.ic_ratio, context.resources.getString(R.string.ratio)),
               // ModelDrawableAssets(7, R.drawable.ic_replace_photo, context.resources.getString(R.string.replace)),
            )
        )
    }.flowOn(ioDispatcher)

    fun loadBGsList() = flow {
        emit(
            listOf(
                ModelDrawableAssets(21, R.drawable.transparentt),
                ModelDrawableAssets(1, R.drawable.bg_1),
                ModelDrawableAssets(2, R.drawable.bg_2),
                ModelDrawableAssets(3, R.drawable.bg_3),
                ModelDrawableAssets(4, R.drawable.bg_4),
                ModelDrawableAssets(5, R.drawable.bg_5),
                ModelDrawableAssets(6, R.drawable.bg_6),
                ModelDrawableAssets(7, R.drawable.bg_7),
                ModelDrawableAssets(8, R.drawable.bg_8),
                ModelDrawableAssets(9, R.drawable.bg_9),
                ModelDrawableAssets(10, R.drawable.bg_10),
                ModelDrawableAssets(11, R.drawable.bg_11),
                ModelDrawableAssets(12, R.drawable.bg_12),
                ModelDrawableAssets(13, R.drawable.bg_13),
                ModelDrawableAssets(14, R.drawable.bg_14),
                ModelDrawableAssets(15, R.drawable.bg_15),
                ModelDrawableAssets(16, R.drawable.bg_16),
                ModelDrawableAssets(17, R.drawable.bg_17),
                ModelDrawableAssets(18, R.drawable.bg_18),
                ModelDrawableAssets(19, R.drawable.bg_19),
                ModelDrawableAssets(20, R.drawable.bg_20),
            )
        )
    }.flowOn(ioDispatcher)


    fun loadRatioIcons() = flow {
        emit(
            listOf(
                ModelRatio(
                    1,
                    R.drawable.rat_insta_1_1,
                    "1:1",
                    "1:1",
                    background = R.drawable.rat_bg_1_1
                ),
                ModelRatio(
                    2,
                    R.drawable.rat_insta_9_16,
                    "9:16",
                    "9:16",
                    background = R.drawable.rat_bg_9_16
                ),
                ModelRatio(
                    5,
                    R.drawable.rat_fb_4_5,
                    "4:5",
                    "4:5",
                    background = R.drawable.rat_bg_4_5
                ),
                ModelRatio(
                    6,
                    R.drawable.rat_fb_5_4,
                    "5:4",
                    "5:4",
                    background = R.drawable.rat_bg_5_4
                ),
                ModelRatio(
                    7,
                    R.drawable.rat_fb_16_9,
                    "16:9",
                    "16:9",
                    background = R.drawable.rat_bg_16_9
                ),
                ModelRatio(
                    9,
                    R.drawable.rat_x_4_3,
                    "4:3",
                    "4:3",
                    background = R.drawable.rat_bg_4_3
                ),
                ModelRatio(
                    10,
                    R.drawable.rat_x_16_9,
                    "16:9",
                    "16:9",
                    background = R.drawable.rat_bg_16_9
                ),
                ModelRatio(
                    11,
                    R.drawable.rat_x_3_1,
                    "3:1",
                    "3:1",
                    background = R.drawable.rat_bg_3_1
                ),
                ModelRatio(
                    12,
                    R.drawable.rat_whatsapp_3_4,
                    "3:4",
                    "3:4",
                    background = R.drawable.rat_bg_3_4
                ),
                ModelRatio(
                    13,
                    R.drawable.rat_pin_3_2,
                    "3:2",
                    "3:2",
                    background = R.drawable.rat_bg_3_2
                ),
                ModelRatio(
                    14,
                    R.drawable.rat_pin_2_3,
                    "2:3",
                    "2:3",
                    background = R.drawable.rat_bg_2_3
                ),
                ModelRatio(
                    15,
                    R.drawable.rat_linked_16_9,
                    "16:9",
                    "16:9",
                    background = R.drawable.rat_bg_16_9
                ),
                ModelRatio(
                    15,
                    R.drawable.rat_linked_9_16,
                    "9:16",
                    "9:16",
                    background = R.drawable.rat_bg_9_16
                ),
                ModelRatio(
                    16,
                    R.drawable.rat_youtube_3_1,
                    "3:1",
                    "3:1",
                    background = R.drawable.rat_bg_3_1
                ),
            )
        )
    }.flowOn(ioDispatcher)
}