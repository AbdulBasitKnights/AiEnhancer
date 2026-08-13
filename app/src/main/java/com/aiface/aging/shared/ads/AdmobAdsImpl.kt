package com.aiface.aging.shared.ads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustEvent
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_CURRENCY
import com.facebook.appevents.AppEventsLogger
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.ads_nextgen.AdsManager
import com.aiface.aging.ads_nextgen.NextGenAdCheck
import com.aiface.aging.ads_nextgen.NextGenInterstitialHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.ads_nextgen.ProductAnalytics
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.utils.AdjustConstant.AD_IMPRESSION_TOKEN
import com.aiface.aging.utils.AdjustConstant.TAG2
import com.aiface.aging.utils.AdjustConstant.TAG_ADJUST_AD_REVENUE
import com.aiface.aging.utils.LogUtils
import java.util.Locale
import java.util.UUID

var isShowingAd = false

var interstitialOb: InterstitialAd? = null
var interstitialHome: InterstitialAd? = null

var nativeLanguage: NativeAd? = null
var nativeLanguageLiveData = MutableLiveData<Boolean>()

var nativeLanguageAlt: NativeAd? = null

var interstitialSurvey: InterstitialAd? = null
private fun isGoogleTestAdUnit(adUnitId: String?): Boolean =
    !adUnitId.isNullOrBlank() && adUnitId.contains(GOOGLE_TEST_AD_PUBLISHER)
private const val GOOGLE_TEST_AD_PUBLISHER = "ca-app-pub-3940256099942544"

