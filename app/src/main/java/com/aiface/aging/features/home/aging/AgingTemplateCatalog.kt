package com.aiface.aging.features.home.aging

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aiface.aging.R
import com.aiface.aging.domain.model.Template
import com.aiface.aging.features.home.HomeAiTemplateResolver
import com.aiface.aging.features.home.HomeItem

object AgingTemplateCatalog {

    private data class Definition(
        @StringRes val titleRes: Int,
        @DrawableRes val thumbnailRes: Int,
        val defaultPrompt: String,
    )

    private val definitions =
        listOf(
            Definition(
                titleRes = R.string.aging_template_child,
                thumbnailRes = R.drawable.child,
                defaultPrompt =
                    "Transform the subject into a realistic 3–6 year old child version, matching the face age and youthful appearance of the reference: soft round cheeks, small smooth facial features, innocent natural smile, bright expressive eyes, childlike facial proportions, smooth skin texture, short youthful face shape, natural hair, and realistic lighting. Preserve the original identity, gender, hairstyle direction, expression style, and overall face structure as much as possible, but make the face clearly look like a young child of this age. Keep the result natural, high-quality, realistic, and detailed.",
            ),
            Definition(
                titleRes = R.string.aging_template_boy,
                thumbnailRes = R.drawable.boy,
                defaultPrompt =
                    "Transform the subject into a realistic 10–12 year old pre-teen child version, matching the face age and youthful appearance of the reference: soft rounded cheeks, smooth natural skin, innocent gentle smile, bright expressive eyes, youthful facial proportions, small soft jawline, natural eyebrows, healthy thick hair, and a fresh childlike face. Preserve the original identity, gender, pose, hairstyle direction, expression style, and overall face structure as much as possible, but make the face clearly look like a natural 10–12 year old child. Keep the result realistic, high-quality, detailed, and natural.",
            ),
            Definition(
                titleRes = R.string.aging_template_teen,
                thumbnailRes = R.drawable.teen,
                defaultPrompt =
                    "Transform the subject into a realistic 15–18 year old teenage version, matching the face age and youthful appearance of the reference: smooth skin, soft but slightly defined facial structure, fresh youthful face, clear bright eyes, natural eyebrows, light teen facial proportions, subtle jawline, healthy youthful hair, and an overall realistic teenage look. Preserve the original identity, gender, pose, hairstyle direction, expression style, and overall face structure as much as possible, but make the face clearly look like a natural teenager of this age group. Keep the result realistic, high-quality, detailed, and natural.",
            ),
            Definition(
                titleRes = R.string.aging_template_young,
                thumbnailRes = R.drawable.young,
                defaultPrompt =
                    "Transform the subject into a realistic 25–30 year old young adult version, matching the face age and youthful mature appearance of the reference: smooth natural skin, defined young adult facial structure, sharp but soft jawline, fresh face, clear expressive eyes, natural eyebrows, healthy skin texture, subtle facial hair or light beard if suitable, full youthful hair, and balanced adult facial proportions. Preserve the original identity, gender, pose, hairstyle direction, expression style, and overall face structure as much as possible, but make the face clearly look like a natural young adult of this age. Keep the result realistic, high-quality, detailed, and professional.",
            ),
            Definition(
                titleRes = R.string.aging_template_mature,
                thumbnailRes = R.drawable.mature,
                defaultPrompt =
                    "Transform the subject into a realistic 45–55 year old middle-aged version, matching the face age and mature appearance of the reference: slightly mature facial structure, subtle forehead lines, light crow's feet around the eyes, mild smile lines, natural skin texture, slightly defined cheeks, mature jawline, realistic under-eye details, and salt-and-pepper hair or slightly graying hair near the sides. Preserve the original identity, gender, pose, hairstyle direction, expression style, and overall face structure as much as possible, but make the face clearly look like a natural middle-aged adult. Keep the result realistic, high-quality, detailed, and professional.",
            ),
            Definition(
                titleRes = R.string.aging_template_old,
                thumbnailRes = R.drawable.old,
                defaultPrompt =
                    "Transform the subject into a realistic 70–80 year old elderly version, matching the face age and mature appearance of the reference: white or gray hair, natural receding hairline, deep forehead wrinkles, crow's feet around the eyes, under-eye lines, mature skin texture, visible age spots, soft sagging cheeks, defined smile lines, slightly thinner lips, elderly facial proportions, and a calm natural expression. Preserve the original identity, gender, pose, hairstyle direction, expression style, and overall face structure as much as possible, but make the face clearly look like an elderly person of this age. Keep the result realistic, natural, high-quality, detailed, and not over-edited.",
            ),
        )

    fun resolve(categories: List<HomeItem>): List<AgingTemplateOption> {
        val apiTemplates = HomeAiTemplateResolver.agingTemplates(categories)
        if (apiTemplates.isNotEmpty()) {
            return apiTemplates.mapIndexed { index, template ->
                mapApiTemplate(template, index)
            }
        }

        return definitions.mapIndexed { index, definition ->
            toFallbackOption(definition, index)
        }
    }

    private fun mapApiTemplate(template: Template, index: Int): AgingTemplateOption {
        val definition = definitions.getOrNull(index)
        return AgingTemplateOption(
            templateId = template.id,
            titleRes = definition?.titleRes ?: R.string.aging,
            thumbnailRes = definition?.thumbnailRes ?: R.drawable.child,
            prompt = template.prompt?.takeIf { it.isNotBlank() } ?: definition?.defaultPrompt.orEmpty(),
            thumbnailUrl = resolveThumbnailUrl(template),
            displayTitle = template.title?.takeIf { it.isNotBlank() },
        )
    }

    private fun toFallbackOption(definition: Definition, index: Int): AgingTemplateOption {
        return AgingTemplateOption(
            // Unique per slot — shared AGING_FALLBACK id made selection always snap to first item.
            templateId = "aging_fallback_$index",
            titleRes = definition.titleRes,
            thumbnailRes = definition.thumbnailRes,
            prompt = definition.defaultPrompt,
            thumbnailUrl = null,
            displayTitle = null,
        )
    }

    /**
     * Keep user pick across catalog refresh / gallery return.
     * Prefer templateId, then titleRes, then previous index.
     */
    fun resolveSelectionId(
        options: List<AgingTemplateOption>,
        preferredTemplateId: String?,
        preferredTitleRes: Int?,
        preferredIndex: Int = -1,
    ): String? {
        if (options.isEmpty()) return null
        if (!preferredTemplateId.isNullOrBlank()) {
            options.firstOrNull { it.templateId == preferredTemplateId }?.templateId?.let { return it }
        }
        if (preferredTitleRes != null && preferredTitleRes != 0) {
            options.firstOrNull { it.titleRes == preferredTitleRes }?.templateId?.let { return it }
        }
        if (preferredIndex in options.indices) {
            return options[preferredIndex].templateId
        }
        return preferredTemplateId
    }

    fun indexOfOption(options: List<AgingTemplateOption>, templateId: String?): Int {
        if (templateId.isNullOrBlank()) return -1
        return options.indexOfFirst { it.templateId == templateId }
    }

    private fun resolveThumbnailUrl(template: Template): String? {
        return template.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: template.mediaUrl?.takeIf { it.isNotBlank() }
            ?: template.gifUrl?.takeIf { it.isNotBlank() }
    }
}
