package com.aiface.aging.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Resolves access-token expiry from backend fields.
 * Prefers [expiresIn] (seconds), then [expiresAt] (ISO / common formats),
 * otherwise applies [defaultTtlMs] from now.
 */
object TokenExpiryParser {

    /** Default 7-day TTL when API omits expiry — forces Get Token on reopen after that. */
    const val DEFAULT_TTL_MS: Long = 7L * 24L * 60L * 60L * 1000L

    fun resolveExpiresAtMillis(
        expiresAt: String?,
        expiresIn: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        defaultTtlMs: Long = DEFAULT_TTL_MS,
    ): Long {
        if (expiresIn != null && expiresIn > 0L) {
            return nowMillis + expiresIn * 1_000L
        }
        val parsed = parseExpiresAt(expiresAt)
        if (parsed != null && parsed > 0L) return parsed
        return nowMillis + defaultTtlMs
    }

    private fun parseExpiresAt(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        value.toLongOrNull()?.let { epoch ->
            // Seconds vs millis heuristic.
            return if (epoch < 1_000_000_000_000L) epoch * 1_000L else epoch
        }
        return tryParseInstant(value)
    }

    private fun tryParseInstant(value: String): Long? {
        val candidates = listOf(
            value,
            value.replace(' ', 'T'),
        )
        for (candidate in candidates) {
            try {
                return Instant.parse(candidate).toEpochMilli()
            } catch (_: Exception) {
            }
            try {
                return OffsetDateTime.parse(candidate).toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
            try {
                return LocalDateTime.parse(candidate, DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            } catch (_: Exception) {
            }
        }
        return null
    }
}
