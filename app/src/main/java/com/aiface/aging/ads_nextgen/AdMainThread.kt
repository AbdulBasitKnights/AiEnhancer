package com.aiface.aging.ads_nextgen

import android.os.Handler
import android.os.Looper

/** Next-Gen AdMob (`GMA(BG)`) may invoke callbacks off the main thread. */
internal object AdMainThread {
    private val handler = Handler(Looper.getMainLooper())

    const val FRAGMENT_CONTINUE_DELAY_MS = 500L

    fun run(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }

    fun postDelayed(delayMs: Long, block: () -> Unit): Runnable {
        val runnable = Runnable { block() }
        handler.postDelayed(runnable, delayMs)
        return runnable
    }

    fun cancel(runnable: Runnable?) {
        if (runnable != null) handler.removeCallbacks(runnable)
    }
}
