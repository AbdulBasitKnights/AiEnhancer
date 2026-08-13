package com.aiface.aging.features.iap

import android.content.Context
import com.aiface.aging.shared.ads.AdsHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object ProSubscriptionChecker {

    const val TIMEOUT_MS = 6_500L

    suspend fun check(context: Context): Boolean {
        if (AdsHelper.FORCE_PRO_NO_ADS) return true
        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val connector = IapManager.getIapConnector(context.applicationContext)
                var finished = false
                connector.checkProStatus { isSubscribed ->
                    if (!finished && continuation.isActive) {
                        finished = true
                        continuation.resume(isSubscribed)
                    }
                }
            }
        } ?: false
    }
}
