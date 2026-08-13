package com.aiface.aging.shared.ads.adsController

import android.util.Log
import androidx.annotation.Keep
import com.google.gson.Gson
import com.aiface.aging.shared.ads.AdsHelper


@Keep
object RemoteJsonConvertor {
    fun langJsonConvertor(splashJson: String) {
        try {
            val config = Gson().fromJson(splashJson, LangAdConfig::class.java)

            AdsHelper.langSessionRemote = config.SessionCount

            AdsHelper.languageButtonDelay = config.LanguageButtonDelay
            AdsHelper.languageButtonStyle = config.LanguageButton

            // Interstitial

            // Native
            AdsHelper.langNative1Enabled = config.NativeAll1 == 1
            AdsHelper.langNativeHigh1Enabled = config.NativeAll1 == 1
            AdsHelper.langNative2Enabled = config.NativeAll2 == 1
            AdsHelper.langNativeHigh2Enabled = config.NativeAll2 == 1

            AdsHelper.langNativeFormat = config.NativeFormat

            // CTA
            AdsHelper.langCtaColor = config.AdCtaColor
            AdsHelper.langCtaTextColor = config.AdCtaTextColor
            AdsHelper.langCtaTextStyle = config.CtaTextStyle


        } catch (e: Exception) {
            Log.e("AdsManager", "JSON parse error", e)
        }
    }

    fun obJsonConvertor(splashJson: String) {
        try {
            val config = Gson().fromJson(splashJson, OnboardingAdsConfig::class.java)


            /* AdsHelper.obFirstEnable = config.Onboarding1 == 1
             AdsHelper.obSecondEnable = config.Onboarding2 == 1
             AdsHelper.obThirdEnable = config.Onboarding3 == 1*/

            AdsHelper.obEnable = config.ObEnable == 1


            AdsHelper.obNative1Enabled = config.ObNativeAll1 == 1
            AdsHelper.obNative3Enabled = config.ObNativeAll3 == 1
            AdsHelper.obNative4Enabled = config.ObNativeAll4 == 1

            AdsHelper.obNativeHigh1Enabled = config.ObNativeHigh1 == 1
            AdsHelper.obNativeHigh3Enabled = config.ObNativeHigh3 == 1
            AdsHelper.obNativeHigh4Enabled = config.ObNativeHigh4 == 1

            AdsHelper.obNativeHighFullScr1Enabled = config.FullScreenNativeHigh1 == 1
            AdsHelper.obNativeHighFullScr2Enabled = config.FullScreenNativeHigh2 == 1


            AdsHelper.obNativeFullScr1Enabled = config.FullScreenNativeAll1 == 1
            AdsHelper.obNativeFullScr2Enabled = config.FullScreenNativeAll2 == 1

            AdsHelper.obInterstitialHighEnabled = config.ObInterstitialHigh == 1
            AdsHelper.obInterstitialEnabled = config.ObInterstitialAll == 1


            AdsHelper.featureNative1Enabled = config.FeatureNativeAll1 == 1
            AdsHelper.featureNativeHigh1Enabled = config.FeatureNativeHigh1 == 1



            AdsHelper.obNativeFormat = config.NativeFormat
            // CTA
            AdsHelper.obCtaColor = config.AdCtaColor
            AdsHelper.obCtaTextColor = config.AdCtaTextColor
            AdsHelper.obCtaTextStyle = config.CtaTextStyle
            AdsHelper.isShowNativeFullCross = config.ShowNativeFullCross
            AdsHelper.nativeFullCrossDelay = config.NativeFullCrossDelay

            Log.e("ObInterstitial", "Remote config loaded: $config")
        } catch (e: Exception) {
            Log.e("AdsManager", "JSON parse error", e)
        }
    }

}