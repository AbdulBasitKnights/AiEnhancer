package com.aiface.aging.utils

import com.google.gson.JsonParser
import com.aiface.aging.domain.model.Resource
import retrofit2.Response

abstract class SafeApiCall {
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return Resource.Success(body)
                }
            }
            return error(parseErrorMessage(response))
        } catch (e: Exception) {
            return error(e.message ?: e.toString())
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        val code = response.code()
        val errorBody = response.errorBody()?.string().orEmpty()
        if (errorBody.isNotBlank()) {
            try {
                val json = JsonParser.parseString(errorBody).asJsonObject
                json.get("message")?.asString?.takeIf { it.isNotBlank() }?.let { msg ->
                    // Keep status code so UI / auth recovery can detect 401 after body-only messages.
                    return if (msg.contains(code.toString())) msg else "$code $msg"
                }
            } catch (_: Exception) {
                // Fall through to raw body / status line.
            }
            return if (errorBody.contains(code.toString())) errorBody else "$code $errorBody"
        }
        return "$code ${response.message()}"
    }

    private fun <T> error(message: String): Resource<T> {
        return Resource.Error(message)
    }
}
