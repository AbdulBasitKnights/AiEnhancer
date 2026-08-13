package com.aiface.aging.domain.model

import androidx.annotation.Keep

@Keep
data class Plan(
    val id: String,
    val name: String,
    val durationDays: Int,
    val videoCredits: Int,
    val imageCredits: Int,
    val isActive: Boolean,
) {
    /** Human-readable duration label (e.g. "7 days", "30 days"). */
    val durationLabel: String get() = "$durationDays days"
}