fun loadNativeLanguageHigh(activity: FragmentActivity) {
    if (!AdsHelper.shouldShowAds()) return
    try {
        // Same unit as normal — one request only, no fail→normal fallback.
        val adUnitId = BuildConfig.native_language_high
        Log.w("checkAD", "native language high request unitId=$adUnitId")
        NextGenNativeLoader.load(
            adUnitId = adUnitId,
            onLoaded = { nativeAd ->
                nativeLanguage = nativeAd
                Log.d("checkAD", "native language high loaded unitId=$adUnitId")
                LogUtils.printLog("language native hf loaded", BuildConfig.native_language_high)
            },
            onFailed = { msg ->
                Log.e("checkAD", "native language high failed unitId=$adUnitId msg=$msg")
                LogUtils.printLog("language native hf failed", BuildConfig.native_language_high)
            }
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadNativeLanguageNormal(activity: FragmentActivity) {
    if (!AdsHelper.shouldShowAds()) return
    try {
        val adUnitId = BuildConfig.native_language
        Log.w("checkAD", "native language normal request unitId=$adUnitId")
        NextGenNativeLoader.load(
            adUnitId = adUnitId,
            onLoaded = { nativeAd ->
                nativeLanguage = nativeAd
                Log.d("checkAD", "native language normal loaded unitId=$adUnitId")
                LogUtils.printLog("language native  loaded", BuildConfig.native_language)
            },
            onFailed = { msg ->
                Log.e("checkAD", "native language normal failed unitId=$adUnitId msg=$msg")
                LogUtils.printLog("language native  failed", BuildConfig.native_language)
            }
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadWithFallback(
    activity: Activity,
    highFloorAdId: String,
    normalAdId: String,
    showHighfloor: Boolean = true,
    showNormalfloor: Boolean = true,
    onAdLoadedHigh: (nativeAd: NativeAd) -> Unit,
    onAdLoadedNormal: (nativeAd: NativeAd) -> Unit,
    onAdFailed: (() -> Unit)? = null
) {
    if (!AdsHelper.shouldShowAds()) {
        onAdFailed?.invoke()
        return
    }
    try {
        if (showHighfloor) {
            NextGenNativeLoader.load(
                adUnitId = highFloorAdId,
                onLoaded = onAdLoadedHigh,
                onFailed = {
                    Log.w("OpenAdTest", "Ad failed âŒ Requesting Normal $highFloorAdId")
                    loadNormalAd(normalAdId, showNormalfloor, onAdLoadedNormal, onAdFailed)
                }
            )
        } else {
            loadNormalAd(normalAdId, showNormalfloor, onAdLoadedNormal, onAdFailed)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onAdFailed?.invoke()
    }
}

private fun loadNormalAd(
    adId: String,
    showAd: Boolean = true,
    onAdLoaded: (nativeAd: NativeAd) -> Unit,
    onAdFailed: (() -> Unit)? = null
) {
    if (!AdsHelper.shouldShowAds()) {
        onAdFailed?.invoke()
        return
    }
    if (!showAd) return
    NextGenNativeLoader.load(
        adUnitId = adId,
        onLoaded = onAdLoaded,
        onFailed = {
            Log.e("OpenAdTest", "Ad failed âŒ Requesting Normal $adId")
            onAdFailed?.invoke()
        }
    )
}

fun loadNativeLanguageAltHigh(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    if (!AdsHelper.shouldShowAds()) return
    try {
        // Same unit as alt normal — one request only, no fail→normal fallback.
        val adUnitId = BuildConfig.native_language_alt_high
        Log.w("checkAD", "native language alt high request unitId=$adUnitId")
        NextGenNativeLoader.load(
            adUnitId = adUnitId,
            onLoaded = { nativeAd ->
                nativeLanguageAlt = nativeAd
                onResult(true)
                Log.d("checkAD", "native language alt high loaded unitId=$adUnitId")
                LogUtils.printLog("language native alt hf loaded", BuildConfig.native_language_alt_high)
            },
            onFailed = { msg ->
                onResult(false)
                Log.e("checkAD", "native language alt high failed unitId=$adUnitId msg=$msg")
                LogUtils.printLog("language native alt hf faliled", BuildConfig.native_language_alt_high)
            }
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadNativeLanguageAltNormal(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    if (!AdsHelper.shouldShowAds()) return
    try {
        val adUnitId = BuildConfig.native_language_alt
        Log.w("checkAD", "native language alt normal request unitId=$adUnitId")
        NextGenNativeLoader.load(
            adUnitId = adUnitId,
            onLoaded = { nativeAd ->
                nativeLanguageAlt = nativeAd
                onResult(true)
                Log.d("checkAD", "native language alt normal loaded unitId=$adUnitId")
                LogUtils.printLog("language native alt loaded", BuildConfig.native_language_alt)
            },
            onFailed = { msg ->
                onResult(false)
                Log.e("checkAD", "native language alt normal failed unitId=$adUnitId msg=$msg")
                LogUtils.printLog("language native alt failed", BuildConfig.native_language_alt)
            }
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadInterSurveyHigh(
    context: Context
) {
    if (!AdsHelper.shouldShowAds() || interstitialSurvey != null) return
    NextGenInterstitialHelper.load(
        adUnitId = BuildConfig.inter_survey_high,
        onLoaded = { ad ->
            interstitialSurvey = ad.rememberAdUnitId(BuildConfig.inter_survey_high)
            LogUtils.printLog("home_inter hf loaded", BuildConfig.inter_survey_high)
        },
        onFailed = {
            loadInterSurvey(context)
            interstitialSurvey = null
            LogUtils.printLog("home_inter hf failed", BuildConfig.inter_survey_high)
        }
    )
}

fun loadInterSurvey(
    context: Context
) {
    if (!AdsHelper.shouldShowAds() || interstitialSurvey != null) return
    NextGenInterstitialHelper.load(
        adUnitId = BuildConfig.inter_survey,
        onLoaded = { ad ->
            interstitialSurvey = ad.rememberAdUnitId(BuildConfig.inter_survey)
            LogUtils.printLog("surveyInter  loaded", BuildConfig.inter_survey)
        },
        onFailed = {
            interstitialSurvey = null
            LogUtils.printLog("surveyInter  failed", BuildConfig.inter_survey)
        }
    )
}

fun loadInterObHigh(
    context: Context
) {
    if (!AdsHelper.shouldShowAds() || interstitialOb != null) return
    NextGenInterstitialHelper.load(
        adUnitId = BuildConfig.inter_ob_high,
        onLoaded = { ad ->
            interstitialOb = ad.rememberAdUnitId(BuildConfig.inter_ob_high)
            LogUtils.printLog("home_ob hf loaded", BuildConfig.inter_ob_high)
        },
        onFailed = {
            interstitialOb = null
            LogUtils.printLog("home_ob hf failed", BuildConfig.inter_ob_high)
        }
    )
}


fun loadInterOb(
    context: Context
) {
    if (!AdsHelper.shouldShowAds() || interstitialOb != null) return
    NextGenInterstitialHelper.load(
        adUnitId = BuildConfig.inter_ob,
        onLoaded = { ad ->
            interstitialOb = ad.rememberAdUnitId(BuildConfig.inter_ob)
            LogUtils.printLog("home_ob  loaded", BuildConfig.inter_ob)
        },
        onFailed = {
            interstitialOb = null
            LogUtils.printLog("home_ob  failed", BuildConfig.inter_ob)
        }
    )
}

/**
 * Pull next home inter from SDK preload buffer into [interstitialHome].
 * Poll only - does not start a new AdMob load. Splash does not use this.
 */
fun refillHomeInterFromPreload() {
    if (!AdsHelper.shouldShowAds() || isProVersion.value == true) return
    if (interstitialHome != null) {
        NextGenAdCheck.skip(
            NextGenAdCheck.INTER,
            AdsManager.interstitialPreloadUnitId(),
            "home slot already available to show",
        )
        return
    }
    MainFullscreenAdsPreloader.takeInterstitial()?.let { ad ->
        interstitialHome = ad
        NextGenAdCheck.loaded(
            NextGenAdCheck.INTER,
            AdsManager.interstitialPreloadUnitId(),
            "home slot filled from preload (backup=1)",
        )
    }
}

/**
 * Home inter: single backup only via [MainFullscreenAdsPreloader].
 * Never starts a parallel helper load (that caused 2â€“3 ads after splash).
 */
fun loadInterHomeHigh(
    context: Context, onResult: (Boolean) -> Unit
) {
    ensureSingleHomeInter(BuildConfig.inter_home_high, onResult)
}

fun loadInterHome(
    context: Context, onResult: (Boolean) -> Unit
) {
    ensureSingleHomeInter(BuildConfig.inter_home, onResult)
}

private fun ensureSingleHomeInter(unitId: String, onResult: (Boolean) -> Unit) {
    if (!AdsHelper.shouldShowAds()) {
        onResult(false)
        return
    }
    if (interstitialHome != null) {
        NextGenAdCheck.skip(
            NextGenAdCheck.INTER,
            unitId,
            "already loaded / available to show",
        )
        onResult(true)
        return
    }
    MainFullscreenAdsPreloader.takeInterstitial()?.let { ad ->
        interstitialHome = ad
        NextGenAdCheck.skip(
            NextGenAdCheck.INTER,
            AdsManager.interstitialPreloadUnitId().ifBlank { unitId },
            "took from preload â€” available to show",
        )
        onResult(true)
        return
    }
    // Defer to one preload pipeline after splash/fullscreen clears â€” no helper load.
    NextGenAdCheck.skip(
        NextGenAdCheck.INTER,
        unitId,
        "deferred to single Home+ preload pipeline (no helper)",
    )
    MainFullscreenAdsPreloader.startFromMainWhenAdsClear()
    onResult(false)
}

/** Clear used home inter; preload pipeline handles next fill. */
fun clearInterHomeAfterShow() {
    interstitialHome = null
    MainFullscreenAdsPreloader.onFullscreenAdConsumed()
}

fun adjustRevenueMMP(adUnitId: String?, adRevenue: Double = 0.00, currency: String = "USD", event_token: String = "b0syy4", source: String) {
    try {
        val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
        adjustAdRevenue.setRevenue(adRevenue, currency)
        adjustAdRevenue.addPartnerParameter("ad_format", source)
        adjustAdRevenue.addPartnerParameter("ad_unit_id", adUnitId)
        Adjust.trackAdRevenue(adjustAdRevenue)

        val logger = AppEventsLogger.newLogger(AiFaceApp.getTheContext())
        val params = Bundle()
        params.putString(EVENT_PARAM_CURRENCY, currency)
        logger.logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, adRevenue, params)

    } catch (e: Exception) {

    }
}
fun trackPaidAdRevenue(
    adUnitId: String?,
    adValue: AdValue,
    source: String,
    adNetwork: String = "AdMob",
) {
    val revenue = adValue.valueMicros.toDouble() / 1_000_000.0
    val isTestAd = isGoogleTestAdUnit(adUnitId)
    Log.d(
        TAG_ADJUST_AD_REVENUE,
        "onAdPaid → testAd=$isTestAd micros=${adValue.valueMicros} currency=${adValue.currencyCode} precision=${adValue.precisionType} unit=$adUnitId source=$source network=$adNetwork"
    )
    if (isTestAd) {
        Log.d(
            TAG_ADJUST_AD_REVENUE,
            "TEST AD onAdPaid | Google sample unit usually reports valueMicros=0 (UNKNOWN precision). Still forwarding to Adjust."
        )
    }
    trackAdjustAdRevenue(
        adUnitId = adUnitId,
        revenue = revenue,
        currency = adValue.currencyCode.ifBlank { "USD" },
        token = AD_IMPRESSION_TOKEN,
        source = source,
        adNetwork = adNetwork,
        precisionType = adValue.precisionType,
        fromPaidListener = true,
    )
}
fun trackAdjustAdRevenue(
    adUnitId: String?,
    revenue: Double = 0.00,
    currency: String = "USD",
    token: String = AD_IMPRESSION_TOKEN,
    source: String,
    adNetwork: String = "AdMob",
    precisionType: PrecisionType? = null,
    fromPaidListener: Boolean = false,
) {
    try {
        val adFormat = source.lowercase(Locale.US)
        ProductAnalytics.log(
            ProductAnalytics.PAID_AD_IMPRESSION,
            ProductAnalytics.SCREEN_OVERALL,
            mapOf(
                ProductAnalytics.PARAM_AD_FORMAT to adFormat,
                ProductAnalytics.PARAM_SOURCE_FEATURE to adFormat,
            ),
        )
        val normalizedCurrency = currency.ifBlank { "USD" }.uppercase(Locale.US)
        val normalizedUnitId = adUnitId?.takeIf { it.isNotBlank() } ?: "unknown"
        val normalizedNetwork = adNetwork.ifBlank { "AdMob" }
        val eventId = UUID.randomUUID().toString()
        val hasRevenue = revenue > 0.0
        val isTestAd = isGoogleTestAdUnit(adUnitId)

        Log.d(
            TAG_ADJUST_AD_REVENUE,
            "track start | testAd=$isTestAd revenue=$revenue $normalizedCurrency unit=$normalizedUnitId format=$adFormat network=$normalizedNetwork precision=$precisionType fromPaid=$fromPaidListener token=$token eventId=$eventId"
        )
        if (isTestAd && !hasRevenue) {
            Log.d(
                TAG_ADJUST_AD_REVENUE,
                "TEST AD tip | ILAR revenue is 0 on sample ads — AdjustAdRevenue still sent; AdjustEvent($token) skipped until revenue > 0"
            )
        }

        // Adjust Ad Revenue (shows under Ad Revenue / admob_sdk).
        // Always send from paid listener; zero-value ILAR still creates the impression row.
        if (hasRevenue || fromPaidListener) {
            val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
            if (hasRevenue) {
                adjustAdRevenue.setRevenue(revenue, normalizedCurrency)
            }
            adjustAdRevenue.setAdImpressionsCount(1)
            adjustAdRevenue.setAdRevenueNetwork(normalizedNetwork)
            adjustAdRevenue.setAdRevenueUnit(normalizedUnitId)
            adjustAdRevenue.setAdRevenuePlacement(adFormat)
            adjustAdRevenue.addCallbackParameter("event_id", eventId)
            adjustAdRevenue.addCallbackParameter("ad_format", adFormat)
            adjustAdRevenue.addCallbackParameter("test_ad", isTestAd.toString())
            adjustAdRevenue.addPartnerParameter("event_id", eventId)
            adjustAdRevenue.addPartnerParameter("ad_network", normalizedNetwork)
            adjustAdRevenue.addPartnerParameter("ad_format", adFormat)
            adjustAdRevenue.addPartnerParameter("ad_unit_id", normalizedUnitId)
            adjustAdRevenue.addPartnerParameter("test_ad", isTestAd.toString())
            precisionType?.let {
                adjustAdRevenue.addPartnerParameter("precision_type", it.name)
                adjustAdRevenue.addCallbackParameter("precision_type", it.name)
            }
            Adjust.trackAdRevenue(adjustAdRevenue)
            Log.d(
                TAG_ADJUST_AD_REVENUE,
                "Adjust.trackAdRevenue SUCCESS | testAd=$isTestAd source=admob_sdk network=$normalizedNetwork placement=$adFormat unit=$normalizedUnitId revenue=$revenue $normalizedCurrency"
            )
            Log.d(
                TAG2,
                "Adjust.trackAdRevenue sent | network=$normalizedNetwork format=$adFormat unit=$normalizedUnitId revenue=$revenue $normalizedCurrency"
            )
        } else {
            Log.w(
                TAG_ADJUST_AD_REVENUE,
                "Adjust.trackAdRevenue SKIPPED | testAd=$isTestAd revenue=$revenue fromPaid=$fromPaidListener"
            )
        }

        // Dashboard custom event token (AppConstant.AD_REVENUE) — previously unused.
        if (hasRevenue && token.isNotBlank()) {
            val adjustEvent = AdjustEvent(token)
            adjustEvent.setRevenue(revenue, normalizedCurrency)
            adjustEvent.addCallbackParameter("event_id", eventId)
            adjustEvent.addCallbackParameter("ad_format", adFormat)
            adjustEvent.addCallbackParameter("ad_unit_id", normalizedUnitId)
            adjustEvent.addCallbackParameter("test_ad", isTestAd.toString())
            adjustEvent.addPartnerParameter("ad_network", normalizedNetwork)
            adjustEvent.addPartnerParameter("ad_format", adFormat)
            adjustEvent.addPartnerParameter("ad_unit_id", normalizedUnitId)
            adjustEvent.addPartnerParameter("test_ad", isTestAd.toString())
            Adjust.trackEvent(adjustEvent)
            Log.d(
                TAG_ADJUST_AD_REVENUE,
                "Adjust.trackEvent SUCCESS | testAd=$isTestAd token=$token format=$adFormat unit=$normalizedUnitId revenue=$revenue $normalizedCurrency"
            )
            Log.d(TAG2, "Adjust.trackEvent($token) revenue sent | format=$adFormat revenue=$revenue")
        } else {
            Log.w(
                TAG_ADJUST_AD_REVENUE,
                "Adjust.trackEvent SKIPPED | testAd=$isTestAd hasRevenue=$hasRevenue tokenBlank=${token.isBlank()}"
            )
        }

        AiFaceApp.context?.let { context ->
            val logger = AppEventsLogger.newLogger(context)
            val params = Bundle().apply {
                putString("event_id", eventId)
                putString("ad_network", normalizedNetwork)
                putString("ad_format", adFormat)
                putString("ad_unit_id", normalizedUnitId)
                putString("currency", normalizedCurrency)
                precisionType?.let { putString("precision_type", it.name) }
            }

            if (hasRevenue) {
                logger.logEvent("fb_ad_revenue", revenue, params)
                logger.logEvent("ad_impression", revenue, params)
                Log.d(
                    TAG2,
                    "Paid revenue sent | network=$normalizedNetwork format=$adFormat unit=$normalizedUnitId revenue=$revenue $normalizedCurrency"
                )
            } else if (fromPaidListener) {
                logger.logEvent("fb_ad_revenue", params)
                logger.logEvent("ad_impression", params)
                Log.d(
                    TAG2,
                    "Fallback impression sent | format=$adFormat unit=$normalizedUnitId"
                )
            }
        } ?: run {
            Log.w(TAG2, "Meta log skipped (application context is null)")
        }
    } catch (e: Exception) {
        Log.e(TAG_ADJUST_AD_REVENUE, "Ad revenue tracking failed: ${e.message}", e)
        Log.e(TAG2, "Ad revenue tracking failed: ${e.message}", e)
    }
}
fun trackAdjustPurchase(
    adUnitId: String?,
    revenue: Double = 0.00,
    currency: String = "USD",
    token: String = AD_IMPRESSION_TOKEN,
    source: String,
    adNetwork: String = "AdMob",
    precisionType: PrecisionType? = null,
    fromPaidListener: Boolean = false,
) {
    try {
        val adFormat = source.lowercase(Locale.US)
        ProductAnalytics.log(
            ProductAnalytics.PAID_AD_IMPRESSION,
            ProductAnalytics.SCREEN_OVERALL,
            mapOf(
                ProductAnalytics.PARAM_AD_FORMAT to adFormat,
                ProductAnalytics.PARAM_SOURCE_FEATURE to adFormat,
            ),
        )
        val normalizedCurrency = currency.ifBlank { "USD" }.uppercase(Locale.US)
        val normalizedUnitId = adUnitId?.takeIf { it.isNotBlank() } ?: "unknown"
        val normalizedNetwork = adNetwork.ifBlank { "AdMob" }
        val eventId = UUID.randomUUID().toString()
        val hasRevenue = revenue > 0.0
        val isTestAd = isGoogleTestAdUnit(adUnitId)

        Log.d(
            TAG_ADJUST_AD_REVENUE,
            "track start | testAd=$isTestAd revenue=$revenue $normalizedCurrency unit=$normalizedUnitId format=$adFormat network=$normalizedNetwork precision=$precisionType fromPaid=$fromPaidListener token=$token eventId=$eventId"
        )
        if (isTestAd && !hasRevenue) {
            Log.d(
                TAG_ADJUST_AD_REVENUE,
                "TEST AD tip | ILAR revenue is 0 on sample ads — AdjustAdRevenue still sent; AdjustEvent($token) skipped until revenue > 0"
            )
        }

        // Adjust Ad Revenue (shows under Ad Revenue / admob_sdk).
        // Always send from paid listener; zero-value ILAR still creates the impression row.
        if (hasRevenue || fromPaidListener) {
            val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
            if (hasRevenue) {
                adjustAdRevenue.setRevenue(revenue, normalizedCurrency)
            }
            adjustAdRevenue.setAdImpressionsCount(1)
            adjustAdRevenue.setAdRevenueNetwork(normalizedNetwork)
            adjustAdRevenue.setAdRevenueUnit(normalizedUnitId)
            adjustAdRevenue.setAdRevenuePlacement(adFormat)
            adjustAdRevenue.addCallbackParameter("event_id", eventId)
            adjustAdRevenue.addCallbackParameter("ad_format", adFormat)
            adjustAdRevenue.addCallbackParameter("test_ad", isTestAd.toString())
            adjustAdRevenue.addPartnerParameter("event_id", eventId)
            adjustAdRevenue.addPartnerParameter("ad_network", normalizedNetwork)
            adjustAdRevenue.addPartnerParameter("ad_format", adFormat)
            adjustAdRevenue.addPartnerParameter("ad_unit_id", normalizedUnitId)
            adjustAdRevenue.addPartnerParameter("test_ad", isTestAd.toString())
            precisionType?.let {
                adjustAdRevenue.addPartnerParameter("precision_type", it.name)
                adjustAdRevenue.addCallbackParameter("precision_type", it.name)
            }
            Adjust.trackAdRevenue(adjustAdRevenue)
            Log.d(
                TAG_ADJUST_AD_REVENUE,
                "Adjust.trackAdRevenue SUCCESS | testAd=$isTestAd source=admob_sdk network=$normalizedNetwork placement=$adFormat unit=$normalizedUnitId revenue=$revenue $normalizedCurrency"
            )
            Log.d(
                TAG2,
                "Adjust.trackAdRevenue sent | network=$normalizedNetwork format=$adFormat unit=$normalizedUnitId revenue=$revenue $normalizedCurrency"
            )
        } else {
            Log.w(
                TAG_ADJUST_AD_REVENUE,
                "Adjust.trackAdRevenue SKIPPED | testAd=$isTestAd revenue=$revenue fromPaid=$fromPaidListener"
            )
        }

        // Dashboard custom event token (AppConstant.AD_REVENUE) — previously unused.
        if (hasRevenue && token.isNotBlank()) {
            val adjustEvent = AdjustEvent(token)
            adjustEvent.setRevenue(revenue, normalizedCurrency)
            adjustEvent.addCallbackParameter("event_id", eventId)
            adjustEvent.addCallbackParameter("ad_format", adFormat)
            adjustEvent.addCallbackParameter("ad_unit_id", normalizedUnitId)
            adjustEvent.addCallbackParameter("test_ad", isTestAd.toString())
            adjustEvent.addPartnerParameter("ad_network", normalizedNetwork)
            adjustEvent.addPartnerParameter("ad_format", adFormat)
            adjustEvent.addPartnerParameter("ad_unit_id", normalizedUnitId)
            adjustEvent.addPartnerParameter("test_ad", isTestAd.toString())
            Adjust.trackEvent(adjustEvent)
            Log.d(
                TAG_ADJUST_AD_REVENUE,
                "Adjust.trackEvent SUCCESS | testAd=$isTestAd token=$token format=$adFormat unit=$normalizedUnitId revenue=$revenue $normalizedCurrency"
            )
            Log.d(TAG2, "Adjust.trackEvent($token) revenue sent | format=$adFormat revenue=$revenue")
        } else {
            Log.w(
                TAG_ADJUST_AD_REVENUE,
                "Adjust.trackEvent SKIPPED | testAd=$isTestAd hasRevenue=$hasRevenue tokenBlank=${token.isBlank()}"
            )
        }

        AiFaceApp.context?.let { context ->
            val logger = AppEventsLogger.newLogger(context)
            val params = Bundle().apply {
                putString("event_id", eventId)
                putString("ad_network", normalizedNetwork)
                putString("ad_format", adFormat)
                putString("ad_unit_id", normalizedUnitId)
                putString("currency", normalizedCurrency)
                precisionType?.let { putString("precision_type", it.name) }
            }

            if (hasRevenue) {
                logger.logEvent("fb_ad_revenue", revenue, params)
                logger.logEvent("ad_impression", revenue, params)
                Log.d(
                    TAG2,
                    "Paid revenue sent | network=$normalizedNetwork format=$adFormat unit=$normalizedUnitId revenue=$revenue $normalizedCurrency"
                )
            } else if (fromPaidListener) {
                logger.logEvent("fb_ad_revenue", params)
                logger.logEvent("ad_impression", params)
                Log.d(
                    TAG2,
                    "Fallback impression sent | format=$adFormat unit=$normalizedUnitId"
                )
            }
        } ?: run {
            Log.w(TAG2, "Meta log skipped (application context is null)")
        }
    } catch (e: Exception) {
        Log.e(TAG_ADJUST_AD_REVENUE, "Ad revenue tracking failed: ${e.message}", e)
        Log.e(TAG2, "Ad revenue tracking failed: ${e.message}", e)
    }
}

object HomeNativeAdManager {

    private val nativeAds = mutableListOf<NativeAd>()

    fun getNativeAds(): List<NativeAd> {
        return nativeAds
    }

    fun loadNativeAds(
        context: Context,
        adUnitId: String,
        count: Int,
        isHighFloor: Boolean,
        onLoaded: (loadedAds: List<NativeAd>) -> Unit,
        onLoadFailed: () -> Unit
    ) {
        if (!AdsHelper.shouldShowAds()) {
            onLoadFailed()
            return
        }
        nativeAds.clear()

        var completedCount = 0

        repeat(count) {
            NextGenNativeLoader.load(
                adUnitId = adUnitId,
                onLoaded = { nativeAd ->
                    nativeAds.add(nativeAd)
                    completedCount++
                    if (completedCount == count) {
                        if (nativeAds.isNotEmpty()) {
                            onLoaded(nativeAds)
                        } else {
                            onLoadFailed()
                        }
                    }
                },
                onFailed = {
                    completedCount++
                    if (completedCount == count) {
                        if (nativeAds.isNotEmpty()) {
                            onLoaded(nativeAds)
                        } else {
                            onLoadFailed()
                        }
                    }
                }
            )
        }
    }

    fun destroyAds() {
        nativeAds.forEach {
            it.destroy()
        }
        nativeAds.clear()
    }
}

object HomeNativeAdParentManager {

    private val nativeAds = mutableListOf<NativeAd>()

    fun getNativeAds(): List<NativeAd> {
        return nativeAds
    }

    fun loadNativeAds(
        context: Context,
        adUnitId: String,
        count: Int,
        isHighFloor: Boolean,
        onLoaded: (loadedAds: List<NativeAd>) -> Unit,
        onLoadFailed: () -> Unit
    ) {
        if (!AdsHelper.shouldShowAds()) {
            onLoadFailed()
            return
        }
        nativeAds.clear()

        var completedCount = 0

        repeat(count) {
            NextGenNativeLoader.load(
                adUnitId = adUnitId,
                onLoaded = { nativeAd ->
                    nativeAds.add(nativeAd)
                    completedCount++
                    if (completedCount == count) {
                        if (nativeAds.isNotEmpty()) {
                            onLoaded(nativeAds)
                        } else {
                            onLoadFailed()
                        }
                    }
                },
                onFailed = {
                    completedCount++
                    if (completedCount == count) {
                        if (nativeAds.isNotEmpty()) {
                            onLoaded(nativeAds)
                        } else {
                            onLoadFailed()
                        }
                    }
                }
            )
        }
    }

    fun destroyAds() {
        nativeAds.forEach {
            it.destroy()
        }
        nativeAds.clear()
    }
}
