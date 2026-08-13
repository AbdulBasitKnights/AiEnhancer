package com.aiface.aging.data.remote

import com.aiface.aging.data.local.PreferenceManager
import com.aiface.aging.shared.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injects Authorization + X-App-Name for generationlab backend requests.
 * Skips auth headers on register / get-token so expired tokens do not poison those calls.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val preferenceManager: PreferenceManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        val skipAuth = shouldSkipAuth(path, original.method)

        val token = if (skipAuth) {
            null
        } else {
            runBlocking { preferenceManager.userToken.first() }
        }
        val appName = Constants.APP_NAME
        val isMultipart = original.body?.contentType()?.type == "multipart"

        val builder = original.newBuilder()
            .removeHeader("Content-Type")
            .header("Accept", "application/json")
            .header("X-App-Name", appName)

        if (!isMultipart) {
            builder.header("Content-Type", "application/json")
        }

        if (!token.isNullOrEmpty()) {
            builder.header("Authorization", "Token $token")
        }

        return chain.proceed(builder.build())
    }

    private fun shouldSkipAuth(path: String, method: String): Boolean {
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
}
