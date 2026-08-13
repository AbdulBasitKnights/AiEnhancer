package com.aiface.aging.features.look.domain


import com.aiface.aging.features.look.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {

    suspend fun clear() = repository.clearAllPreferences()
    
    // Makeup color preferences
    fun getLipstickColor(): Flow<String> = repository.getLipstickColor()
    suspend fun saveLipstickColor(color: String) = repository.saveLipstickColor(color)
    fun getLipstickOpacity(): Flow<Int> = repository.getLipstickOpacity()
    suspend fun saveLipstickOpacity(opacity: Int) = repository.saveLipstickOpacity(opacity)
    
    fun getBlushColor(): Flow<String> = repository.getBlushColor()
    suspend fun saveBlushColor(color: String) = repository.saveBlushColor(color)
    fun getBlushOpacity(): Flow<Int> = repository.getBlushOpacity()
    suspend fun saveBlushOpacity(opacity: Int) = repository.saveBlushOpacity(opacity)
    
    fun getEyeshadowColor(): Flow<String> = repository.getEyeshadowColor()
    suspend fun saveEyeshadowColor(color: String) = repository.saveEyeshadowColor(color)
    fun getEyeshadowOpacity(): Flow<Int> = repository.getEyeshadowOpacity()
    suspend fun saveEyeshadowOpacity(opacity: Int) = repository.saveEyeshadowOpacity(opacity)
    
    fun getEyebrowColor(): Flow<String> = repository.getEyebrowColor()
    suspend fun saveEyebrowColor(color: String) = repository.saveEyebrowColor(color)
    fun getEyebrowOpacity(): Flow<Int> = repository.getEyebrowOpacity()
    suspend fun saveEyebrowOpacity(opacity: Int) = repository.saveEyebrowOpacity(opacity)
    
    // First session tracking
    fun isFirstSession(): Flow<Boolean> = repository.isFirstSession()
    suspend fun setFirstSessionCompleted() = repository.setFirstSessionCompleted()
    fun getLanguage(): Flow<String> = repository.getSelectedLanguage()
    suspend fun setLanguage(lang: String) = repository.saveSelectedLanguage(lang)
}
