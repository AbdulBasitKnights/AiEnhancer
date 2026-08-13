package com.aiface.aging.data.remote

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On HTTP 401: refresh token via Get Token / Register, update prefs, retry same request once.
 * Second 401 (or failed refresh) → toast "no data currently available" and stop.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefreshHelper: TokenRefreshHelper,
    private val authSessionEvents: AuthSessionEvents,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        val path = request.url.encodedPath

        if (shouldSkipAuthRefresh(path, request.method)) {
            return null
        }

        // Already retried once with a fresh token — give up.
        if (request.header(HEADER_AUTH_RETRY) != null || responseCount(response) > 1) {
            Log.e(TAG, "401 again after token refresh — notifying UI")
            authSessionEvents.notifyNoDataAvailable()
            return null
        }

        val newToken = runBlocking {
            tokenRefreshHelper.refreshAccessToken()
        }
        if (newToken.isNullOrBlank()) {
            Log.e(TAG, "Token refresh returned empty — notifying UI")
            authSessionEvents.notifyNoDataAvailable()
            return null
        }

        Log.d(TAG, "Retrying request with refreshed token: $path")
        return request.newBuilder()
            .header("Authorization", "Token $newToken")
            .header(HEADER_AUTH_RETRY, "1")
            .build()
    }

    private fun shouldSkipAuthRefresh(path: String, method: String): Boolean {
        val normalized = path.trimEnd('/')
        if (method.equals("POST", ignoreCase = true) &&
            (normalized.endsWith("/api/users") || normalized.endsWith("/api/users/"))
        ) {
            return true
        }
        if (normalized.contains("/api/users/token")) {
            return true
        }
        return false
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    companion object {
        private const val TAG = "TokenAuthenticator"
        const val HEADER_AUTH_RETRY = "X-Auth-Retry"
    }
}
