package com.aiface.aging.shared.ads.adsController

import androidx.annotation.Keep

@Keep
data class OnboardingAdsConfig(

    val ObEnable: Int = 1,

  //  val Onboarding1: Int = 1,
    val ObNativeHigh1: Int = 1,
    val ObNativeAll1: Int = 1,

    val FullScreenNativeHigh1: Int = 0,
    val FullScreenNativeAll1: Int = 0,

  //  val Onboarding2: Int = 1,
    val ObNativeHigh3: Int = 1,
    val ObNativeAll3: Int = 1,

    val FullScreenNativeHigh2: Int = 1,
    val FullScreenNativeAll2: Int = 1,

  //  val Onboarding3: Int = 1,
    val ObNativeHigh4: Int = 1,
    val ObNativeAll4: Int = 1,

    // Interstitial ads
    val ObInterstitialHigh: Int = 1,
    val ObInterstitialAll: Int = 1,


    val FeatureNativeHigh1: Int = 1,
    val FeatureNativeAll1: Int = 1,

    val NativeFormat: Int = 2,
    val AdCtaColor: String = "#4E2BF6",
    val AdCtaTextColor: String = "#FFFFFF",
    val CtaTextStyle: String =  "bold",

    val NativeFullCrossDelay : Int = 3,
    val ShowNativeFullCross: Boolean = true
)
