package com.aiface.aging.shared

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

const val DATA_STORE_NAME = "PRO-CAPTURE-STUDIO"

val IS_ONBOARD = booleanPreferencesKey("onboarding-check")
val IS_LANGUAGE_SPLASH = booleanPreferencesKey("language-check")
val HAS_SHOWN_RATE_US = booleanPreferencesKey("has-shown-rate-us")

val IS_LANGUAGE = stringPreferencesKey("language")

enum class CollageType(type : String) {
    Dynamic("dynamic"), Classic("classic"), Shapes("shapes")
}

const val ASSETS_PATH_FILTER_HEADER = "filters/offline_filters_json/FilterHeader.json"

const val ASSETS_PATH_FILTER_PACK = "filters/offline_filters_json/FilterPacks.json"

object Constants {
    /** Legacy image-generation backend (generate / generations endpoints). */
    const val BASE_URL = "https://apero-image-app-backend.aspire.pics/"

    /** New backend: user registration + public categories. */
    const val NEW_BASE_URL = "https://generationlab-appbackend.aspire.pics/"

    /** App name expected by the new backend. Sent in X-App-Name on every request. */
      const val APP_NAME = "face-aging"
    /** Same as Ai Face Aging — has Aging category templates. */
//    const val APP_NAME = "beauty_camera"

    const val PREFERENCES_NAME = "aienhancer_preferences"
    const val DEFAULT_TIMEOUT = 30L

    const val FACE_SWAP_BASE_URL = "https://face-swap-backend.aspire.pics/"
    const val FACE_SWAP_API_KEY = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
    const val FACE_SWAP_APP_LABEL = "hub"
    const val FACE_SWAP_TIMEOUT = 60L
    const val FACE_SWAP_POLL_DELAY_MS = 2_500L
    const val FACE_SWAP_POLL_MAX_ATTEMPTS = 72
}
