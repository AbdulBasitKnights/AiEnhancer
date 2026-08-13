package com.aiface.aging.shared

import android.view.View

/**
 * Debounced click listener — prefer over [View.setOnClickListener] on CTAs.
 * Respects [ClickGuard] (debounce + ad/loader busy).
 */
fun View.setSafeClickListener(
    intervalMs: Long = ClickGuard.DEFAULT_INTERVAL_MS,
    onClick: (View) -> Unit,
) {
    setOnClickListener {
        if (!ClickGuard.tryClick(intervalMs)) return@setOnClickListener
        onClick(it)
    }
}
