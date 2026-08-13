package com.aiface.aging.data.remote

import com.aiface.aging.shared.Constants
import com.aiface.aging.data.local.PreferenceManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injects Face Swap headers exactly as defined in the Insomnia collection:
 *
 * Common (all Face Swap APIs):
 * - X-API-KEY
 * - X-App-Label
 *
 * Register Device (POST /auth/device):
 * - Content-Type: application/json
 * - (no Authorization)
 *
 * Categories / Templates:
 * - (no Authorization)
 *
 * Swap Face + Status:
 * - Authorization: Bearer <token from Register Device>
 *
 * Never forces Content-Type on multipart — OkHttp must set the boundary.
 */
@Singleton
class FaceSwapAuthInterceptor @Inject constructor(
    private val preferenceManager: PreferenceManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        val builder = original.newBuilder()
            .header(HEADER_API_KEY, Constants.FACE_SWAP_API_KEY)
            .header(HEADER_APP_LABEL, Constants.FACE_SWAP_APP_LABEL)

        when {
            isRegisterPath(path) -> {
                // Insomnia: Content-Type + X-API-KEY + X-App-Label (no Bearer)
                builder.removeHeader(HEADER_AUTHORIZATION)
                builder.header(HEADER_CONTENT_TYPE, "application/json")
            }
            isAuthenticatedPath(path) -> {
                // Insomnia: X-API-KEY + X-App-Label + Authorization: Bearer <token>
                // Do not set Content-Type here (multipart must keep its boundary).
                applyBearerToken(builder)
            }
            else -> {
                // Categories / Templates: X-API-KEY + X-App-Label only
                builder.removeHeader(HEADER_AUTHORIZATION)
            }
        }
        return chain.proceed(builder.build())
    }

    private fun applyBearerToken(builder: Request.Builder) {
        val token = preferenceManager.peekFaceSwapToken()
            ?: runBlocking { preferenceManager.readFaceSwapToken() }

        builder.removeHeader(HEADER_AUTHORIZATION)
        if (!token.isNullOrBlank()) {
            builder.header(HEADER_AUTHORIZATION, "Bearer $token")
        }
    }

    private fun isRegisterPath(path: String): Boolean =
        path.contains("/auth/device")

    private fun isAuthenticatedPath(path: String): Boolean =
        path.contains("/swap-face") || path.contains("/generations/")

    companion object {
        private const val HEADER_API_KEY = "X-API-KEY"
        private const val HEADER_APP_LABEL = "X-App-Label"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
