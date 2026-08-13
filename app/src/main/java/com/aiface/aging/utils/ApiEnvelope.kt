package com.aiface.aging.utils

/** Helpers for `{ status, data, message }` backend responses. */
object ApiEnvelope {
    fun isSuccess(status: Int?): Boolean {
        return status == null || status == 200 || status == 201
    }
}
