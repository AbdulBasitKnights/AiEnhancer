package com.aiface.aging.data.remote

import android.util.Log
import com.aiface.aging.data.local.PreferenceManager
import com.aiface.aging.data.model.RegisterRequest
import com.aiface.aging.data.model.RegisterResponse
import com.aiface.aging.utils.ApiEnvelope
import com.aiface.aging.utils.DeviceIdManager
import com.aiface.aging.utils.TokenExpiryParser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Refreshes generationlab access token without using the authenticated OkHttp client
 * (avoids Authenticator recursion). Order: Get Token → Register fallback.
 */
@Singleton
class TokenRefreshHelper @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val deviceIdManager: DeviceIdManager,
    @Named("authPlain") private val authApi: ApiService,
) {
    private val mutex = Mutex()

    /**
     * @return fresh token string, or null if refresh/register failed.
     */
    suspend fun refreshAccessToken(): String? = mutex.withLock {
        val deviceId = deviceIdManager.getDeviceId()
        Log.d(TAG, "Refreshing access token for device=$deviceId")

        tryAuthCall { authApi.getAccessToken(RegisterRequest(deviceId)) }?.let { return it }
        Log.d(TAG, "Get Token failed — trying register")
        preferenceManager.clearAuthSession()
        tryAuthCall { authApi.registerUser(RegisterRequest(deviceId)) }?.let { return it }

        Log.e(TAG, "Token refresh failed (get-token + register)")
        null
    }

    private suspend fun tryAuthCall(
        call: suspend () -> Response<RegisterResponse>,
    ): String? {
        return try {
            val response = call()
            val body = response.body()
            if (!response.isSuccessful || body == null || !ApiEnvelope.isSuccess(body.status)) {
                Log.e(TAG, "Auth call failed: code=${response.code()} msg=${body?.message}")
                return null
            }
            val dto = body.data
            if (dto == null || dto.token.isBlank()) {
                Log.e(TAG, "Auth call returned empty token")
                return null
            }
            val expiresAtMillis = TokenExpiryParser.resolveExpiresAtMillis(
                expiresAt = dto.expiresAt,
                expiresIn = dto.expiresIn,
            )
            preferenceManager.saveAuthSession(
                token = dto.token,
                userId = dto.userId,
                deviceId = dto.deviceId,
                appName = dto.appName,
                expiresAtMillis = expiresAtMillis,
            )
            Log.d(TAG, "Auth session saved")
            dto.token
        } catch (e: Exception) {
            Log.e(TAG, "Auth call exception", e)
            null
        }
    }

    companion object {
        private const val TAG = "TokenRefreshHelper"
    }
}
