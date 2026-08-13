package com.aiface.aging.domain.model

import androidx.annotation.Keep

@Keep
data class Category(
    val id: String,
    val appName: String,
    val name: String,
    val description: String?,
    val position: Int,
    val isActive: Boolean,
    val templateCount: Int,
    val templates: List<Template>,
)
@Keep
data class Template(
    val id: String,
    val categoryId: String,
    val title: String?,
    val generationType: String,
    val isActive: Boolean,
    val priority: Int,
    val isPro: Boolean,
    val imageCount: Int?,
    val vendorTemplateId: String?,
    val mediaUrl: String?,
    val gifUrl: String?,
    val thumbnailUrl: String?,
    val prompt: String?,
    val negativePrompt: String?,
)

