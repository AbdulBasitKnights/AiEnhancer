package com.aiface.aging.features.home

/**
 * Resolves how many user photos a catalog template needs for generate.
 * API [image_count] == 2 → pick & upload two images (`input_img_file_one` + `input_img_file_two`).
 */
object TemplateImageRequirements {
    const val ARG_IMAGE_COUNT = "image_count"
    const val ARG_IMAGE_URI = "imageUri"
    const val ARG_IMAGE_URI_TWO = "imageUriTwo"

    fun requiredCount(imageCount: Int?): Int {
        val n = imageCount ?: 1
        return if (n >= 2) 2 else 1
    }
}
