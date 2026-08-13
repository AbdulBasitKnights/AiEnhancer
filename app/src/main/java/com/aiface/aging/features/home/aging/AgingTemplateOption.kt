package com.aiface.aging.features.home.aging

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class AgingTemplateOption(
    val templateId: String,
    @StringRes val titleRes: Int,
    @DrawableRes val thumbnailRes: Int,
    val prompt: String,
    val thumbnailUrl: String? = null,
    val displayTitle: String? = null,
)
