package com.aiface.aging.features.look.data

import com.aiface.aging.features.look.utils.LookPreferenceManager
import com.aiface.aging.features.look.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val preferenceManager: LookPreferenceManager,
) : UserPreferencesRepository {



    override suspend fun clearAllPreferences() {
        preferenceManager.clear()
    }
    
    // Makeup color preferences implementation
    override fun getLipstickColor(): Flow<String> {
        return preferenceManager.getValue(PreferenceKeys.LIPSTICK_COLOR, "#FF0000")
    }

    override suspend fun saveLipstickColor(color: String) {
        preferenceManager.putValue(PreferenceKeys.LIPSTICK_COLOR, color)
    }

    override fun getLipstickOpacity(): Flow<Int> {
        return preferenceManager.getValue(PreferenceKeys.LIPSTICK_OPACITY, 70)
    }

    override suspend fun saveLipstickOpacity(opacity: Int) {
        preferenceManager.putValue(PreferenceKeys.LIPSTICK_OPACITY, opacity)
    }

    override fun getBlushColor(): Flow<String> {
        return preferenceManager.getValue(PreferenceKeys.BLUSH_COLOR, "#FF6B6B")
    }

    override suspend fun saveBlushColor(color: String) {
        preferenceManager.putValue(PreferenceKeys.BLUSH_COLOR, color)
    }

    override fun getBlushOpacity(): Flow<Int> {
        return preferenceManager.getValue(PreferenceKeys.BLUSH_OPACITY, 60)
    }

    override suspend fun saveBlushOpacity(opacity: Int) {
        preferenceManager.putValue(PreferenceKeys.BLUSH_OPACITY, opacity)
    }

    override fun getEyeshadowColor(): Flow<String> {
        return preferenceManager.getValue(PreferenceKeys.EYESHADOW_COLOR, "#FFA500")
    }

    override suspend fun saveEyeshadowColor(color: String) {
        preferenceManager.putValue(PreferenceKeys.EYESHADOW_COLOR, color)
    }

    override fun getEyeshadowOpacity(): Flow<Int> {
        return preferenceManager.getValue(PreferenceKeys.EYESHADOW_OPACITY, 50)
    }

    override suspend fun saveEyeshadowOpacity(opacity: Int) {
        preferenceManager.putValue(PreferenceKeys.EYESHADOW_OPACITY, opacity)
    }

    override fun getEyebrowColor(): Flow<String> {
        return preferenceManager.getValue(PreferenceKeys.EYEBROW_COLOR, "#8B4513")
    }

    override suspend fun saveEyebrowColor(color: String) {
        preferenceManager.putValue(PreferenceKeys.EYEBROW_COLOR, color)
    }

    override fun getEyebrowOpacity(): Flow<Int> {
        return preferenceManager.getValue(PreferenceKeys.EYEBROW_OPACITY, 80)
    }

    override suspend fun saveEyebrowOpacity(opacity: Int) {
        preferenceManager.putValue(PreferenceKeys.EYEBROW_OPACITY, opacity)
    }
    
    // First session tracking implementation
    override fun isFirstSession(): Flow<Boolean> {
        return preferenceManager.getValue(PreferenceKeys.IS_FIRST_SESSION, true)
    }
    
    override suspend fun setFirstSessionCompleted() {
        preferenceManager.putValue(PreferenceKeys.IS_FIRST_SESSION, false)
    }

    // 🌐 Language preference implementation
    override fun getSelectedLanguage(): Flow<String> {
        return preferenceManager.getValue(PreferenceKeys.SELECTED_LANGUAGE, "en")
    }

    override suspend fun saveSelectedLanguage(languageCode: String) {
        preferenceManager.putValue(PreferenceKeys.SELECTED_LANGUAGE, languageCode)
    }
}
