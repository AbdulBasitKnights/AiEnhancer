package com.aiface.aging.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.aiface.aging.AiFaceApp
import com.aiface.aging.utils.NetworkUtils.Companion.isOnline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.let

object FirebaseLogUtils {
    private var fbAnalytics: FirebaseAnalytics? = null
    fun initFirebaseAnalytics(context: Context) {
        fbAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logEvent(eventName: String, logEvent: String) {
       // Singular.event(eventName)
        val param = Bundle()
        param.putString(eventName, logEvent)
        fbAnalytics?.let { analytics ->
            analytics.logEvent(eventName, param)
        }
    }
    fun logEvents(eventName: String, logEvent: String,param: Map<String, String> = emptyMap()) {
       // Singular.event(eventName)
        val param = Bundle()
        param.putString(eventName, logEvent)
        fbAnalytics?.let { analytics ->
            analytics.logEvent(eventName, param)
        }
    }
    fun firebaseUserAction(
        action: String,
        activityName: String,
        extraParams: Map<String, String> = emptyMap(),
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            AiFaceApp.context?.let {
                if (isOnline(it)) {
                    if (FirebaseApp.getApps(it).isEmpty()) {
                        FirebaseApp.initializeApp(it)
                    } else {
                        if (fbAnalytics == null) {
                            fbAnalytics = Firebase.analytics
                        }
                        fbAnalytics?.let { analytics ->
                            analytics.logEvent(action) {
                                param("Screen_Name", activityName)
                                extraParams.forEach { (key, value) ->
                                    if (key.isNotBlank() && value.isNotBlank()) {
                                        param(key, value.take(100))
                                    }
                                }
                            }
                        }
                        Log.d("firebaseE", "$action params=$extraParams")
                    }
                }
            }
        }
    }

}