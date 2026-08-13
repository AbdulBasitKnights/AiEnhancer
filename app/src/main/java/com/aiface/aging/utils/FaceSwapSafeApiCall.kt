package com.aiface.aging.utils

import android.util.Log
import com.google.gson.JsonParser
import com.aiface.aging.domain.model.Resource
import retrofit2.Response

/**
 * Face Swap networking helper that surfaces the backend [message] from error bodies
 * instead of only Retrofit's generic "400 Bad Request".
 */
abstract class FaceSwapSafeApiCall {

    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Resource.Success(body)
                } else {
                    Resource.Error("Empty response from server")
                }
            } else {
                val backendMessage = parseErrorBody(response)
                Log.e(
                    TAG,
                    "API ${response.raw().request.method} ${response.raw().request.url} " +
                        "→ HTTP ${response.code()}: $backendMessage",
                )
                Resource.Error(backendMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "API call failed", e)
            Resource.Error(e.message ?: e.toString())
        }
    }

    private fun <T> parseErrorBody(response: Response<T>): String {
        val raw = try {
            response.errorBody()?.string()?.trim().orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read errorBody", e)
            ""
        }

        if (raw.isNotBlank()) {
            Log.e(TAG, "errorBody=$raw")
            extractMessage(raw)?.let { return it }
            return raw
        }

        return "${response.code()} ${response.message()}".trim()
    }

    private fun extractMessage(rawJson: String): String? {
        return try {
            val element = JsonParser.parseString(rawJson)
            if (!element.isJsonObject) return null
            val obj = element.asJsonObject
            when {
                obj.has("message") && !obj.get("message").isJsonNull ->
                    obj.get("message").asString.takeIf { it.isNotBlank() }
                obj.has("detail") && !obj.get("detail").isJsonNull ->
                    obj.get("detail").asString.takeIf { it.isNotBlank() }
                obj.has("error") && !obj.get("error").isJsonNull ->
                    obj.get("error").asString.takeIf { it.isNotBlank() }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "FaceSwapApi"
    }
}
