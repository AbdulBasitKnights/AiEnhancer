package com.aiface.aging.features.look.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object PreferenceKeys {
    val USER_TOKEN = stringPreferencesKey("user_token")
    val AD_OPEN_COUNT = intPreferencesKey("ad_open_count")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val VOLUME_LEVEL = floatPreferencesKey("volume_level")
    
    // Makeup color preferences
    val LIPSTICK_COLOR = stringPreferencesKey("lipstick_color")
    val LIPSTICK_OPACITY = intPreferencesKey("lipstick_opacity")
    val BLUSH_COLOR = stringPreferencesKey("blush_color")
    val BLUSH_OPACITY = intPreferencesKey("blush_opacity")
    val EYESHADOW_COLOR = stringPreferencesKey("eyeshadow_color")
    val EYESHADOW_OPACITY = intPreferencesKey("eyeshadow_opacity")
    val EYEBROW_COLOR = stringPreferencesKey("eyebrow_color")
    val EYEBROW_OPACITY = intPreferencesKey("eyebrow_opacity")
    
    // First session tracking
    val IS_FIRST_SESSION = booleanPreferencesKey("is_first_session")
     val SELECTED_LANGUAGE =stringPreferencesKey("user_token")

}
