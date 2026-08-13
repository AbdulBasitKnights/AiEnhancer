package com.aiface.aging.domain.model

import androidx.annotation.Keep

@Keep
data class Subscription(
    val planId: String?,
    val planName: String?,
    val durationDays: Int?,
    val videoCreditsRemaining: Int,
    val imageCreditsRemaining: Int,
    val startedAt: String?,
    val expiresAt: String?,
    val status: String?,
) {
    val isActive: Boolean get() = status == "active"
    val isFree: Boolean get() = planId == null
}
