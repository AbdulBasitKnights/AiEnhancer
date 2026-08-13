package com.aiface.aging.features.result



import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aiface.aging.R



enum class ResultTryMoreFeature(
    @StringRes val titleRes: Int,
    @DrawableRes val imageRes: Int,
) {
    ENHANCER(R.string.photo_enhancer, R.drawable.enhancer_share),
}
