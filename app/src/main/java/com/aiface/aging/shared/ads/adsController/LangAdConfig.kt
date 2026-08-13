package com.aiface.aging.shared.ads.adsController

import androidx.annotation.Keep

@Keep
data class LangAdConfig(
    val SessionCount: Int = 0,

    // Language screen
    val LanguageButton: Int = 0,
    val LanguageButtonDelay: Int = 3,

    // Interstitial ads

    // Native ads
    val NativeAll1: Int = 1,
    val NativeAll2: Int  = 1,
    val NativeFormat: Int  = 1,

    // Ad CTA
    val AdCtaColor: String = "#4E2BF6",
    val AdCtaTextColor: String = "#FFFFFF",
    val CtaTextStyle: String =  "bold"
)