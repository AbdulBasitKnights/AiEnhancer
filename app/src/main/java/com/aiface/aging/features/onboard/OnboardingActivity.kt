package com.aiface.aging.features.onboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aiface.aging.MainActivity
import com.aiface.aging.R
import com.aiface.aging.shared.DataStoreManager
import com.aiface.aging.shared.IS_LANGUAGE
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.loadFallbackInterstitialAd
import com.aiface.aging.shared.ads.AdsHelper.loadWithFallback
import com.aiface.aging.shared.ads.AdsHelper.obInterstitialEnabled
import com.aiface.aging.shared.ads.AdsHelper.obInterstitialHighEnabled
import com.aiface.aging.shared.ads.AdsHelper.showInterstitial
import com.aiface.aging.shared.ads.loadInterOb
import com.aiface.aging.shared.ads.loadInterObHigh
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.databinding.ActivityOnboardingBinding
import com.aiface.aging.features.language.LanguageActivity
import com.aiface.aging.features.onboard.adapter.OnboardingViewPager
import com.aiface.aging.features.onboard.adapter.PagerNav

import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlin.text.compareTo

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity(), PagerNav {
    private var binding: ActivityOnboardingBinding? = null

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private val pagerAdapter by lazy { OnboardingViewPager(this) }

    /*  companion object {
          var selectedPosition = MutableLiveData(0)
      }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLocate(this)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()

       /* ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/
        handleBackPress()
        loadOB4ds()
        setAdapter()
    }

    fun setAdapter() {
        binding?.vpOnboard?.adapter = pagerAdapter
        AdsHelper.obFull1Loaded.observe(this) { if (it) pagerAdapter.refresh() }
        AdsHelper.obFull2Loaded.observe(this) { if (it) pagerAdapter.refresh() }

    }

    fun setLocate(activity: Activity) {
        var lang = Locale.getDefault().language //System Default Language
        dataStoreManager.readDataStoreValue(IS_LANGUAGE, "") {
            Log.e("Languageset", this.toString())
            //  val langnew = this
            val langnew = LanguageActivity.mSelectedLanguage
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

    fun loadOB4ds() {

        if (AdsHelper.obFifthEnable) {
            if (AdsHelper.obNativeAdHigh4 == null) {
                loadWithFallback(
                    activity = this,
                    highFloorAdId = getString(R.string.native_ob4_high),
                    normalAdId = getString(R.string.native_ob4),
                    showHighfloor = AdsHelper.obNativeHigh4Enabled,
                    showNormalfloor = AdsHelper.obNative4Enabled,
                    onAdLoadedHigh = { AdsHelper.obNativeAdHigh4 = it },
                    onAdLoadedNormal = { AdsHelper.obNativeAd4 = it },
                    onAdFailed = {}
                )
            }
        }

        if (obInterstitialHighEnabled && obInterstitialEnabled){
            loadInterObHigh(this)
        }else if (obInterstitialEnabled){
            loadInterOb(this)
        }

    }

    override fun goNext() {
        val next = binding?.vpOnboard?.currentItem?.plus(1)
        val total = pagerAdapter.itemCount

        if (next != null) {
            if (next < total) {
                binding?.vpOnboard?.setCurrentItem(next, true)
            } else {
                navigateNext()
            }
        }
    }

    private fun handleBackPress() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isFinishing && !isDestroyed) {
                    navigateNext()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    fun navigateNext(){
            startMain()
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()

    }

    override fun pageCount(): Int = pagerAdapter.itemCount
}