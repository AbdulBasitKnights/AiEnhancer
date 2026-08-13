package com.aiface.aging

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.aiface.aging.ads_nextgen.AdsInitializer
import com.aiface.aging.ads_nextgen.AppOpenAdManager
import com.aiface.aging.data.initializer.RemoteInitializer
import com.aiface.aging.features.noti.FcmPushHelper
import com.aiface.aging.shared.ads.InterstitialAdGate
import com.aiface.aging.shared.ads.adsController.RemoteJsonConvertor.langJsonConvertor
import com.aiface.aging.shared.ads.adsController.RemoteJsonConvertor.obJsonConvertor
import com.aiface.aging.utils.ActivityTracker
import com.aiface.aging.utils.BitmapMemoryUtils
import com.aiface.aging.features.body.Constant
import com.aiface.aging.utils.AdjustConstant.ADJUST_TOKEN
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.NetworkDialogManager
import com.aiface.aging.utils.NetworkMonitor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltAndroidApp
class AiFaceApp : Application() {

    lateinit var adsInitializer: AdsInitializer
        private set

    @Volatile
    private var appOpenAdManagerInstance: AppOpenAdManager? = null

    /** Lazy — ProcessLifecycle / resume ads only after first real use (post-splash). */
    val appOpenAdManager: AppOpenAdManager
        get() {
            appOpenAdManagerInstance?.let { return it }
            synchronized(this) {
                appOpenAdManagerInstance?.let { return it }
                return AppOpenAdManager(this).also { appOpenAdManagerInstance = it }
            }
        }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "AiFaceApp"
        /** Delay so Splash first frame paints before Adjust/FB/FCM. */
        private const val DEFER_SECONDARY_SDK_MS = 400L
        @JvmField
        var isEditing = false
        @JvmField
        var editorName = ""
        @JvmField
        var tempImag = ""


        lateinit var context: Context

        fun getTheContext(): Context {
            return context
        }

        var dailyCredits = 5
        var weeklyCredits = 50

        var isConfigFetched = MutableLiveData(false)
        var onboardingSession = 1
        var showTimeSubScreenX = 3L
        var showProFromSplash = true
        var isNativeHomeHf = true
        var isNativeHome = true
        var bottom_nav_inter = false

        var isNativePreviewAi = true
        var isNativePreviewAiHf = true

        var isNativeSplash = true
        var isNativeSplashHf = true

        var isInterSplashHf = true
        var isInterSplash = true

        var isInterHomeHf = true
        var isInterHome = true

        var isBannerHomeHf = true
        var fromHome = false
        var isBannerHome = true

        var isBannerEdit = true
        var isBannerEditHf = true

        var isShowPermission = true

        var isNativeSurvey = true
        var isNativeSurveyHf = true

        var isInterSurvey = false
        var survey_enable = true
        var isInterSurveyHf = false

        var isInterCollageSaveHf = true
        var isInterCollageSave = true

        var isInterBodySaveHf = true
        var isInterBodySave = true

        var isNativeShare = false
        var exit_native = true
        var isNativeShareHf = true

        /** First-time Rate Us sheet on share/result screen. */
        var showRateUsOnShare = true

        var isNativeImgPicker = true
        var isNativeImgPickerHf = true

        var isNativePermission = true
        var isNativePermissionHf = true
        var isAppOpenResume = true

        var isInterEditSaveHf = true
        var isInterEditSave = true

        var isNativeSettingHf = true
        var isNativeSetting = true

        var isNativeCollageHf = true
        var isNativeCollage = true

        var timePushDailyNoti = 10

        var timePushLockNoti = 14
        var isDailyNoti = true

        var isLockNoti = true

        var isShortcut = true

        var nativeUninstall = true
        var nativeUninstallHf = true

        var nativeEditAi = true
        var nativeEditAiHf = true

        var nativePreview = true
        var nativePreviewHf = true

        var nativeSeeAll = true
        var nativeSeeAllHf = true

        var nativeResultHf = false
        var nativeResult = false

        var isInterEdit = true
        var isInterEditHf = true
        var langSetting = false

        var isInterPicker = false
        var isInterPickerHf = false

        var isNativeHomeChild = true
        var isNativeHomeChildHf = true

        var isRewardHome = true
        var isRewardHomeHf = true
        var showRewardDialog = true

        var isSolidObButton = false

        var isInterBgRemoverHf = true
        var isInterBgRemover = true

        var isRewardPrompt = true
        var isRewardPromptHf = true

        var isInterPromptHf = true
        var isInterPrompt = true

