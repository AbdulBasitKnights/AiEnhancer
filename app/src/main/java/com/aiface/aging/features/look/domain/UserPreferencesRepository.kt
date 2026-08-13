package com.aiface.aging.features.look.domain

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    suspend fun clearAllPreferences()
    
    // Makeup color preferences
    fun getLipstickColor(): Flow<String>
    suspend fun saveLipstickColor(color: String)
    fun getLipstickOpacity(): Flow<Int>
    suspend fun saveLipstickOpacity(opacity: Int)
    
    fun getBlushColor(): Flow<String>
    suspend fun saveBlushColor(color: String)
    fun getBlushOpacity(): Flow<Int>
    suspend fun saveBlushOpacity(opacity: Int)
    
    fun getEyeshadowColor(): Flow<String>
    suspend fun saveEyeshadowColor(color: String)
    fun getEyeshadowOpacity(): Flow<Int>
    suspend fun saveEyeshadowOpacity(opacity: Int)
    
    fun getEyebrowColor(): Flow<String>
    suspend fun saveEyebrowColor(color: String)
    fun getEyebrowOpacity(): Flow<Int>
    suspend fun saveEyebrowOpacity(opacity: Int)
    
    // First session tracking
    fun isFirstSession(): Flow<Boolean>
    suspend fun setFirstSessionCompleted()

    fun getSelectedLanguage(): Flow<String>
    suspend fun saveSelectedLanguage(languageCode: String)
}
