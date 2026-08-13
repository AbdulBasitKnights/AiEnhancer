package com.aiface.aging.ads_nextgen

import android.os.Bundle
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustEvent
import com.facebook.appevents.AppEventsLogger
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.aiface.aging.AiFaceApp
import com.aiface.aging.shared.ads.trackPaidAdRevenue
import com.aiface.aging.utils.AdjustConstant.TAG2

/**
 * Next-Gen replacement for legacy [com.google.android.gms.ads.OnPaidEventListener].
 * Use from [AdEventCallback.onAdPaid].
 */
object NextGenAdRevenue {

    fun track(adUnitId: String?, adValue: AdValue, source: String) {
        trackPaidAdRevenue(adUnitId,adValue,source,"AdMob")
        /*adjustRevenueMMP(
            adUnitId = adUnitId,
            adRevenue = adValue.valueMicros / 1_000_000.0,
            currency = adValue.currencyCode,
            event_token = "",
            source = source,
        )*/
    }
    fun trackIapEventFBAdjust(eventToken: String, eventName: String, revenue:Double=0.0) {
        try {
            val event = AdjustEvent(eventToken)
//            event.addPartnerParameter("eventName", eventName)
            Adjust.trackEvent(event)
            Log.d("AdjustEvent", "Tracked event: $eventName | Token: $eventToken")

            AiFaceApp.context?.let { context ->
                val logger = AppEventsLogger.newLogger(context)
                val params = Bundle().apply {
                    putString("event_id", "Subscription")
                    putString("price_currency", eventName)
                }
                    logger.logEvent("fb_iap_revenue", revenue, params)


            } ?: run {
                Log.w(TAG2, "Meta log skipped (application context is null)")
            }
        } catch (e: Exception) {
            Log.e("AdjustEvent", "Error tracking event $eventName: ${e.message}")
        }
    }
}