        // FaceSwap ad flags (default off — FORCE_PRO_NO_ADS / no-ads path)
        var interGenerateFaceswap = false
        var interGenerateFaceswapHf = false
        var nativeResultFaceSwap = false
        var nativeResultFaceSwapHf = false
        var nativePreviewFaceswap = false
        var nativePreviewFaceswapHf = false
        var isRewardFaceSwapGenerate = false
        var isRewardFaceSwapGenerateHf = false
        var nativeGenerateFaceswap = false
        var nativeGenerateFaceswapHf = false
    }

    val environment = if (BuildConfig.DEBUG) {
        AdjustConfig.ENVIRONMENT_SANDBOX
    } else {
        AdjustConfig.ENVIRONMENT_PRODUCTION
    }

    private val networkListener: (Boolean) -> Unit = { isConnected ->
        Handler(Looper.getMainLooper()).post {
            val activity = ActivityTracker.currentActivity

            Log.d("networkChecker", "isConnected=$isConnected")
            Log.d("networkChecker", "activity=$activity")
            Log.d("networkChecker", "isFinishing=${activity?.isFinishing}")
            Log.d("networkChecker", "isDestroyed=${activity?.isDestroyed}")

            if (isConnected) {
                NetworkDialogManager.dismissDialog()
                activity?.let { activity ->
                    //  NetworkDialogManager.restartApp(activity)
                }
                Log.d("networkChecker", "Connected")
            } else {
                NetworkDialogManager.dismissDialog()
                activity?.let {
                    Log.d("networkChecker", "Showing dialog on ${it::class.java.simpleName}")
                    NetworkDialogManager.showNoInternetDialog(it)
                }
                Log.d("networkChecker", "Disconnected")
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate()
        context = applicationContext

        // Local RC defaults only (SharedPrefs) — no network / no Mobile Ads.
        runCatching { RemoteInitializer.init(this) }

        // Construct only — MobileAds.initialize runs after UMP consent on Splash.
        adsInitializer = AdsInitializer(this)
        registerActivityLifecycleCallbacks(ActivityTracker)

        // Minimal Firebase so Crashlytics/Analytics work; FCM deferred.
        runCatching {
            FirebaseApp.initializeApp(this)
            FirebaseLogUtils.initFirebaseAnalytics(this)
        }

        // Post-frame: Remote Config + secondary SDKs (keeps cold start short).
        mainHandler.post {
            fetchRemoteConfig()
            mainHandler.postDelayed(::initSecondarySdks, DEFER_SECONDARY_SDK_MS)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                BitmapMemoryUtils.trimImageCaches(this)
                Constant.releaseStaticBitmaps()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                BitmapMemoryUtils.trimImageCaches(this)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        BitmapMemoryUtils.trimImageCaches(this)
        Constant.releaseStaticBitmaps()
        BitmapMemoryUtils.trimImageCachesAsync(this, includeDisk = true)
    }

    /** Facebook, Adjust, FCM — not on critical cold-start path. */
    private fun initSecondarySdks() {
        runCatching { AppEventsLogger.activateApp(this) }
            .onFailure { Log.w(TAG, "Facebook activateApp failed", it) }
        runCatching { initializeAdjust() }
            .onFailure { Log.w(TAG, "Adjust init failed", it) }
        appScope.launch(Dispatchers.IO) {
            runCatching { FcmPushHelper.init(this@AiFaceApp) }
                .onFailure { Log.w(TAG, "FCM init failed", it) }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        NetworkMonitor.removeListener(networkListener)
    }


    private fun fetchRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Missing RC keys return false — apply defaults first, then fetch.
        remoteConfig.setDefaultsAsync(
            mapOf(
                "native_setting" to true,
                "native_setting_hf" to true,
                "native_share_screen" to true,
                "native_share_screen_hf" to true,
                "rate_us_share" to true,
                "native_home" to true,
                "native_home_hf" to true,
                "native_edit_ai" to true,
                "native_edit_ai_hf" to true,
                "native_result" to true,
                "native_result_hf" to true,
                "native_preview" to true,
                "native_preview_hf" to true,
                "native_see_all" to true,
                "native_see_all_hf" to true,
                "native_img_picker" to true,
                "native_img_picker_hf" to true,
                "banner_home" to true,
                "banner_home_high" to true,
            ),
        ).addOnCompleteListener {
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    applyRemoteConfigValues(remoteConfig, task.isSuccessful)
                }
        }
    }

    /**
     * Apply RC off main thread. Always mark [isConfigFetched] true so Splash never waits
     * forever on network failure (was a cold-start / ANR-adjacent hang).
     */
    private fun applyRemoteConfigValues(
        remoteConfig: com.google.firebase.remoteconfig.FirebaseRemoteConfig,
        fetchSuccessful: Boolean,
    ) {
        appScope.launch {
            try {
                if (fetchSuccessful) {
                    runCatching { RemoteInitializer.syncWithRemoteConfig(remoteConfig) }

                    val langJson = remoteConfig.getString("language_ads")
                    langJsonConvertor(langJson)
                    val onboardingJson = remoteConfig.getString("onboarding_ads")
//                    obJsonConvertor(onboardingJson)

                    onboardingSession = remoteConfig.getLong("ob_session").toInt()

                    isNativeSplash = remoteConfig.getBoolean("native_splash")
                    isNativeSplashHf = remoteConfig.getBoolean("native_splash_high")

                    isNativeHomeHf = remoteConfig.getBoolean("native_home_hf")
                    isNativeHome = remoteConfig.getBoolean("native_home")
                    bottom_nav_inter = remoteConfig.getBoolean("bottom_nav_inter")

                    showProFromSplash = remoteConfig.getBoolean("show_pro_from_splash")
                    showTimeSubScreenX = remoteConfig.getLong("time_show_sub_close_btn")

                    isInterSplash = remoteConfig.getBoolean("inter_splash")
                    isInterSplashHf = remoteConfig.getBoolean("inter_splash_high")

                    isShortcut = remoteConfig.getBoolean("is_shortcut")
                    Log.w("remoteConfigFetch", "isNativeHomeHf:$isNativeHomeHf")
                    Log.w("remoteConfigFetch", "isNativeHome:$isNativeHome")
                    Log.w("remoteConfigFetch", "showProFromSplash:$showProFromSplash")
                    Log.w("remoteConfigFetch", "onboardingSession:$onboardingSession")

                    isInterHome = remoteConfig.getBoolean("inter_home")
                    isInterHomeHf = remoteConfig.getBoolean("inter_home_high")

                    isNativePreviewAi = remoteConfig.getBoolean("native_preview_ai")
                    isNativePreviewAiHf = remoteConfig.getBoolean("native_preview_ai_hf")

                    dailyCredits = remoteConfig.getDouble("daily_credits").toInt()
                    weeklyCredits = remoteConfig.getDouble("weekly_credits").toInt()

                    isNativePermission = remoteConfig.getBoolean("permission_native")
                    isNativePermissionHf = remoteConfig.getBoolean("permission_native_hf")

                    isAppOpenResume = remoteConfig.getBoolean("appopen_resume")

                    InterstitialAdGate.applyRemoteCooldownSeconds(
                        remoteConfig.getLong(InterstitialAdGate.REMOTE_KEY),
                    )

                    isNativeSurvey = remoteConfig.getBoolean("native_survey")
                    isNativeSurveyHf = remoteConfig.getBoolean("native_survey_hf")

                    isInterSurvey = remoteConfig.getBoolean("inter_survey")
                    survey_enable = remoteConfig.getBoolean("survey_enable")
                    isInterSurveyHf = remoteConfig.getBoolean("inter_survey_hf")

                    isBannerHomeHf = remoteConfig.getBoolean("banner_home_high")
                    isBannerHome = remoteConfig.getBoolean("banner_home")

                    isShowPermission = remoteConfig.getBoolean("show_permission_screen")

                    isInterCollageSave = remoteConfig.getBoolean("inter_collage_save")
                    isInterCollageSaveHf = remoteConfig.getBoolean("inter_collage_save_high")

                    isInterBodySaveHf = remoteConfig.getBoolean("inter_body_save_high")
                    isInterBodySave = remoteConfig.getBoolean("inter_body_save")

                    isNativeShare = remoteConfig.getBoolean("native_share_screen")
                    exit_native = remoteConfig.getBoolean("exit_native")
                    isNativeShareHf = remoteConfig.getBoolean("native_share_screen_hf")
                    showRateUsOnShare = remoteConfig.getBoolean("rate_us_share")

                    isNativeImgPickerHf = remoteConfig.getBoolean("native_img_picker_hf")
                    isNativeImgPicker = remoteConfig.getBoolean("native_img_picker")

                    isInterEditSaveHf = remoteConfig.getBoolean("inter_edit_save_hf")
                    isInterEditSave = remoteConfig.getBoolean("inter_edit_save")

                    isNativeSettingHf = remoteConfig.getBoolean("native_setting_hf")
                    isNativeSetting = remoteConfig.getBoolean("native_setting")
                    Log.w(
                        "remoteConfigFetch",
                        "isNativeSetting=$isNativeSetting isNativeSettingHf=$isNativeSettingHf",
                    )

                    isNativeCollageHf = remoteConfig.getBoolean("native_collage_hf")
                    isNativeCollage = remoteConfig.getBoolean("native_collage")

                    timePushLockNoti = remoteConfig.getDouble("time_push_lock_noti").toInt()
                    isLockNoti = remoteConfig.getBoolean("lock_noti")

                    timePushDailyNoti = remoteConfig.getDouble("time_push_daily_noti").toInt()
                    isDailyNoti = remoteConfig.getBoolean("daily_noti")

                    nativeUninstall = remoteConfig.getBoolean("native_uninstall")
                    nativeUninstallHf = remoteConfig.getBoolean("native_uninstall_hf")

                    nativePreviewHf = remoteConfig.getBoolean("native_preview_hf")
                    nativePreview = remoteConfig.getBoolean("native_preview")

                    isBannerEditHf = remoteConfig.getBoolean("banner_edit_hf")
                    isBannerEdit = remoteConfig.getBoolean("banner_edit")

                    nativeEditAi = remoteConfig.getBoolean("native_edit_ai")
                    nativeEditAiHf = remoteConfig.getBoolean("native_edit_ai_hf")

                    nativeSeeAllHf = remoteConfig.getBoolean("native_see_all_hf")
                    nativeSeeAll = remoteConfig.getBoolean("native_see_all")

                    nativeResultHf = remoteConfig.getBoolean("native_result_hf")
                    nativeResult = remoteConfig.getBoolean("native_result")

                    isInterEdit = remoteConfig.getBoolean("inter_edit")
                    isInterEditHf = remoteConfig.getBoolean("inter_edit_hf")

                    isInterPicker = remoteConfig.getBoolean("inter_picker")
                    isInterPickerHf = remoteConfig.getBoolean("inter_picker_hf")

                    isNativeHomeChild = remoteConfig.getBoolean("native_home_child")
                    isNativeHomeChildHf = remoteConfig.getBoolean("native_home_child_hf")

                    isRewardHome = remoteConfig.getBoolean("reward_home")
                    isRewardHomeHf = remoteConfig.getBoolean("reward_home_hf")
                    showRewardDialog = remoteConfig.getBoolean("show_reward_dialog")

                    isSolidObButton = remoteConfig.getBoolean("solid_ob_button")

                    isInterBgRemover = remoteConfig.getBoolean("inter_bg_remover")
                    isInterBgRemoverHf = remoteConfig.getBoolean("inter_bg_remover_hf")

                    isRewardPromptHf = remoteConfig.getBoolean("reward_edit_hf")
                    isRewardPrompt = remoteConfig.getBoolean("reward_edit")

                    isInterPromptHf = remoteConfig.getBoolean("inter_prompt_hf")
                    isInterPrompt = remoteConfig.getBoolean("inter_prompt")
                } else {
                    Log.e("remoteConfigFetch", "error — using defaults where set")
                    isNativeSetting = remoteConfig.getBoolean("native_setting")
                    isNativeSettingHf = remoteConfig.getBoolean("native_setting_hf")
                }
            } catch (t: Throwable) {
                Log.e("remoteConfigFetch", "applyRemoteConfigValues failed", t)
            } finally {
                // ALWAYS unblock Splash — success or fail.
                withContext(Dispatchers.Main) {
                    isConfigFetched.value = true
                }
            }
        }
    }

    private fun initializeAdjust() {
        val config = AdjustConfig(this, ADJUST_TOKEN, environment)

        // 🔸 Recommended: verbose logs for debugging
        config.setLogLevel(LogLevel.VERBOSE)

        // Optional listeners for debugging / analytics
        config.setOnEventTrackingSucceededListener {
            Log.d("ADJUST_Config", "✅ Event tracked: $it")
            // printDebugLog("✅ Event tracked: $it")
        }
        config.setOnSessionTrackingSucceededListener {
            Log.d("ADJUST_Config", "✅ Session tracked: $it")
        }
        config.setOnEventTrackingFailedListener {
            Log.d("ADJUST_Config", "❌ Event failed: $it")
        }
// Allow to send in the background.
        config.enableSendingInBackground()
        // Initialize Adjust SDK
        Adjust.initSdk(config)
        // Register lifecycle callbacks so Adjust handles onResume/onPause automatically
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = Adjust.onResume()
            override fun onActivityPaused(activity: Activity) = Adjust.onPause()
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
        Log.d("ADJUST_Config", "🔥 Adjust initialized with token: $ADJUST_TOKEN")


    }
}