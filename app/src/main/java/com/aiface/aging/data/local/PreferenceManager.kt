package com.aiface.aging.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiface.aging.shared.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val USER_TOKEN_KEY = stringPreferencesKey("user_token")
        val APP_NAME_KEY = stringPreferencesKey("procapture")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        val FACE_SWAP_TOKEN_KEY = stringPreferencesKey("face_swap_token")
        val TOKEN_EXPIRES_AT_KEY = stringPreferencesKey("token_expires_at")
    }

    @Volatile
    private var memoryFaceSwapToken: String? = null
    @Volatile
    private var faceSwapCacheWarmed = false

    // ── Token ─────────────────────────────────────────────────────────────────

    suspend fun saveUserToken(token: String) {
        dataStore.edit { it[USER_TOKEN_KEY] = token }
    }

    suspend fun saveTokenExpiresAt(expiresAtMillis: Long) {
        dataStore.edit { it[TOKEN_EXPIRES_AT_KEY] = expiresAtMillis.toString() }
    }

    suspend fun saveAuthSession(
        token: String,
        userId: String,
        deviceId: String,
        appName: String,
        expiresAtMillis: Long,
    ) {
        dataStore.edit {
            it[USER_TOKEN_KEY] = token
            it[USER_ID_KEY] = userId
            it[DEVICE_ID_KEY] = deviceId
            it[APP_NAME_KEY] = appName
            it[TOKEN_EXPIRES_AT_KEY] = expiresAtMillis.toString()
        }
    }

    /** Clears auth fields so splash/home can register again after 401/stale token. */
    suspend fun clearAuthSession() {
        dataStore.edit {
            it.remove(USER_TOKEN_KEY)
            it.remove(USER_ID_KEY)
            it.remove(TOKEN_EXPIRES_AT_KEY)
        }
    }

    val userToken: Flow<String?> = dataStore.data.map { it[USER_TOKEN_KEY] }

    val tokenExpiresAtMillis: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[TOKEN_EXPIRES_AT_KEY]?.toLongOrNull()
    }

    /** True when a non-blank token exists and is not past local expiry. */
    suspend fun hasValidAccessToken(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val token = userToken.first()
        if (token.isNullOrBlank()) return false
        val expiresAt = tokenExpiresAtMillis.first() ?: return true
        return nowMillis < expiresAt
    }

    suspend fun isAccessTokenExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val token = userToken.first()
        if (token.isNullOrBlank()) return true
        val expiresAt = tokenExpiresAtMillis.first() ?: return false
        return nowMillis >= expiresAt
    }

    // ── App name ──────────────────────────────────────────────────────────────

    suspend fun saveAppName(appName: String) {
        dataStore.edit { it[APP_NAME_KEY] = appName }
    }

    val appName: Flow<String?> = dataStore.data.map { it[APP_NAME_KEY] }

    // ── User ID ───────────────────────────────────────────────────────────────

    suspend fun saveUserId(userId: String) {
        dataStore.edit { it[USER_ID_KEY] = userId }
    }

    val userId: Flow<String?> = dataStore.data.map { it[USER_ID_KEY] }

    // ── Device ID ─────────────────────────────────────────────────────────────

    suspend fun saveDeviceId(deviceId: String) {
        dataStore.edit { it[DEVICE_ID_KEY] = deviceId }
    }

    val deviceId: Flow<String?> = dataStore.data.map { it[DEVICE_ID_KEY] }

    // ── Face Swap token ───────────────────────────────────────────────────────

    suspend fun warmFaceSwapCache() {
        if (faceSwapCacheWarmed) return
        val prefs = dataStore.data.first()
        memoryFaceSwapToken = prefs[FACE_SWAP_TOKEN_KEY]
        faceSwapCacheWarmed = true
    }

    fun peekFaceSwapToken(): String? = if (faceSwapCacheWarmed) memoryFaceSwapToken else null

    suspend fun readFaceSwapToken(): String? {
        warmFaceSwapCache()
        return memoryFaceSwapToken
    }

    suspend fun saveFaceSwapToken(token: String) {
        memoryFaceSwapToken = token
        faceSwapCacheWarmed = true
        dataStore.edit { it[FACE_SWAP_TOKEN_KEY] = token }
    }

    val faceSwapToken: Flow<String?> = dataStore.data.map { it[FACE_SWAP_TOKEN_KEY] }
}