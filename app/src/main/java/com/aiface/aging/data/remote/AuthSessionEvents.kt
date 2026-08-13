package com.aiface.aging.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.aiface.aging.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Surfaces final auth-failure UI (second 401 after refresh+retry).
 * Debounced so parallel 401s do not spam toasts.
 */
@Singleton
class AuthSessionEvents @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lastToastAtMs = AtomicLong(0L)

    fun notifyNoDataAvailable() {
        val now = System.currentTimeMillis()
        val previous = lastToastAtMs.get()
        if (now - previous < TOAST_DEBOUNCE_MS) return
        if (!lastToastAtMs.compareAndSet(previous, now)) return

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                context.getString(R.string.no_data_currently_available),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    companion object {
        private const val TOAST_DEBOUNCE_MS = 2_500L
    }
}
