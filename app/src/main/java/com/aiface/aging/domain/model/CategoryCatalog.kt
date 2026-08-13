package com.aiface.aging.domain.model

object CategoryCatalog {

    fun isVideoGeneration(generationType: String): Boolean {
        return generationType.contains("video", ignoreCase = true)
    }

    /** Categories/templates for Home (AI Photos catalog). */
    fun photoCategories(categories: List<Category>): List<Category> {
        return categories.mapNotNull { category ->
            val templates =
                category.templates.filter { it.isActive && !isVideoGeneration(it.generationType) }
            if (templates.isEmpty()) null else category.copy(templates = templates)
        }
    }

    /** Categories/templates for Library → AI Videos (video generation only). */
    fun videoCategories(categories: List<Category>): List<Category> {
        return categories.mapNotNull { category ->
            val templates =
                category.templates.filter { it.isActive && isVideoGeneration(it.generationType) }
            if (templates.isEmpty()) null else category.copy(templates = templates)
        }
    }
}
