package com.aiface.aging.features.home

import com.aiface.aging.domain.model.Category
import com.aiface.aging.domain.model.Template

/**
 * Resolves template UUIDs for standalone home shortcuts (Enhancer, Aging).
 * Hard-coded legacy IDs are no longer valid on the backend catalog.
 */
object HomeAiTemplateResolver {

    /** Verified against hd_camera catalog — Women Portrait's > Soft-lit */
    const val ENHANCER_FALLBACK_TEMPLATE_ID = "195dc8db-668f-48ca-8e51-913949a9eb00"

    /** Verified against beauty_camera catalog — Aging > first age template */
    const val AGING_FALLBACK_TEMPLATE_ID = "ee3f1409-508c-4496-ae21-48bf1740daab"

    const val DEFAULT_ENHANCER_PROMPT =
        "high-resolution upscale of the image, remove all noise and grain, smooth skin naturally without over-blurring, preserve realistic textures, improve lighting and contrast, and refine facial features for a clean, natural look. Maintain the original color tones and proportions. Enhance natural beauty subtly with an even skin tone, healthy soft glow, and fresh polished appearance. Improve lighting further with balanced exposure, gentle highlights, soft shadows, and natural depth while keeping the original mood and colors unchanged. Add gentle facial beauty enhancement: brighten the face slightly, refine skin clarity, smooth minor imperfections, enhance facial freshness and radiance while keeping the face realistic, natural, and true to the original person."

    fun isAgingCategoryName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val normalized = name.trim().lowercase()
        return normalized == "aging" ||
            normalized == "ageing" ||
            normalized.contains("aging") ||
            normalized.contains("ageing") ||
            normalized.contains("child to aged") ||
            normalized.contains("child to age")
    }

    fun agingCategory(categories: List<HomeItem>): Category? {
        return categories.filterIsInstance<HomeItem.CategoryItem>()
            .map { it.category }
            .firstOrNull { isAgingCategoryName(it.name) }
    }

    fun agingTemplates(categories: List<HomeItem>): List<Template> {
        return agingCategory(categories)
            ?.templates
            ?.sortedBy { it.priority }
            .orEmpty()
    }

    fun enhancerTemplateId(categories: List<HomeItem>): String {
        return firstTemplateInCategory(categories, "Women Portrait")
            ?: firstTemplateInCategory(categories, "Men Portrait")
            ?: ENHANCER_FALLBACK_TEMPLATE_ID
    }

    fun enhancerPrompt(categories: List<HomeItem>, templateId: String? = null): String {
        val resolvedId = templateId?.takeIf { it.isNotBlank() } ?: enhancerTemplateId(categories)
        return templatePrompt(categories, resolvedId)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ENHANCER_PROMPT
    }

    fun agingTemplateId(categories: List<HomeItem>): String {
        return agingTemplates(categories).firstOrNull()?.id ?: AGING_FALLBACK_TEMPLATE_ID
    }

    fun templatePrompt(categories: List<HomeItem>, templateId: String): String? {
        return allTemplates(categories).firstOrNull { it.id == templateId }?.prompt
    }

    private fun firstTemplateInCategory(categories: List<HomeItem>, categoryName: String): String? {
        return categories.filterIsInstance<HomeItem.CategoryItem>()
            .firstOrNull { it.category.name.contains(categoryName, ignoreCase = true) }
            ?.category
            ?.templates
            ?.firstOrNull()
            ?.id
    }

    private fun allTemplates(categories: List<HomeItem>): List<Template> {
        return categories.filterIsInstance<HomeItem.CategoryItem>()
            .flatMap { it.category.templates }
    }
}
