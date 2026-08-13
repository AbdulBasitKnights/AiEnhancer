package com.aiface.aging.features.language

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.aiface.aging.BuildConfig
import com.aiface.aging.BuildConfig.native_language
import com.aiface.aging.MainActivity
import com.aiface.aging.AiFaceApp
import com.aiface.aging.AiFaceApp.Companion.langSetting
import com.aiface.aging.AiFaceApp.Companion.survey_enable
import com.aiface.aging.R
import com.aiface.aging.SplashActivity
import com.aiface.aging.SplashActivity.Companion.isLanguage
import com.aiface.aging.shared.DataStoreManager
import com.aiface.aging.shared.IS_LANGUAGE
import com.aiface.aging.shared.IS_ONBOARD
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.getMediationInfo
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.AdsHelper.langCtaColor
import com.aiface.aging.shared.ads.AdsHelper.langCtaTextColor
import com.aiface.aging.shared.ads.AdsHelper.langCtaTextStyle
import com.aiface.aging.shared.ads.AdsHelper.langNative1Enabled
import com.aiface.aging.shared.ads.AdsHelper.langNative2Enabled
import com.aiface.aging.shared.ads.AdsHelper.langNativeAd1
import com.aiface.aging.shared.ads.AdsHelper.langNativeAd2
import com.aiface.aging.shared.ads.AdsHelper.langNativeAdHigh1
import com.aiface.aging.shared.ads.AdsHelper.langNativeAdHigh2
import com.aiface.aging.shared.ads.AdsHelper.langNativeFormat
import com.aiface.aging.shared.ads.AdsHelper.langNativeHigh1Enabled
import com.aiface.aging.shared.ads.AdsHelper.langNativeHigh2Enabled
import com.aiface.aging.shared.ads.AdsHelper.languageButtonDelay
import com.aiface.aging.shared.ads.AdsHelper.languageButtonStyle
import com.aiface.aging.shared.ads.AdsHelper.loadWithFallback
import com.aiface.aging.shared.ads.AdsHelper.obEnable
import com.aiface.aging.shared.ads.loadInterSurvey
import com.aiface.aging.shared.ads.loadInterSurveyHigh
import com.aiface.aging.shared.ads.loadNativeLanguageAltHigh
import com.aiface.aging.shared.ads.loadNativeLanguageAltNormal
import com.aiface.aging.shared.ads.nativeLanguage
import com.aiface.aging.shared.ads.nativeLanguageAlt
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.databinding.ActivityLanguageBinding
import com.aiface.aging.features.fullonboard.FullOnboardActivity
import com.aiface.aging.features.main.MainFragment
import com.aiface.aging.features.onboard.OnboardingActivity
import com.aiface.aging.features.survey.SurveyActivity
import com.aiface.aging.shared.IS_LANGUAGE_SPLASH
import com.aiface.aging.shared.ads.nativeLanguageLiveData
import com.aiface.aging.utils.FirebaseLogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity(),
    LanguageSelectionAdapter.LanguageSelectionClickListener {
    private var binding: ActivityLanguageBinding? = null


    private var selectedLanguage = "none"

    private var isOnboard = false
    private var isShow = false

    private var adapter: LanguageSelectionAdapter? = null

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLocate(this)
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()
        applyLightSystemBars()

        FirebaseLogUtils.logEvent("language_view", "")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (!langSetting) {
            binding?.back?.visibility = View.GONE
        }
        else{
            binding?.back?.visibility = View.VISIBLE
        }

        preloadLanguageNativeIfNeeded()
        nativeLanguageLiveData.observe(this) {
            if (it == true && nativeLanguage != null) {
                showNativeLanguage(this@LanguageActivity)
            } else if (it == false) {
                binding?.clbottom?.visibility = View.GONE
            }
        }

        if (isProVersion.value == true) {
            binding?.clbottom?.visibility = View.GONE
        } else if (nativeLanguage != null) {
            showNativeLanguage(this)
        }

        dataStoreManager.readDataStoreValue(IS_ONBOARD, false) {
            isOnboard = !this
        }
        dataStoreManager.writeDataStoreValue(IS_LANGUAGE_SPLASH, true)
        doneButtonDisableStyle()
        setLanguageRv(this)

        // Alt native: one request only, 500ms after primary language native path starts.
        lifecycleScope.launch {
            delay(500)
            if (isFinishing || isDestroyed) return@launch
            when {
                langNative2Enabled -> {
                    loadNativeLanguageAltNormal(this@LanguageActivity) { }
                }
            }
        }



        if (isProVersion.value == false) {
            lifecycleScope.launch {
                delay(4000)
                binding?.langLoading?.visibility = View.GONE

            }
        } else {
            binding?.langLoading?.visibility = View.GONE
        }



        binding?.btnDone?.setOnClickListener {
            if (selectedLanguage == "none") {
                Toast.makeText(
                    this,
                    getString(R.string.plz_select_language),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!isOnboard) {
                dataStoreManager.writeDataStoreValue(IS_LANGUAGE, selectedLanguage)
            }
            //   dataStoreManager.writeDataStoreValue(IS_LANGUAGE, selectedLanguage)


            navigateNext()
        }
        binding?.icBtnDone?.setOnClickListener {
            if (selectedLanguage == "none") {
                Toast.makeText(
                    this,
                    getString(R.string.plz_select_language),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }




            if (!isOnboard) {
                dataStoreManager.writeDataStoreValue(IS_LANGUAGE, selectedLanguage)
            }
            //   dataStoreManager.writeDataStoreValue(IS_LANGUAGE, selectedLanguage)

            navigateNext()
        }

        binding?.back?.setOnClickListener {
            finish()
        }

    }
    private fun preloadLanguageNativeIfNeeded() {
        if (!AdsHelper.shouldShowAds() || isProVersion.value == true) {
            Log.w("checkAD", "native language skip — ads off / pro")
            return
        }
        binding?.clbottom?.visibility = View.VISIBLE
        // Skip if already held from a prior screen.
        if (nativeLanguage != null) {
            Log.d("checkAD", "native language already held — skip request")
            return
        }
        if (!langNativeHigh1Enabled && !langNative1Enabled) {
            Log.w("checkAD", "native language skip — both high/normal RC off")
            binding?.clbottom?.visibility = View.GONE
            return
        }

        // High + normal share the same unit id — request once only (no fail→normal fallback).
        val useHigh = langNativeHigh1Enabled
        val unitId = if (useHigh) {
            BuildConfig.native_language_high
        } else {
            native_language
        }
        val floor = if (useHigh) "high" else "normal"

        Log.w("checkAD", "native language $floor request unitId=$unitId")
        com.aiface.aging.ads_nextgen.NextGenNativeLoader.load(
            adUnitId = unitId,
            onLoaded = { ad ->
                Log.d("checkAD", "native language $floor loaded unitId=$unitId")
                nativeLanguage = ad
                nativeLanguageLiveData.postValue(true)
            },
            onFailed = { msg ->
                Log.e("checkAD", "native language $floor failed unitId=$unitId msg=$msg")
                nativeLanguageLiveData.postValue(false)
            },
        )
    }
    private fun navigateNext() {
        FirebaseLogUtils.logEvent("language_click_next", "")

        if (isOnboard){

            /*if (AiFaceApp.isInterSurveyHf && AiFaceApp.isInterSurvey && survey_enable) {
                loadInterSurveyHigh(this)
            } else if (AiFaceApp.isInterSurvey && survey_enable) {
                loadInterSurvey(this)
            }*/
            if(survey_enable) {
                val nextActivity = if (obEnable && isOnboard) {
                    loadOB2Ads()
                    FullOnboardActivity::class.java
                } else {
                    MainFragment.selectedItem.value = 0
                    FullOnboardActivity::class.java
                }
                startActivity(Intent(this, nextActivity))
                finish()
            }
            else{
                val nextActivity = if (obEnable && isOnboard) {
                    loadOB2Ads()
                    FullOnboardActivity::class.java
                } else {
                    MainFragment.selectedItem.value = 0
                    FullOnboardActivity::class.java
                }
                startActivity(Intent(this, nextActivity))
                finish()
            }

        }else{
            startActivity(Intent(this, MainActivity::class.java))
        }


    }

    private fun setLanguageRv(activity: FragmentActivity) {
        val languageList = arrayListOf<LanguageModel>()

        languageList.add(
            LanguageModel(
                2,
                "ja",
                "Japanese",
                ContextCompat.getDrawable(activity, R.drawable.japan_svg),
                false
            )
        )
        languageList.add(
            LanguageModel(
                3,
                "hi",
                "Hindi",
                ContextCompat.getDrawable(activity, R.drawable.india_svg),
                false
            )
        )
        languageList.add(
            LanguageModel(
                4,
                "in",
                "Indonesian",
                ContextCompat.getDrawable(activity, R.drawable.indo_svg),
                false
            )
        )
        languageList.add(
            LanguageModel(
                1,
                "en",
                "English (Auto)",
                ContextCompat.getDrawable(activity, R.drawable.eng_svg),
                false
            )
        )
        languageList.add(
            LanguageModel(
                5,
                "es",
                "Spanish",
                ContextCompat.getDrawable(activity, R.drawable.flag_spain),
                false
            )
        )
        languageList.add(
            LanguageModel(
                6,
                "de",
                "German",
                ContextCompat.getDrawable(activity, R.drawable.flag_germany),
                false
            )
        )
        languageList.add(
            LanguageModel(
                7,
                "it",
                "Italian",
                ContextCompat.getDrawable(activity, R.drawable.flag_italy),
                false
            )
        )
        languageList.add(
            LanguageModel(
                8,
                "pt",
                "Portuguese",
                ContextCompat.getDrawable(activity, R.drawable.flag_portugal),
                false
            )
        )
        languageList.add(
            LanguageModel(
                9,
                "ko",
                "Korean",
                ContextCompat.getDrawable(activity, R.drawable.flag_korea),
                false
            )
        )
        languageList.add(
            LanguageModel(
                10,
                "fr",
                "Français",
                ContextCompat.getDrawable(activity, R.drawable.france_svg),
                false
            )
        )
        languageList.add(
            LanguageModel(
                11,
                "ar",
                "العربية",
                ContextCompat.getDrawable(activity, R.drawable.saudia_svg),
                false
            )
        )
        languageList.add(
            LanguageModel(
                12,
                "vi",
                "Tiếng Việt",
                ContextCompat.getDrawable(activity, R.drawable.vietnam_svg),
                false
            )
        )
        languageList.add(
            LanguageModel(
                13,
                "ta",
                "தமிழ்",
                ContextCompat.getDrawable(activity, R.drawable.tamil_svg),
                false
            )
        )

        adapter = LanguageSelectionAdapter(languageList, this)
        binding?.rvLanguage?.adapter = adapter
    }

    override fun onLanguageClick(language: LanguageModel?) {

        mSelectedLanguage = language?.lang ?: "en"
        //     binding?.btnDone?.background = ContextCompat.getDrawable(this,R.drawable.bg_btn_selected)

        if (!isShow) {
            loadOB1Ads()

            isShow = true
            binding?.progressBar?.visibility = View.VISIBLE
            binding?.btnDone?.visibility = View.INVISIBLE
            binding?.icBtnDone?.visibility = View.INVISIBLE
            lifecycleScope.launch {
                delay(1000 * languageButtonDelay.toLong())
                showNativeLanguageAlt(this@LanguageActivity)
                delay(1000)
                doneButtonStyle()
            }
        }
            selectedLanguage = language?.lang ?: "en"
          //  binding?.btnDone?.background = ContextCompat.getDrawable(this,R.drawable.bg_btn_selected)


    }

    fun doneButtonDisableStyle() {
        when (languageButtonStyle) {
            1 -> {
                binding?.btnDone?.apply {
                    background = ContextCompat.getDrawable(
                        this@LanguageActivity,
                        R.drawable.bg_unselected_btn
                    )
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.white))

                    visibility = View.VISIBLE
                }
            }

            2 -> {
                binding?.icBtnDone?.apply {
                    setImageDrawable(
                        ContextCompat.getDrawable(
                            this@LanguageActivity,
                            R.drawable.arrow_next
                        )
                    )
                    visibility = View.VISIBLE
                }
            }

            3 -> {
                binding?.btnDone?.apply {
                    setTextColor(
                        ContextCompat.getColor(
                            this@LanguageActivity,
                            R.color.unselected_color
                        )
                    )
                    visibility = View.VISIBLE
                }
            }

            else -> {
                binding?.btnDone?.apply {
                    background = ContextCompat.getDrawable(
                        this@LanguageActivity,
                        R.drawable.bg_unselected_btn
                    )
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.white))
                    visibility = View.VISIBLE
                }
            }
        }
    }

    fun doneButtonStyle() {
        binding?.progressBar?.visibility = View.GONE
        when (languageButtonStyle) {
            1 -> {
                binding?.btnDone?.apply {
                    background = ContextCompat.getDrawable(
                        this@LanguageActivity,
                        R.drawable.bg_selected_btn
                    )
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.white))

                    visibility = View.VISIBLE
                }
            }

            2 -> {
                binding?.icBtnDone?.apply {
                    setImageDrawable(
                        ContextCompat.getDrawable(
                            this@LanguageActivity,
                            R.drawable.arrow_next_enable
                        )
                    )
                    visibility = View.VISIBLE
                }
            }

            3 -> {
                binding?.btnDone?.apply {
                    setTextColor(
                        ContextCompat.getColor(
                            this@LanguageActivity,
                            R.color.primaryColor
                        )
                    )
                    visibility = View.VISIBLE
                }
            }

            else -> {
                binding?.btnDone?.apply {
                    background = ContextCompat.getDrawable(
                        this@LanguageActivity,
                        R.drawable.bg_selected_btn
                    )
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.white))
                    visibility = View.VISIBLE
                }
            }
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

    companion object {
        var mSelectedLanguage = "none"
    }


    private fun showNativeLanguage(activity: FragmentActivity) {
        try {
            if (isProVersion.value == true) return
            nativeLanguage?.let {

                val layoutResId = R.layout.layout_native_ads

                val adView = LayoutInflater.from(activity)
                    .inflate(layoutResId, null) as NativeAdView

                populateNativeAdView(it, adView, activity)

                binding?.clbottom?.visibility = View.VISIBLE
                binding?.nativeAdView?.removeAllViews()
                binding?.nativeAdView?.addView(adView)
                binding?.nativeAdView?.visibility = View.VISIBLE
                nativeLanguage=null
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun showNativeLanguageAlt(activity: FragmentActivity) {
        try {

            if (isProVersion.value == true) return
            nativeLanguageAlt?.let {

                val layoutResId = R.layout.layout_native_ads

                val adView = LayoutInflater.from(activity)
                    .inflate(layoutResId, null) as NativeAdView

                populateNativeAdView(it, adView, activity)

                binding?.clbottom?.visibility = View.VISIBLE
                binding?.nativeAdView?.removeAllViews()
                binding?.nativeAdView?.addView(adView)
                binding?.nativeAdView?.visibility = View.VISIBLE
                nativeLanguageAlt=null
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        activity: FragmentActivity
    ) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        // adView.iconView = adView.findViewById(R.id.ad_app_icon)
        val mediaView = adView.findViewById<com.google.android.libraries.ads.mobile.sdk.nativead.MediaView>(R.id.ad_media)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? AppCompatButton)?.text = nativeAd.callToAction
        

        adView.registerNativeAd(nativeAd, mediaView)
    }

    fun loadOB1Ads() {
        // ONBOARDING 1
       /* if (AdsHelper.obFirstEnable) {

            // Native 1
            if (AdsHelper.obNativeAdHigh1 == null) {
                loadWithFallback(
                    activity = this,
                    highFloorAdId = getString(R.string.native_ob1_high),
                    normalAdId = getString(R.string.native_ob1),
                    showHighfloor = AdsHelper.obNativeHigh1Enabled,
                    showNormalfloor = AdsHelper.obNative1Enabled,
                    onAdLoadedHigh = { AdsHelper.obNativeAdHigh1 = it },
                    onAdLoadedNormal = { AdsHelper.obNativeAd1 = it },
                    onAdFailed = {}
                )
            }

            // Fullscreen 1
            loadWithFallback(
                activity = this,
                highFloorAdId = getString(R.string.native_full_ob1_high),
                normalAdId = getString(R.string.native_full_ob1),
                showHighfloor = AdsHelper.obNativeHighFullScr1Enabled,
                showNormalfloor = AdsHelper.obNativeFullScr1Enabled,
                onAdLoadedHigh = {
                    AdsHelper.obNativeAdHighFullScr1 = it
                    AdsHelper.obFull1Ready()
                },
                onAdLoadedNormal = {
                    AdsHelper.obNativeAdFullScr1 = it
                    AdsHelper.obFull1Ready()
                },
                onAdFailed = {}
            )
        }*/
    }

    fun loadOB2Ads() {

        // ONBOARDING 2
      /*  if (AdsHelper.obSecondEnable) {

            // Fullscreen 2
            loadWithFallback(
                activity = this,
                highFloorAdId = getString(R.string.native_full_ob2_high),
                normalAdId = getString(R.string.native_full_ob2),
                showHighfloor = AdsHelper.obNativeHighFullScr2Enabled,
                showNormalfloor = AdsHelper.obNativeFullScr2Enabled,
                onAdLoadedHigh = {
                    AdsHelper.obNativeAdHighFullScr2 = it
                    AdsHelper.obFull2Ready()
                },
                onAdLoadedNormal = {
                    AdsHelper.obNativeAdFullScr2 = it
                    AdsHelper.obFull2Ready()
                },
                onAdFailed = {}
            )
        }

        if (AdsHelper.obSecondEnable) {
            if (AdsHelper.obNativeAdHigh3 == null) {
                loadWithFallback(
                    activity = this,
                    highFloorAdId = getString(R.string.native_ob3_high),
                    normalAdId = getString(R.string.native_ob3),
                    showHighfloor = AdsHelper.obNativeHigh3Enabled,
                    showNormalfloor = AdsHelper.obNative3Enabled,
                    onAdLoadedHigh = { AdsHelper.obNativeAdHigh3 = it },
                    onAdLoadedNormal = { AdsHelper.obNativeAd3= it },
                    onAdFailed = {}
                )
            }
        }*/
    }
}