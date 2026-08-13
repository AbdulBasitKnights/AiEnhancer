package com.aiface.aging.data.initializer

import com.aiface.aging.data.params.RemoteKeys
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class RemoteAdsConfiguration private constructor() : BaseRemoteConfiguration() {

    companion object {
        private const val PREFS_NAME = "remote_ads_config"

        @Volatile
        private var instance: RemoteAdsConfiguration? = null

        fun getInstance(): RemoteAdsConfiguration {
            return instance ?: synchronized(this) {
                instance ?: RemoteAdsConfiguration().also { instance = it }
            }
        }
    }

    override fun getPrefsName(): String = PREFS_NAME

    override fun sync(remoteConfig: FirebaseRemoteConfig) {
        with(remoteConfig) {
            saveToLocal(EnableBannerHome)
            saveToLocal(bannerSplash)
            saveToLocal(enableAppResume)
            saveToLocal(hfInterSplash)
            saveToLocal(hfNativeLang1)
            saveToLocal(hfNativeLang2)
            saveToLocal(hfNativeOnboarding1)
            saveToLocal(interSplash)
            saveToLocal(nativeLang1)
            saveToLocal(nativeLang2)
            saveToLocal(nativeOnboarding1)
            saveToLocal(nativeOnboarding3)
            saveToLocal(fohfnativefullscr1)
            saveToLocal(fohfnativefullscr2)
            saveToLocal(fonativefullscr1)
            saveToLocal(fonativefullscr2)
        }
    }
    // ---------------- BooleanKeys ----------------
    private object EnableBannerHome: RemoteKeys.BooleanKey("banner_all", true)
    private object bannerSplash: RemoteKeys.BooleanKey("fo_banner_splash", true)
    private object enableAppResume: RemoteKeys.BooleanKey("fo_enable_app_resume", true)
    private object hfInterSplash: RemoteKeys.BooleanKey("fo_hf_inter_splash", true)
    private object hfNativeLang1: RemoteKeys.BooleanKey("fo_hf_native_language_1", true)
    private object hfNativeLang2: RemoteKeys.BooleanKey("fo_hf_native_language_2", true)
    private object hfNativeOnboarding1: RemoteKeys.BooleanKey("fo_hf_native_onboarding1", true)
    private object interSplash: RemoteKeys.BooleanKey("fo_inter_splash", true)
    private object nativeLang1: RemoteKeys.BooleanKey("fo_native_language_1", true)
    private object nativeLang2: RemoteKeys.BooleanKey("fo_native_language_2", true)
    private object nativeOnboarding1: RemoteKeys.BooleanKey("fo_native_onboarding1", true)
    private object nativeOnboarding3: RemoteKeys.BooleanKey("fo_native_onboarding3", true)
    private object fohfnativefullscr1: RemoteKeys.BooleanKey("fo_hf_native_full_scr1", true)
    private object fohfnativefullscr2: RemoteKeys.BooleanKey("fo_hf_native_full_scr2", true)
    private object fonativefullscr1: RemoteKeys.BooleanKey("fo_native_full_scr1", true)
    private object fonativefullscr2: RemoteKeys.BooleanKey("fo_native_full_scr2", true)

    // ---------------- Getters ----------------
    val isBannerHomeEnabled: Boolean get() = EnableBannerHome.get()
    val isSplashBannerEnabled: Boolean get() = bannerSplash.get()
    val isAppResumeEnabled: Boolean get() = enableAppResume.get()
    val isHFInterstitialSplashEnabled: Boolean get() = hfInterSplash.get()
    val isHFNativeLanguage1Enabled: Boolean get() = hfNativeLang1.get()
    val isHFNativeLanguage2Enabled: Boolean get() = hfNativeLang2.get()
    val isHFNativeOnboarding1Enabled: Boolean get() = hfNativeOnboarding1.get()
    val isInterstitialSplashEnabled: Boolean get() = interSplash.get()
    val isNativeLanguage1Enabled: Boolean get() = nativeLang1.get()
    val isNativeLanguage2Enabled: Boolean get() = nativeLang2.get()
    val isNativeOnboarding1Enabled: Boolean get() = nativeOnboarding1.get()
    val isNativeOnboarding3Enabled: Boolean get() = nativeOnboarding3.get()

    val fo_hf_native_full_scr1: Boolean get() = fohfnativefullscr1.get()
    val fo_hf_native_full_scr2: Boolean get() = fohfnativefullscr2.get()
    val fo_native_full_scr1: Boolean get() = fonativefullscr1.get()
    val fo_native_full_scr2: Boolean get() = fonativefullscr2.get()
}
