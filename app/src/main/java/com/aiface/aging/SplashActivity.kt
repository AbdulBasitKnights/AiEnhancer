package com.aiface.aging

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.aiface.aging.AiFaceApp.Companion.isConfigFetched
import com.aiface.aging.AiFaceApp.Companion.survey_enable
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.AdUiState
import com.aiface.aging.ads_nextgen.AdsInitializer
import com.aiface.aging.ads_nextgen.AdsManager
import com.aiface.aging.ads_nextgen.BannerSizeHelper
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.NextGenAdCheck
import com.aiface.aging.core.consent_controler.ConsentCallback
import com.aiface.aging.core.consent_controler.ConsentController
import com.aiface.aging.data.initializer.RemoteAdsConfiguration
import com.aiface.aging.databinding.ActivitySplashBinding
import com.aiface.aging.features.fullonboard.FullOnboardActivity
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.features.iap.IapManager
import com.aiface.aging.features.iap.utils.DataWrappers
import com.aiface.aging.features.iap.utils.IapConnector
import com.aiface.aging.features.iap.utils.SubscriptionServiceListener
import com.aiface.aging.features.language.LanguageActivity
import com.aiface.aging.features.splash.RegisterState
import com.aiface.aging.features.splash.SplashViewModel
import com.aiface.aging.features.survey.SurveyActivity
import com.aiface.aging.features.uninstall.UninstallActivity
import com.aiface.aging.domain.repository.DailyCheckInRepository
import com.aiface.aging.shared.CreditManager
import com.aiface.aging.shared.DataStoreManager
import com.aiface.aging.shared.IS_LANGUAGE
import com.aiface.aging.shared.IS_LANGUAGE_SPLASH
import com.aiface.aging.shared.IS_ONBOARD
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.OnboardSessionCounter
import com.aiface.aging.utils.ShortcutUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Splash ad flow mirrors ProCapture-Studio (shimmer bottom + inter early-continue).
 *
 * Startup (this app):
 * 1. Notification permission (allow / deny)
 * 2. Progress loader
 * 3. initIAP → pro true/false (3s timeout → false)
 * 4. initConsent → ads init (before any ad request)
 * 5. [onRemoteConfigReady] → banner/native shimmer + inter
 * 6. Inter: GlobalLoader 1s → show → Idle → navigate under ad
 *
 * Language native loads in LanguageActivity (not splash).
 * UI assets stay Face Aging; ad timing/shimmer match ProCapture.
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private enum class SplashAdPath {
        INTER_NATIVE,
        INTER_BANNER,
        INTER_ONLY,
        NATIVE_ONLY,
        NONE,
    }

    private var binding: ActivitySplashBinding? = null

    lateinit var adsInitializer: AdsInitializer
        private set
    private val viewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var dailyCheckInRepository: DailyCheckInRepository

    private lateinit var creditManager: CreditManager
    private lateinit var iapConnector: IapConnector

    private var bannerAdView: AdView? = null
    private var splashAdPath: SplashAdPath = SplashAdPath.NONE

    private var bottomSettled = false
    private var interReady = false
    private var interFailed = false
    private var interShowStarted = false
    private var splashAdsFinished = false
    private var splashFlowStarted = false

    private var splashInterRequested = false
    private var splashNativeRequested = false
    private var splashBannerRequested = false
    private var splashAdsLocked = false

    private var bottomTimeoutJob: Job? = null
    private var bottomDisplayJob: Job? = null
    private var interWaitJob: Job? = null
    private var splashHardTimeoutJob: Job? = null
    private var bannerStartJob: Job? = null
    private var registrationRetryJob: Job? = null

    private var hasNavigated = false
    private var pendingNavigation = false

    private var notificationPermissionContinuation: ((Boolean) -> Unit)? = null

    private fun isSplashRequestBlocked(): Boolean =
        splashAdsLocked || splashAdsFinished || hasNavigated || interShowStarted

    private fun lockSplashAfterShowCommit() {
        splashAdsLocked = true
        bottomTimeoutJob?.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NextGenAdCheck.setScreen(NextGenAdCheck.SCREEN_SPLASH)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setLocate(this)
        setContentView(binding?.root)
        adsInitializer = (application as AiFaceApp).adsInitializer
        applyLightSystemBars()
        hideNavigationBar()
        FirebaseLogUtils.logEvent("splash_view", "")

        isSplash = true
        dataStoreManager.readDataStoreValue(IS_ONBOARD, false) {
            isOnboard = !this
        }
        dataStoreManager.readDataStoreValue(IS_LANGUAGE_SPLASH, false) {
            isLanguage = !this
        }

        creditManager = CreditManager(this@SplashActivity)
        observeRegistrationState()
        // Absolute ceiling — never stay on splash longer than this (ads/auth/consent).

        lifecycleScope.launch {
            try {
                // 1) Notification first — allow or deny, then continue.
                awaitNotificationPermission()

                // 2) Progress loader (cosmetic — does not block).
                showProgressBar(PROGRESS_UI_MS)
                startSplashAbsoluteTimeout()
                // 3) IAP purchase restore → pro flag (timeout → false).
                resolveProStatusWithTimeout()
                binding?.containAds?.visibility =
                    if (isProVersion.value == true) View.INVISIBLE else View.VISIBLE

                if (!NetworkUtils.isOnline(this@SplashActivity)) {
                    hideBottomAdSlot()
                    splashAdsFinished = true
                    navigateNext()
                    return@launch
                }

                // Auth can run while consent / RC proceed.
                viewModel.ensureAccessToken()

                // 4) Consent BEFORE any ad request.
                initConsent()
            } catch (e: Exception) {
                Log.e(TAG, "Splash startup failed", e)
                splashAdsFinished = true
                navigateNext()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
    }

    // ── Consent + Remote Config ─────────────────────────────────────────────

    private fun initConsent() {
        runCatching {
            ConsentController(this).apply {
                initConsent("3E0C66FC65F12F1C83DC8561646DF22B", object : ConsentCallback {
                    override fun onAdsLoad(canRequestAd: Boolean) {
                        adsInitializer.initialize {
                            runOnUiThread {
                                if (isFinishing || isDestroyed) return@runOnUiThread
                                isConfigFetched.observe(this@SplashActivity, Observer { ready ->
                                    if (!ready) return@Observer
                                    onRemoteConfigReady()
                                })
                                // If RC already failed/succeeded before observe, LiveData delivers current.
                                // Extra safety: if still false after soft wait, proceed with defaults.
                                lifecycleScope.launch {
                                    delay(RC_READY_FALLBACK_MS)
                                    if (!splashFlowStarted && !hasNavigated) {
                                        Log.w(TAG, "RC ready fallback — proceed with defaults")
                                        AiFaceApp.isConfigFetched.value = true
                                    }
                                }
                            }
                        }
                    }

                    override fun onConsentFormLoaded() {
                        runCatching { this@apply.showConsentForm() }
                    }

                    override fun onConsentFormDismissed() = Unit
                    override fun onPolicyStatus(required: Boolean) = Unit
                })
            }
        }.onFailure { e ->
            Log.e(TAG, "initConsent failed — continue without ads gate", e)
            adsInitializer.initialize {
                runOnUiThread {
                    AiFaceApp.isConfigFetched.value = true
                    onRemoteConfigReady()
                }
            }
        }
    }

    private fun onRemoteConfigReady() {
        if (splashFlowStarted) return
        splashFlowStarted = true

        val onboardingSession = AiFaceApp.onboardingSession
        if (onboardingSession == 2 || onboardingSession == 3) {
            val counts = OnboardSessionCounter.getCounter(this)
            if (counts <= onboardingSession) {
                isOnboard = true
                dataStoreManager.writeDataStoreValue(IS_ONBOARD, false)
            }
        }

        if (AiFaceApp.isShortcut) {
            ShortcutUtils.createDynamicShortcut(this)
        } else {
            ShortcutUtils.removeAllShortcuts(this)
        }

        lifecycleScope.launch {
            delay(RC_SETTLE_MS)
            if (!hasNavigated) startSplashAdFlow()
        }
    }

    // ── IAP / Pro ───────────────────────────────────────────────────────────

    private fun initIAP() {
        iapConnector = IapManager.getIapConnector(applicationContext)
        iapConnector.addSubscriptionListener(object : SubscriptionServiceListener {
            override fun onSubscriptionRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
                val isPro = purchaseInfo.sku == IapManager.skuKeyWeek
                AdsHelper.updateProVersion(isPro)
                lifecycleScope.launch(Dispatchers.IO) {
                    if (isPro) {
                        creditManager.claimWeeklyPremiumCredits()
                    } else {
                        dailyCheckInRepository.syncWithServer()
                    }
                }
            }

            override fun onSubscriptionPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
                when (purchaseInfo.sku) {
                    IapManager.skuKeyWeek -> AdsHelper.updateProVersion(true)
                }
            }

            override fun onPricesUpdated(iapKeyPrices: Map<String, List<DataWrappers.ProductDetails>>) = Unit
        })
    }

    /**
     * Query owned purchases. Subscribed → pro true; otherwise false.
     * If billing returns nothing within [IAP_PRO_TIMEOUT_MS] → false.
     */
    private suspend fun resolveProStatusWithTimeout() {
        initIAP()
        val isPro = withTimeoutOrNull(IAP_PRO_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                iapConnector.checkProStatus { subscribed ->
                    if (continuation.isActive) {
                        continuation.resume(subscribed)
                    }
                }
            }
        } ?: false

        AdsHelper.updateProVersion(isPro)
        Log.d(TAG, "Pro status resolved isPro=$isPro (timeout=${IAP_PRO_TIMEOUT_MS}ms)")
        lifecycleScope.launch(Dispatchers.IO) {
            if (isPro) {
                creditManager.claimWeeklyPremiumCredits()
            } else if (AdsHelper.shouldShowAds()) {
                dailyCheckInRepository.syncWithServer()
            }
        }
    }

    // ── Splash ads ──────────────────────────────────────────────────────────

    private fun resolveSplashAdPath(): SplashAdPath {
        if (!AdsHelper.shouldShowAds() || isProVersion.value == true) {
            return SplashAdPath.NONE
        }
        val inter = AiFaceApp.isInterSplash
        val native = AiFaceApp.isNativeSplash
        val banner = RemoteAdsConfiguration.getInstance().isSplashBannerEnabled

        return when {
            inter && banner -> SplashAdPath.INTER_BANNER
            inter && native -> SplashAdPath.INTER_NATIVE
            inter -> SplashAdPath.INTER_ONLY
            native -> SplashAdPath.NATIVE_ONLY
            else -> SplashAdPath.NONE
        }
    }

    private fun startSplashAdFlow() {
        splashAdPath = resolveSplashAdPath()
        Log.d(
            TAG,
            "Splash ad path=$splashAdPath inter=${AiFaceApp.isInterSplash} " +
                "native=${AiFaceApp.isNativeSplash} " +
                "banner=${RemoteAdsConfiguration.getInstance().isSplashBannerEnabled}",
        )

        when (splashAdPath) {
            SplashAdPath.NONE -> {
                hideBottomAdSlot()
                splashAdsFinished = true
                splashAdsLocked = true
                navigateNext()
            }
            SplashAdPath.INTER_ONLY -> {
                hideBottomAdSlot()
                beginInterOnlyFlow()
            }
            SplashAdPath.NATIVE_ONLY -> {
                showBottomAdSlot()
                beginNativeOnlyFlow()
            }
            SplashAdPath.INTER_NATIVE -> {
                showBottomAdSlot()
                adsInitializer.runWhenInitialized {
                    runOnUiThread { beginParallelBottomAndInter(SplashAdPath.INTER_NATIVE) }
                }
            }
            SplashAdPath.INTER_BANNER -> {
                showBottomAdSlot()
                adsInitializer.runWhenInitialized {
                    runOnUiThread { beginParallelBottomAndInter(SplashAdPath.INTER_BANNER) }
                }
            }
        }
    }

    /**
     * Absolute max time on splash. Survives ads finished + auth wait.
     * Always forces leave if still here.
     */
    private fun startSplashAbsoluteTimeout() {
        splashHardTimeoutJob?.cancel()
        splashHardTimeoutJob = lifecycleScope.launch {
            delay(SPLASH_MAX_WAIT_MS)
            if (hasNavigated || isFinishing || isDestroyed) return@launch
            Log.w(TAG, "splash absolute timeout ${SPLASH_MAX_WAIT_MS}ms — force leave")
            FirebaseLogUtils.logEvent("splash_timeout", "")
            forceLeaveSplash()
        }
    }

    /** Bypass ads/auth gates — leave splash now. */
    private fun forceLeaveSplash() {
        if (hasNavigated || isFinishing || isDestroyed) return
        splashAdsFinished = true
        splashAdsLocked = true
        interFailed = true
        interShowStarted = true
        cancelAdJobs()
        registrationRetryJob?.cancel()
        splashHardTimeoutJob?.cancel()
        GlobalLoader.hide(this)
        try {
            seedCreditsAfterSplash()
            hasNavigated = true
            startNextActivityIntent()
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "forceLeaveSplash failed", e)
            hasNavigated = true
            finish()
        }
    }

    /**
     * Parallel inter + bottom (ProCapture / AVD style).
     * Inter shows as soon as ready — bottom does not gate show.
     */
    private fun beginParallelBottomAndInter(path: SplashAdPath) {
        loadSplashInterstitial()
        startInterWaitLoop()

        when (path) {
            SplashAdPath.INTER_NATIVE -> loadSplashNativeBottom()
            SplashAdPath.INTER_BANNER -> {
                // Delay banner ~1s so inter request gets first look at network.
                bannerStartJob?.cancel()
                bannerStartJob = lifecycleScope.launch {
                    delay(BANNER_START_DELAY_MS)
                    if (!hasNavigated && !isFinishing && !isDestroyed) {
                        loadSplashBannerBottom()
                    }
                }
            }
            else -> Unit
        }

        bottomTimeoutJob = lifecycleScope.launch {
            delay(BOTTOM_AD_TIMEOUT_MS)
            onBottomTimeout()
        }
    }

    private fun beginInterOnlyFlow() {
        adsInitializer.runWhenInitialized {
            runOnUiThread {
                loadSplashInterstitial()
                startInterWaitLoop()
            }
        }
    }

    private fun beginNativeOnlyFlow() {
        adsInitializer.runWhenInitialized {
            runOnUiThread {
                loadSplashNativeBottom()
                bottomTimeoutJob = lifecycleScope.launch {
                    delay(BOTTOM_AD_TIMEOUT_MS)
                    onBottomTimeout()
                }
            }
        }
    }

    private fun startInterWaitLoop() {
        if (interShowStarted || splashAdsFinished || hasNavigated) return
        interWaitJob?.cancel()
        interWaitJob = lifecycleScope.launch {
            val deadline = System.currentTimeMillis() + INTER_MAX_WAIT_MS
            while (!interReady && !interFailed && !hasNavigated && !splashAdsFinished) {
                if (System.currentTimeMillis() >= deadline) {
                    Log.w(TAG, "inter max wait — navigate without ad")
                    FirebaseLogUtils.logEvent("splash_inter_timeout", "")
                    interFailed = true
                    break
                }
                delay(100)
            }
            if (hasNavigated || splashAdsFinished || interShowStarted) return@launch
            if (interReady) {
                tryShowInterstitialNow()
            } else {
                finishSplashAds()
            }
        }
    }

    /** Show inter immediately when loaded (1s GlobalLoader — ProCapture / AVD). */
    private fun tryShowInterstitialNow() {
        if (interShowStarted || hasNavigated || splashAdsFinished || isFinishing || isDestroyed) return
        if (!interReady || interFailed) return

        interShowStarted = true
        lockSplashAfterShowCommit()
        interWaitJob?.cancel()

        interWaitJob = lifecycleScope.launch {
            GlobalLoader.show(this@SplashActivity)
            delay(INTER_LOADER_MS)
            GlobalLoader.hide(this@SplashActivity)
            if (hasNavigated || splashAdsFinished) return@launch
            if (interReady && !interFailed) {
                showSplashInterstitial()
            } else {
                finishSplashAds()
            }
        }
    }

    /**
     * Show interstitial while Splash is foreground.
     * Idle = early continue → [finishSplashAds] → navigate under the ad (ProCapture).
     */
    private fun showSplashInterstitial() {
        if (hasNavigated || isFinishing || isDestroyed) return
        lockSplashAfterShowCommit()
        AdsManager.showInterstitialNormal(this) { state ->
            runOnUiThread {
                when (state) {
                    is AdUiState.Idle,
                    is AdUiState.Error,
                    -> finishSplashAds()
                    else -> Unit
                }
            }
        }
    }

    private fun loadSplashInterstitial() {
        if (splashAdsLocked || splashInterRequested) {
            NextGenAdCheck.skip(
                NextGenAdCheck.INTER,
                BuildConfig.inter_splash,
                "splash inter already requested / locked — no reload",
            )
            return
        }
        when (splashAdPath) {
            SplashAdPath.INTER_NATIVE,
            SplashAdPath.INTER_BANNER,
            SplashAdPath.INTER_ONLY,
            -> Unit
            else -> {
                NextGenAdCheck.skip(
                    NextGenAdCheck.INTER,
                    BuildConfig.inter_splash,
                    "path=$splashAdPath — inter not allowed",
                )
                interFailed = true
                return
            }
        }
        if (!AiFaceApp.isInterSplash) {
            interFailed = true
            return
        }
        splashInterRequested = true
        val tryHigh = AiFaceApp.isInterSplashHf
        fun load(unitId: String, allowFallback: Boolean) {
            Log.w("checkAD", "splash inter request unitId=$unitId hfFallback=$allowFallback")
            AdsManager.loadInterstitialNormal(unitId) { state ->
                runOnUiThread {
                    if (isSplashRequestBlocked() && state !is AdUiState.Ready) {
                        return@runOnUiThread
                    }
                    when (state) {
                        is AdUiState.Ready -> {
                            if (splashAdsFinished || hasNavigated) return@runOnUiThread
                            interReady = true
                            interFailed = false
                            Log.d("checkAD", "splash inter loaded unitId=$unitId")
                            LogUtils.printLog("splash_inter loaded", unitId)
                            tryShowInterstitialNow()
                        }
                        is AdUiState.Error -> {
                            if (allowFallback && !isSplashRequestBlocked()) {
                                Log.e(
                                    "checkAD",
                                    "splash inter failed unitId=$unitId msg=${state.message} — fallback normal",
                                )
                                LogUtils.printLog("splash_interhf failed", unitId)
                                load(BuildConfig.inter_splash, allowFallback = false)
                            } else if (!isSplashRequestBlocked()) {
                                interReady = false
                                interFailed = true
                                Log.e(
                                    "checkAD",
                                    "splash inter failed unitId=$unitId msg=${state.message}",
                                )
                                LogUtils.printLog("splash_inter failed", state.message)
                                finishSplashAds()
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
        if (tryHigh) {
            load(BuildConfig.inter_splash_high, allowFallback = true)
        } else {
            load(BuildConfig.inter_splash, allowFallback = false)
        }
    }

    private fun loadSplashNativeBottom() {
        if (splashAdsLocked || splashNativeRequested) {
            NextGenAdCheck.skip(
                NextGenAdCheck.NATIVE,
                BuildConfig.native_splash,
                "splash native already requested / locked — no reload",
            )
            return
        }
        if (splashAdPath != SplashAdPath.INTER_NATIVE && splashAdPath != SplashAdPath.NATIVE_ONLY) {
            NextGenAdCheck.skip(
                NextGenAdCheck.NATIVE,
                BuildConfig.native_splash,
                "path=$splashAdPath — native not allowed",
            )
            settleBottom(adShown = false)
            return
        }
        val container = binding?.nativeAdView ?: return
        splashNativeRequested = true
        binding?.shimmer?.visibility = View.GONE
        container.visibility = View.VISIBLE
        AdShimmerHelper.showNativeShimmerWithoutMedia(container)

        val tryHigh = AiFaceApp.isNativeSplashHf
        fun load(unitId: String, allowFallback: Boolean) {
            Log.d("checkAD", "splash native request unitId=$unitId hfFallback=$allowFallback")
            AdsManager.loadNativeNormal(
                adUnitId = unitId,
                onState = { state ->
                    runOnUiThread {
                        if (state is AdUiState.Error) {
                            if (isSplashRequestBlocked()) {
                                AdShimmerHelper.hideShimmerWithoutMedia(container)
                                return@runOnUiThread
                            }
                            if (allowFallback) {
                                Log.d("checkAD", "splash native failed unitId=$unitId — fallback normal")
                                load(BuildConfig.native_splash, allowFallback = false)
                            } else {
                                Log.d("checkAD", "splash native failed unitId=$unitId")
                                AdShimmerHelper.hideShimmerWithoutMedia(container)
                                settleBottom(adShown = false)
                            }
                        }
                    }
                },
                onAdReady = { nativeAd ->
                    runOnUiThread {
                        if (isSplashRequestBlocked()) {
                            Log.d("checkAD", "splash native loaded but blocked — destroy unitId=$unitId")
                            nativeAd.destroy()
                            return@runOnUiThread
                        }
                        Log.d("checkAD", "splash native loaded unitId=$unitId")
                        NativeAdDisplayHelper.displayWithoutMedia(
                            container = container,
                            inflater = layoutInflater,
                            nativeAd = nativeAd,
                            onDestroyPrevious = { AdsManager.destroyDisplayedNativeAd() },
                            adUnitId = unitId,
                        )
                        settleBottom(adShown = true)
                    }
                },
            )
        }
        if (tryHigh) {
            load(BuildConfig.native_splash_high, allowFallback = true)
        } else {
            load(BuildConfig.native_splash, allowFallback = false)
        }
    }

    private fun loadSplashBannerBottom() {
        if (splashBannerRequested) {
            NextGenAdCheck.skip(
                NextGenAdCheck.BANNER,
                BuildConfig.banner_splash,
                "splash banner already requested — no reload",
            )
            return
        }
        if (splashAdPath != SplashAdPath.INTER_BANNER) {
            NextGenAdCheck.skip(
                NextGenAdCheck.BANNER,
                BuildConfig.banner_splash,
                "path=$splashAdPath — banner not allowed",
            )
            settleBottom(adShown = false)
            return
        }
        if (hasNavigated || isFinishing || isDestroyed) return
        val container = binding?.nativeAdView ?: return
        splashBannerRequested = true
        binding?.shimmer?.visibility = View.GONE
        container.visibility = View.VISIBLE
        AdShimmerHelper.showBannerShimmer(container)

        val adView = AdView(this)
        BannerSizeHelper.applyMatchParentWidth(adView)
        bannerAdView = adView
        container.addView(adView)

        val bannerUnitId = BuildConfig.banner_splash
        Log.d("checkAD", "splash banner request unitId=$bannerUnitId")
        AdsManager.loadBannerNormal(this, adView, bannerUnitId) { state ->
            runOnUiThread {
                when (state) {
                    is AdUiState.Ready -> {
                        // Always show when loaded — do not block behind inter lock.
                        if (hasNavigated || isFinishing || isDestroyed) return@runOnUiThread
                        Log.d("checkAD", "splash banner loaded unitId=$bannerUnitId")
                        AdShimmerHelper.hideShimmer(container)
                        settleBottom(adShown = true)
                    }
                    is AdUiState.Error -> {
                        Log.d(
                            "checkAD",
                            "splash banner failed unitId=$bannerUnitId msg=${state.message}",
                        )
                        AdShimmerHelper.hideShimmer(container)
                        settleBottom(adShown = false)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun settleBottom(adShown: Boolean) {
        if (bottomSettled || hasNavigated) return
        bottomSettled = true
        bottomTimeoutJob?.cancel()

        if (splashAdPath == SplashAdPath.NATIVE_ONLY) {
            bottomDisplayJob = lifecycleScope.launch {
                if (adShown) delay(BOTTOM_DISPLAY_MS)
                if (!hasNavigated) finishSplashAds()
            }
        }
    }

    private fun onBottomTimeout() {
        if (bottomSettled || hasNavigated) return
        bottomSettled = true
        binding?.nativeAdView?.let { AdShimmerHelper.hideShimmerWithoutMedia(it) }
        when (splashAdPath) {
            SplashAdPath.NATIVE_ONLY -> finishSplashAds()
            else -> Unit
        }
    }

    private fun finishSplashAds() {
        if (splashAdsFinished) return
        splashAdsFinished = true
        splashAdsLocked = true
        cancelAdJobs()
        GlobalLoader.hide(this)
        navigateNext()
    }

    private fun cancelAdJobs() {
        bottomTimeoutJob?.cancel()
        bottomDisplayJob?.cancel()
        interWaitJob?.cancel()
        bannerStartJob?.cancel()
        // Keep splashHardTimeoutJob — absolute leave timer must survive ads finish.
    }

    private fun showBottomAdSlot() {
        binding?.clAd?.visibility = View.VISIBLE
        binding?.nativeAdView?.visibility = View.VISIBLE
        // Layout include shimmer hidden — AdShimmerHelper paints into nativeAdView.
        binding?.shimmer?.visibility = View.GONE
    }

    private fun hideBottomAdSlot() {
        binding?.shimmer?.visibility = View.GONE
        binding?.clAd?.visibility = View.INVISIBLE
        binding?.nativeAdView?.visibility = View.GONE
    }

    // ── Auth + navigation ───────────────────────────────────────────────────

    private fun observeRegistrationState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collectLatest { state ->
                    when (state) {
                        RegisterState.Success,
                        RegisterState.AlreadyRegistered,
                        -> {
                            registrationRetryJob?.cancel()
                            if (!splashAdsFinished &&
                                (!AdsHelper.shouldShowAds() || isProVersion.value == true)
                            ) {
                                splashAdsFinished = true
                                splashAdsLocked = true
                                hideBottomAdSlot()
                            }
                            Log.d(TAG, "Auth ready ($state) — attempt navigate")
                            navigateNext()
                        }
                        is RegisterState.Error -> {
                            Log.e(TAG, "Auth error: ${state.message}")
                            if (!hasNavigated && NetworkUtils.isOnline(this@SplashActivity)) {
                                scheduleRegistrationRetry()
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun scheduleRegistrationRetry() {
        registrationRetryJob?.cancel()
        registrationRetryJob = lifecycleScope.launch {
            delay(2_000)
            if (!hasNavigated && !viewModel.isRegistrationComplete()) {
                viewModel.retryRegistration()
            }
        }
    }

    private fun shouldWaitForRegistration(): Boolean {
        if (!NetworkUtils.isOnline(this)) return false
        return !viewModel.isRegistrationComplete()
    }

    private fun navigateNext() {
        if (hasNavigated) return

        if (!splashAdsFinished) {
            pendingNavigation = true
            Log.d(TAG, "Navigation waiting for splash ads, path=$splashAdPath")
            return
        }

        if (shouldWaitForRegistration()) {
            pendingNavigation = true
            Log.d(TAG, "Navigation waiting for access token")
            viewModel.ensureAccessToken()
            return
        }

        pendingNavigation = false
        registrationRetryJob?.cancel()
        performNavigation()
    }

    private fun performNavigation() {
        if (hasNavigated) return
        hasNavigated = true
        cancelAdJobs()
        registrationRetryJob?.cancel()
        splashHardTimeoutJob?.cancel()

        try {
            seedCreditsAfterSplash()
            startNextActivityIntent()
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seedCreditsAfterSplash() {
        (application as? AiFaceApp)?.appOpenAdManager?.startPreloadAfterSplash()
        lifecycleScope.launch {
            creditManager.ensureFreeCredits()
            dailyCheckInRepository.syncWithServer()
        }
    }

    private fun startNextActivityIntent() {
        val theIntents = intent
        if (theIntents.action?.contains("my_shortcut") == true) {
            if (theIntents.action == "my_shortcut.ACTION_UNINSTALL_SHORTCUT") {
                startActivity(Intent(this, UninstallActivity::class.java))
            } else {
                startActivity(buildPostSplashIntent())
            }
        } else {
            startActivity(buildPostSplashIntent())
        }
    }

    private fun buildPostSplashIntent(): Intent {
        if (isProVersion.value == false) {
            return Intent(this, IAPActivity::class.java)
        }
        return when {
            isOnboard && isLanguage -> Intent(this, LanguageActivity::class.java)
            isOnboard  -> Intent(this, FullOnboardActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
    }

    private fun showProgressBar(duration: Long = PROGRESS_UI_MS) {
        binding?.loadingBar?.progress = 0
        lifecycleScope.launch {
            val steps = 100
            val delayPerStep = duration.toDouble() / steps
            for (i in 1..steps) {
                binding?.loadingBar?.progress = i
                delay(delayPerStep.toLong())
            }
        }
    }

    // ── Notification permission ─────────────────────────────────────────────

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private val permissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isAccessed ->
        notificationPermissionContinuation?.invoke(isAccessed)
        notificationPermissionContinuation = null
    }

    /** Blocks until allow/deny (or already granted / pre-Tiramisu). */
    private suspend fun awaitNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || isNotiEnabled(this)) {
            return
        }
        askNotificationPermissionSuspend()
        Log.d(TAG, "Notification permission resolved — continue splash")
    }

    private suspend fun askNotificationPermissionSuspend(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        if (isNotiEnabled(this)) return true
        return suspendCancellableCoroutine { continuation ->
            notificationPermissionContinuation = { continuation.resume(it) }
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun isNotiEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ── Locale ──────────────────────────────────────────────────────────────

    fun setLocate(activity: Activity) {
        var lang = Locale.getDefault().language
        dataStoreManager.readDataStoreValue(IS_LANGUAGE, "") {
            val langnew = this
            if (langnew == "") {
                val supportedLangs = listOf(
                    "ja", "es", "in", "hi", "de", "it", "pt", "ko", "fr", "ar", "vi", "ta",
                )
                lang = if (lang in supportedLangs) lang else "en"
            } else {
                lang = langnew
            }
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            activity.baseContext.resources.updateConfiguration(
                config,
                activity.baseContext.resources.displayMetrics,
            )
        }
    }

    override fun onDestroy() {
        registrationRetryJob?.cancel()
        splashHardTimeoutJob?.cancel()
        cancelAdJobs()
        bannerAdView?.destroy()
        bannerAdView = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SplashAds"

        private const val BOTTOM_AD_TIMEOUT_MS = 5_000L
        private const val BOTTOM_DISPLAY_MS = 1_500L
        private const val INTER_LOADER_MS = 800L
        /** Cap inter wait — was 100s (cold-start / ANR risk). */
        private const val INTER_MAX_WAIT_MS = 12_000L
        /** Absolute splash ceiling — was 100s. */
        private const val SPLASH_MAX_WAIT_MS = 18_000L
        private const val BANNER_START_DELAY_MS = 500L
        /** Cosmetic progress only — must not feel like 15s block. */
        private const val PROGRESS_UI_MS = 2_500L
        private const val RC_SETTLE_MS = 400L
        /** If RC LiveData stuck false (race), unblock splash ads flow. */
        private const val RC_READY_FALLBACK_MS = 6_000L
        private const val IAP_PRO_TIMEOUT_MS = 2_500L

        var isSplash = true
        var isOnboard = false
        var isLanguage = false
    }
}
