package com.aiface.aging

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustEvent
import com.aiface.aging.shared.DataStoreManager
import com.aiface.aging.shared.IS_LANGUAGE
import com.aiface.aging.shared.IS_ONBOARD
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.AppOpenManager
import com.aiface.aging.shared.applyWindowInsets
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.databinding.ActivityMainBinding
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.AiFaceApp.Companion.fromHome
import com.aiface.aging.features.home.HomeFragment.Companion.requestPermission
import com.aiface.aging.features.main.MainFragment
import com.aiface.aging.features.result.ResultFeatureNavigator
import com.aiface.aging.features.noti.DailyNotificationWorker
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.features.noti.ExitNotification.onPauseNotification
import com.aiface.aging.features.noti.FullIntentPermissionBottomsheet
import com.aiface.aging.features.noti.LockNotificationWorker
import com.aiface.aging.shared.ads.canPresentHomeInterstitial
import com.aiface.aging.shared.ads.interstitialHome
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.shared.ads.FullscreenAdGate
import com.aiface.aging.shared.ads.MainFullscreenAdsPreloader
import com.aiface.aging.shared.safeNavigate
import com.aiface.aging.shared.showAppExitFlow
import com.aiface.aging.utils.AdjustConstant
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.OnboardSessionCounter
import com.aiface.aging.utils.permission.PermissionNavigator
import com.aiface.aging.utils.permission.PermissionStateChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val OVERLAY_PERMISSION_SHEET_TAG = "overlay_permission_sheet"

        private const val TAG = "MainActivity"
    }

    private var binding: ActivityMainBinding? = null
    private var navController: NavController? = null

    @Inject
    lateinit var dataStoreManager: DataStoreManager



        var hasOverlay=false


    private val permissionSettingsLauncher =

        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handlePermissionReturn()
        }
    private fun handlePermissionReturn() {
        AppOpenManager.suppressForSettings()
        hasOverlay = PermissionStateChecker.hasOverlayPermission(this)
        if (hasOverlay) {

        } else {

        }

        // UI refresh / analytics / next step
    }
    fun isNotiEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 13 → notification permission is always granted by default
            true
        }
    }

    private var returnToResultHostOnExit = false

    fun shouldReturnToResultHostOnExit(): Boolean = returnToResultHostOnExit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        returnToResultHostOnExit = ResultFeatureNavigator.isLaunchedFromResult(intent)
        setLocate(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        enableEdgeToEdge()
        applyLightSystemBars()
        hideNavigationBar()
        pinContentBehindHiddenNavBar()
//        applyWindowInsets()
        (application as AiFaceApp).appOpenAdManager.startPreloadAfterSplash()
        // Wait until splash inter fully gone — then mark Home+ and preload exactly 1 backup.
        FullscreenAdGate.runWhenAdsClear {
            com.aiface.aging.ads_nextgen.NextGenAdCheck.setScreen(
                com.aiface.aging.ads_nextgen.NextGenAdCheck.SCREEN_HOME,
            )
            MainFullscreenAdsPreloader.startFromMain()
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_main) as NavHostFragment
        navController = navHostFragment.navController
        handleResultDeepLinks()
        setupExitBackPress()

        fromHome=true
        dataStoreManager.writeDataStoreValue(IS_ONBOARD, true)
        OnboardSessionCounter.incrementCounter(this)
        SplashActivity.isSplash = false
        loadAdaptiveBannerAd(
            binding?.bannerAdView!!,
            this,
            BuildConfig.banner_home,
            BuildConfig.banner_home_high
        )
        val eventToken = AdjustConstant.ADJUST_HOME_TOKEN
        val adjustEvent = AdjustEvent(eventToken)
        Adjust.trackEvent(adjustEvent)
    }



    fun showHomeBannerAd() =
        if (AdsHelper.shouldShowAds()) binding?.clAd?.visibility = View.VISIBLE
        else {
            binding?.shimmer?.visibility = View.GONE
            binding?.clAd?.visibility = View.GONE
        }

    fun hideHomeBannerAd() = binding?.clAd?.visibility = View.GONE

    /**
     * Nav bar is hidden app-wide — never reserve bottom system-bar space under banner.
     */
    private fun pinContentBehindHiddenNavBar() {
        val root = binding?.root ?: return
        val listener = androidx.core.view.OnApplyWindowInsetsListener { v, insets ->
            if (v.paddingBottom != 0) {
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, 0)
            }
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    androidx.core.graphics.Insets.of(bars.left, bars.top, bars.right, 0)
                )
                .setInsets(
                    WindowInsetsCompat.Type.navigationBars(),
                    androidx.core.graphics.Insets.NONE
                )
                .build()
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, listener)
        binding?.clAd?.let { ViewCompat.setOnApplyWindowInsetsListener(it, listener) }
        ViewCompat.requestApplyInsets(root)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
            binding?.clAd?.let { ad ->
                if (ad.paddingBottom != 0) {
                    ad.setPadding(ad.paddingLeft, ad.paddingTop, ad.paddingRight, 0)
                }
                ad.requestLayout()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
        binding?.root?.let { root ->
            if (root.paddingBottom != 0) {
                root.setPadding(root.paddingLeft, root.paddingTop, root.paddingRight, 0)
            }
        }
    }

    /** AI Video and Magic Eraser are announced but not shipped yet. */
    fun showComingSoon(@StringRes featureNameRes: Int) {
        Toast.makeText(
            this,
            getString(R.string.coming_soon_feature, getString(featureNameRes)),
            Toast.LENGTH_SHORT,
        ).show()
    }
    private fun getMainFragment(): MainFragment? {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_main) as? NavHostFragment
        return navHost?.childFragmentManager?.fragments?.firstOrNull { it is MainFragment } as? MainFragment
    }

    private fun loadAdaptiveBannerAd(
        adContainer: FrameLayout,
        activity: FragmentActivity,
        normalAdId: String,
        highFloorAdId: String
    ) {
        try {
            // Premium ya offline → hide
            if (!AdsHelper.shouldShowAds() || !NetworkUtils.isOnline(activity)) {
                adContainer.visibility = View.GONE
                binding?.shimmer?.visibility = View.GONE
                binding?.clAd?.visibility = View.GONE
                return
            }

            if (AiFaceApp.isBannerHomeHf && AiFaceApp.isBannerHome) {
                binding?.clAd?.visibility = View.VISIBLE
                binding?.shimmer?.visibility = View.GONE
                AdsHelper.loadBanner(
                    activity = activity,
                    highFloorAdId = highFloorAdId,
                    normalAdId = normalAdId,
                    showHighFloor = true,
                    showNormalFloor = true,
                    onLoaded = {
                        binding?.clAd?.visibility = View.VISIBLE
                        LogUtils.printLog("home_banner loaded", highFloorAdId)
                    },
                    onAdFailed = {
                        adContainer.visibility = View.GONE
                        binding?.clAd?.visibility = View.GONE
                        LogUtils.printLog("home_banner failed to load", highFloorAdId)
                    },
                    adContainer = adContainer
                )
            } else if (AiFaceApp.isBannerHome) {
                binding?.clAd?.visibility = View.VISIBLE
                binding?.shimmer?.visibility = View.GONE
                AdsHelper.loadBannerAd(
                    activity = activity,
                    container = adContainer,
                    adId = normalAdId,
                    onLoaded = {
                        binding?.clAd?.visibility = View.VISIBLE
                        LogUtils.printLog("home_banner loaded", normalAdId)
                    },
                    onFailure = {
                        adContainer.visibility = View.GONE
                        binding?.clAd?.visibility = View.GONE
                        LogUtils.printLog("home_banner failed to load", normalAdId)
                    }
                )
            } else {
                binding?.clAd?.visibility = View.GONE
                binding?.shimmer?.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            adContainer.visibility = View.GONE
            binding?.shimmer?.visibility = View.GONE
            binding?.clAd?.visibility = View.GONE
        }
    }

    fun setLocate(activity: Activity) {
        var lang = Locale.getDefault().language //System Default Language
        dataStoreManager.readDataStoreValue(IS_LANGUAGE, "") {
            Log.e("Languageset", this.toString())
            val langnew = this
            if (langnew == "") {
                val supportedLangs = listOf(
                    "ja",
                    "es",
                    "in",
                    "hi",
                    "de",
                    "it",
                    "pt",
                    "ko",
                    "fr",
                    "ar",
                    "vi",
                    "ta",
                )

                // Check if the system language is in the list of supported languages, else default to English
                var lange = if (lang in supportedLangs) lang else "en"
                lang = lange
            } else {
                lang = langnew
            }
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            activity.baseContext.resources.updateConfiguration(
                config,
                activity.baseContext.resources.displayMetrics
            )
        }
    }


    private fun scheduleDailyNoti(){
        try {

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()

                var time = AiFaceApp.timePushDailyNoti
                if (time == 0) time = 10//10am
                set(Calendar.HOUR_OF_DAY, time)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            // If time passed today → schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val delay = calendar.timeInMillis - System.currentTimeMillis()

             val testDelay = 20_000L // 30 seconds in milliseconds


//                val workRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
//                    .setInitialDelay(testDelay, TimeUnit.MILLISECONDS)
//                    .build()
//                WorkManager.getInstance(this).enqueueUniqueWork(
//                    "lock-screen-noti",
//                    ExistingWorkPolicy.REPLACE,
//                    workRequest
//                )


            val workRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("lockscreen_periodic_am")
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "lockscreen_periodic_8am",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleLockScreenNoti(){
        try {

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()

                var time = AiFaceApp.timePushLockNoti
                if (time == 0) time = 14 //2pm
                set(Calendar.HOUR_OF_DAY, time)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            // If time passed today → schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val delay = calendar.timeInMillis - System.currentTimeMillis()

//             val testDelay = 20_000L // 30 seconds in milliseconds
//
//                val workRequest = OneTimeWorkRequestBuilder<LockNotificationWorker>()
//                    .setInitialDelay(testDelay, TimeUnit.MILLISECONDS)
//                    .build()
//                WorkManager.getInstance(this).enqueueUniqueWork(
//                    "lock-screen-noti",
//                    ExistingWorkPolicy.REPLACE,
//                    workRequest
//                )


            val workRequest = PeriodicWorkRequestBuilder<LockNotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("lockscreen_periodic_am")
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "lockscreen_periodic_8am",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (ResultFeatureNavigator.isLaunchedFromResult(intent)) {
            returnToResultHostOnExit = true
        }
        binding?.root?.post { handleResultDeepLinks() }
    }

    private fun setupExitBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
                if (GlobalLoader.isLoaderShowing) return
                try {
                    val nav = navController
                    if (nav != null && nav.previousBackStackEntry != null) {
                        nav.navigateUp()
                        return
                    }
                    showAppExitFlow()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun handleResultDeepLinks() {
        binding?.root?.post {
            if (intent.getBooleanExtra(ResultFeatureNavigator.EXTRA_OPEN_PREVIEW, false)) {
                val previewArgs = intent.getBundleExtra(ResultFeatureNavigator.EXTRA_PREVIEW_ARGS)
                intent.removeExtra(ResultFeatureNavigator.EXTRA_OPEN_PREVIEW)
                intent.removeExtra(ResultFeatureNavigator.EXTRA_PREVIEW_ARGS)
                if (previewArgs != null) {
                    navController?.navigate(R.id.previewFragment, previewArgs)
                }
            }
            // Dropped: collage / photo-edit deep links from result "try more".
            if (intent.getBooleanExtra(ResultFeatureNavigator.EXTRA_OPEN_COLLAGE, false)) {
                intent.removeExtra(ResultFeatureNavigator.EXTRA_OPEN_COLLAGE)
            }
            if (intent.getBooleanExtra(ResultFeatureNavigator.EXTRA_OPEN_PHOTO_EDIT, false)) {
                intent.removeExtra(ResultFeatureNavigator.EXTRA_OPEN_PHOTO_EDIT)
            }
        }
    }
}