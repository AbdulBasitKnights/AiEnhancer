package com.aiface.aging.ads_nextgen

import com.aiface.aging.BuildConfig

object AdConstants {
    const val APP_ID = "ca-app-pub-5972202469838280~6432133240"

//    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    var INTERSTITIAL = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-5972202469838280/1591438326"
    var REWARDED = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else "ca-app-pub-5972202469838280/9278356658"
    var APP_OPEN = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9257395921" else "ca-app-pub-5972202469838280/7965274988"
    var BANNER = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9214589741" else "ca-app-pub-5972202469838280/5271094817"
    var NATIVE = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5972202469838280/3214797175"

    const val PRELOAD_INTERSTITIAL = "preload_interstitial"
    const val PRELOAD_REWARDED = "preload_rewarded"
    const val PRELOAD_APP_OPEN = "preload_app_open"
    const val PRELOAD_BANNER = "preload_banner"
    const val PRELOAD_NATIVE = "preload_native"

    const val APP_OPEN_MAX_AGE_HOURS = 4L
}

enum class AdLoadMode {
    NORMAL,
    PRELOAD
}

enum class AdFormat {
    INTERSTITIAL,
    REWARDED,
    APP_OPEN,
    BANNER,
    NATIVE
}
