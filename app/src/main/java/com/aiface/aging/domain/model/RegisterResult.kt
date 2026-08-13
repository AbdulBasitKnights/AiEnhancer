package com.aiface.aging.domain.model

import androidx.annotation.Keep

@Keep
data class RegisterResult(
    val token: String,
    val deviceId: String,
    val appName: String,
    val userId: String,
    /** Epoch millis; null means server did not provide expiry. */
    val expiresAtMillis: Long? = null,
)
