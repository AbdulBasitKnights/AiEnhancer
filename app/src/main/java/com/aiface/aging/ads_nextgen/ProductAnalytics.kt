package com.aiface.aging.ads_nextgen

import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.FirebaseLogUtils

/**
 * Product analytics catalog from the Imora event tracking sheet.
 * All Firebase product events should go through [log].
 */
object ProductAnalytics {

    // ── Screens ──────────────────────────────────────────────────────────────
    const val SCREEN_SPLASH = "SplashScreen"
    const val SCREEN_PRO = "ProPanel"
    const val SCREEN_LANGUAGE = "LanguageScreen"
    const val SCREEN_ONBOARDING = "OnboardingScreen"
    const val SCREEN_SURVEY = "SurveyScreen"
    const val SCREEN_HOME = "HomeScreen"
    const val SCREEN_PREVIEW = "PreviewScreen"
    const val SCREEN_IMAGE_TO_IMAGE = "ImageToImageScreen"
    const val SCREEN_EDIT = "EditScreen"
    const val SCREEN_LOADING = "LoadingScreen"
    const val SCREEN_RESULT = "ResultScreen"
    const val SCREEN_TEMPLATES = "TemplatesScreen"
    const val SCREEN_AI_GEN = "AiGenScreen"
    const val SCREEN_AI_VIDEO = "AiVideoScreen"
    const val SCREEN_TOOLS = "ToolsScreen"
    const val SCREEN_SEE_ALL = "SeeAllScreen"
    const val SCREEN_HISTORY = "HistoryScreen"
    const val SCREEN_SETTINGS = "SettingsScreen"
    const val SCREEN_EXIT = "ExitScreen"
    const val SCREEN_SELECT_IMAGE = "SelectImageScreen"
    const val SCREEN_OVERALL = "OverallApp"

    // ── Param keys ───────────────────────────────────────────────────────────
    const val PARAM_AD_FORMAT = "ad_format"
    const val PARAM_SOURCE_FEATURE = "source_feature"
    const val PARAM_SOURCE = "source"
    const val PARAM_PLAN = "plan"
    const val PARAM_LANGUAGE = "language"
    const val PARAM_CATEGORY_NAME = "category_name"
    const val PARAM_SECTION_NAME = "section_name"
    const val PARAM_TEMPLATE_ID = "template_id"
    const val PARAM_TEMPLATE_NAME = "template_name"
    const val PARAM_IMAGE_SOURCE = "image_source"
    const val PARAM_TOOL_NAME = "tool_name"
    const val PARAM_CHIP_NAME = "chip_name"
    const val PARAM_GENERATION_TYPE = "generation_type"
    const val PARAM_ERROR_TYPE = "error_type"
    const val PARAM_ITEM_ID = "item_id"

    // ── Overall App / Ads ────────────────────────────────────────────────────
    const val TRACK_AD_REQUEST = "track_ad_request"
    const val TRACK_AD_MATCHED_REQUEST = "track_ad_matched_request"
    const val PAID_AD_IMPRESSION = "paid_ad_impression"

    // ── Home ─────────────────────────────────────────────────────────────────
    const val HOME_VIEW = "home_view"
    const val UPGRADE_CLICK = "upgrade_click"
    const val HOME_SETTINGS_CLICK = "home_settings_click"
    const val INSPIRATION_TRY_CLICK = "inspiration_try_click"
    const val HOME_CATEGORY_CLICK = "home_category_click"
    const val SEE_ALL_CLICK = "see_all_click"
    const val TEMPLATE_CLICK = "template_click"
    const val NATIVE_HOME_SCR_VIEW = "native_home_scr_view"
    const val NATIVE_HOME_SCR_COMPLETE = "native_home_scr_complete"

    // ── Bottom Nav ───────────────────────────────────────────────────────────
    const val AI_VIDEO_VIEW = "ai_video_view"
    const val AI_GEN_VIEW = "ai_gen_view"
    const val TOOLS_VIEW = "tools_view"
    const val HISTORY_VIEW = "history_view"

    // ── Templates ────────────────────────────────────────────────────────────
    const val TEMPLATES_VIEW = "templates_view"
    const val SELECT_IMAGE_VIEW = "select_image_view"
    const val IMAGE_SOURCE_SELECT = "image_source_select"

    // ── AI Gen / Tools ───────────────────────────────────────────────────────
    const val AI_ENHANCE_TRY_CLICK = "ai_enhance_try_click"
    const val TOOL_CLICK = "tool_click"
    const val IMAGE_TO_IMAGE_VIEW = "image_to_image_view"
    const val II_UPLOAD_IMAGE_CLICK = "ii_upload_image_click"
    const val II_SECOND_IMAGE_CLICK = "ii_second_image_click"
    const val II_PROMPT_CHIP_CLICK = "ii_prompt_chip_click"

    // ── Generation Flow ──────────────────────────────────────────────────────
    const val GENERATE_CLICK = "generate_click"
    const val REWARDED_AD_VIEW = "rewarded_ad_view"
    const val REWARDED_AD_COMPLETE = "rewarded_ad_complete"
    const val REWARDED_AD_CLOSE_CLICK = "rewarded_ad_close_click"
    const val REWARDED_AD_FAILED = "rewarded_ad_failed"
    const val GENERATION_LOADING = "generation_loading"
    const val GENERATION_SUCCESS = "generation_success"
    const val GENERATION_FAILED = "generation_failed"

    const val GENERATION_TYPE_IMAGE = "image"
    const val GENERATION_TYPE_VIDEO = "video"
    const val IMAGE_SOURCE_CAMERA = "camera"
    const val IMAGE_SOURCE_GALLERY = "gallery"
    const val IMAGE_SOURCE_EXAMPLE = "example"

    fun log(
        event: String,
        screen: String = SCREEN_HOME,
        params: Map<String, String> = emptyMap(),
    ) {
        FirebaseLogUtils.firebaseUserAction(event, screen, params)
    }
    fun sectionNameSlug(name: String): String =
        name.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

}