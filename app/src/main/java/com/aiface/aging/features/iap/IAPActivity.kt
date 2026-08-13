package com.aiface.aging.features.iap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.MainActivity
import com.aiface.aging.AiFaceApp
import com.aiface.aging.AiFaceApp.Companion.survey_enable
import com.aiface.aging.R
import com.aiface.aging.SplashActivity
import com.aiface.aging.SplashActivity.Companion.isLanguage
import com.aiface.aging.SplashActivity.Companion.isOnboard
import com.aiface.aging.ads_nextgen.NextGenAdRevenue.trackIapEventFBAdjust
import com.aiface.aging.shared.CreditManager
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.shared.invisible
import com.aiface.aging.shared.privacyPolicy
import com.aiface.aging.shared.show
import com.aiface.aging.shared.termsOfServices
import com.aiface.aging.databinding.ActivityIapBinding
import com.aiface.aging.features.edit.EditFragment
import com.aiface.aging.features.fullonboard.FullOnboardActivity
import com.aiface.aging.features.iap.utils.BillingClientConnectionListener
import com.aiface.aging.features.iap.utils.DataWrappers
import com.aiface.aging.features.iap.utils.IapConnector
import com.aiface.aging.features.iap.utils.SubscriptionServiceListener
import com.aiface.aging.features.language.LanguageActivity
import com.aiface.aging.features.survey.SurveyActivity
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.loadInterHome
import com.aiface.aging.shared.ads.loadInterHomeHigh
import com.aiface.aging.utils.AdjustConstant.ADJUST_SUBSCRIPTION_TOKEN
import com.aiface.aging.utils.AdjustConstant.ADJUST_SUBSCRIPTION_TRIAL_TOKEN
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.SpannableTextHelper.setTwoLineStyledText
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.iterator

@AndroidEntryPoint
class IAPActivity : AppCompatActivity() {
    private lateinit var iapConnector: IapConnector
    private lateinit var binding: ActivityIapBinding
    val isBillingClientConnected: MutableLiveData<Boolean> = MutableLiveData()
    private var currentSku = IapManager.skuKeyWeek
    private val availableProductDetails: MutableMap<String, DataWrappers.ProductDetails> = mutableMapOf()

    private lateinit var creditManager: CreditManager
    var isTrial = false
    var priceCurrency = ""
    private var isFromProPanel = false
    private var isFromHome = false

    private var isFromEdit = false
    private var isFromSplash = false
    private var isTrialOfferAvailable = false
    private var isTrialEnabled = false
    private var trialToggleInitialized = false
    private var loadedProduct: DataWrappers.ProductDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       /* if (!IAP_ENABLED) {
            ToastUtils.showToast(this, getString(R.string.premium_feature_coming_soon))
            finish()
            return
        }*/
        try {
            binding = ActivityIapBinding.inflate(layoutInflater)
            setContentView(binding.root)
            // Retrieve and increment the view count
            enableEdgeToEdge()
            applyLightSystemBars()
            hideNavigationBar()
//            applyWindowInsets()
            creditManager = CreditManager(this)
            val sharedPreferences = getSharedPreferences("iap_prefs", MODE_PRIVATE)
            val viewCount = sharedPreferences.getInt("iap_view_count", 0) + 1
            sharedPreferences.edit { putInt("iap_view_count", viewCount) }
            // Log Firebase event with dynamic view count
            FirebaseLogUtils.logEvent("iap_view", "iap screen")
            onClickListeners()
            unSelectContinue()

            isFromProPanel = intent.getBooleanExtra("isFromProPanel", false)
            isFromHome = intent.getBooleanExtra("isFromHome", false)
            isFromEdit = intent.getBooleanExtra("isFromEdit", false)
            isFromSplash = intent.getBooleanExtra(EXTRA_FROM_SPLASH, false)
            Log.w("checkinAPP","fromEditor:$isFromEdit")
            binding.tvPriceInfo.text = "Enjoy faster image generations and unlimitted trending templates access free for 3 days, then \$ per week. Cancel anytime."
//            binding.switchFreeTrial.isChecked = true
            isTrialEnabled = true
//            binding.switchFreeTrial.setOnCheckedChangeListener { _, isChecked ->
//                isTrialEnabled = isChecked
//                updateTrialPresentation()
//            }

            updatePurchaseCardText(priceText = getString(R.string.iap_plan_price_placeholder))
            updateTrialPresentation()

            if (isFromEdit){
                loadInterAds(this)
                binding?.tvContinueAds?.visibility = View.VISIBLE
                checkEditor=isFromEdit
            }

            isBillingClientConnected.value = false
            if (!NetworkUtils.isOnline(this)) {
                binding.textFetchingPrices.text = "No Network Connection, Please Try Again"
            }
            iapConnector = IapManager.getIapConnector(this)
            initInAppListeners()

            val showCrossButtonTime = AiFaceApp.showTimeSubScreenX * 1000 //in milliseconds
            lifecycleScope.launch {
                delay(showCrossButtonTime)
                binding.closeButton.show()
            //    binding?.tvContinueAds?.visibility = View.GONE

            }

           /* if (isFromProPanel){
                binding?.tvContinueAds?.visibility = View.VISIBLE
                binding?.closeButton?.visibility = View.GONE
            }else{
                lifecycleScope.launch {
                    delay(showCrossButtonTime)
                    binding.closeButton.show()
                    binding?.tvContinueAds?.visibility = View.GONE
                }
            }*/

            val pulseAnim = AnimationUtils.loadAnimation(this,R.anim.button_pulse)
            binding.btnContinue.startAnimation(pulseAnim)


         /*   if (isProVersion.value == false && isFromProPanel){
                loadRewardedAd(
                    this,
                    BuildConfig.reward_home_hf,
                    BuildConfig.reward_home,
                    AiFaceApp.isRewardHomeHf,
                    AiFaceApp.isRewardHome
                ) { onLoaded ->
                    //   isRewardLoaded = onLoaded

                }
            }*/

            binding?.tvContinueAds?.setOnClickListener {
                if (isFromEdit){
                    EditFragment.isProClosedForEdit.value = true
                    finish()
                }
            }
        } catch (e: Exception) {
            ToastUtils.showErrorToast(this)
            finish()
        }
    }

    private fun getSource(): String {
        return intent.getStringExtra("iap_source") ?: "unknown_source"
    }
    private fun loadInterAds(activity: FragmentActivity) {
        if (AiFaceApp.isInterEditHf && AiFaceApp.isInterEdit) {
            loadInterHomeHigh(activity) { onLoaded ->
                if (onLoaded) {

                } else {
                    loadInterHome(activity) { onLoaded ->
                        if (onLoaded) {

                        } else {

                        }
                    }
                }
            }

        } else if (AiFaceApp.isInterEdit) {
            loadInterHome(activity) { onLoaded ->
                if (onLoaded) {

                } else {

                }
            }
        }
    }
    private fun initInAppListeners() {
        iapConnector.addBillingClientConnectionListener(object : BillingClientConnectionListener {
            override fun onConnected(status: Boolean, billingResponseCode: Int) {
                isBillingClientConnected.value = status
            }
        })

        iapConnector.addSubscriptionListener(object : SubscriptionServiceListener {
            override fun onSubscriptionRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
                // will be triggered upon fetching owned subscription upon initialization
                Log.d("InAppData", "onSubscriptionPurchased1: $purchaseInfo")
                when (purchaseInfo.sku) {
                    IapManager.skuKeyWeek -> {
                        AdsHelper.updateProVersion(true)
                    }
                    else -> {
                        AdsHelper.updateProVersion(false)
                    }
                }
            }

            override fun onSubscriptionPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
                Log.d("InAppData", "onSubscriptionPurchased22: ${purchaseInfo.sku}")
                FirebaseLogUtils.logEvent("iap_successful", "user purchase iap successful")
                val sharedPreferences = getSharedPreferences("iap_prefs", MODE_PRIVATE)
                val purchaseCount = sharedPreferences.getInt("iap_purchase_count", 0) + 1
                sharedPreferences.edit { putInt("iap_purchase_count", purchaseCount) }
                FirebaseLogUtils.logEvent("iap_success", "purchase success")

                val productPrice = getProductDetailsForSku(purchaseInfo.sku)
                val amountPaid = productPrice?.priceAmount ?: 0.0
                if (amountPaid==0.0) {
                    trackIapEventFBAdjust(ADJUST_SUBSCRIPTION_TRIAL_TOKEN,"Trial Purchased: Free",amountPaid)
                    CoroutineScope(Dispatchers.IO).launch {
                        creditManager.resetProCredits()
                        creditManager.claimWeeklyPremiumCredits()
                    }
                }else{
                    trackIapEventFBAdjust(ADJUST_SUBSCRIPTION_TOKEN,"Weekly Purchased:$amountPaid"+"${productPrice?.priceCurrencyCode}",amountPaid)
                    CoroutineScope(Dispatchers.IO).launch {
                        creditManager.resetProCredits()
                        creditManager.claimWeeklyPremiumCredits()
                    }
                }

                if (!purchaseInfo.isAcknowledged) {

                }
                if (purchaseInfo.sku.contains("free_trial", ignoreCase = true)) {
                    FirebaseLogUtils.logEvent("trial success", "trial"
                    )
                }
                // Fetch ProductDetails based on SKU
                val productDetails = getProductDetailsForSku(purchaseInfo.sku)
                confirmPurchaseWithStore(purchaseInfo, productDetails, purchaseInfo.isAcknowledged)
                when (purchaseInfo.sku) {

                    IapManager.skuKeyWeek -> {
                        AdsHelper.updateProVersion(true)
                    }
                }
                Log.w("checkinAPP","after purchased fromEditor:$isFromEdit")
                Log.w("checkinAPP","after purchased checkEditor:$checkEditor")
                currentSku = purchaseInfo.sku
                if (checkEditor){
                    Log.w("checkinAPP","InApp fromEditor")
                    lifecycleScope.launch {
                        isProVersion.postValue(true)
                        delay(700)
                        EditFragment.isProClosedForEdit.value = true
                        finish()
                    }
                }
                else {
                    Log.d("checkinAPP","InApp Direct from home")
                    startActivity(
                        Intent(
                            this@IAPActivity,
                            MainActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    )
                    finish()
                }
            }

            override fun onPricesUpdated(iapKeyPrices: Map<String, List<DataWrappers.ProductDetails>>) {
                for (product in iapKeyPrices){
                    /*Log.w("InAppData", "onPricesUpdated 0: ${product.value[0].billingPeriod}")
                    Log.d("InAppData", "onPricesUpdated 1: ${product.value[1].billingPeriod}")*/
                    for (productDetail in product.value) {
                        if (productDetail!=null)
                            subscribeUi(productDetail,if(product.value[0].billingPeriod=="P3D") true else false)
                        else {
                            binding.textFetchingPrices.text = "Unable to fetch plan details"
                            binding.textFetchingPrices.visibility=View.VISIBLE
                            binding.btnContinue.visibility= View.INVISIBLE
                        }
                    }
                }
            }

        })

        isBillingClientConnected.observe(this) { connected ->
            when (connected) {
                true -> {
                    selectContinue()
                }

                else -> {
                    unSelectContinue()
                }
            }
        }
    }

    fun getProductDetailsForSku(sku: String): DataWrappers.ProductDetails? {
        return availableProductDetails[sku] // Ensure `availableProductDetails` is populated before calling this function
    }

    fun confirmPurchaseWithStore(purchaseInfo: DataWrappers.PurchaseInfo,  productDetails: DataWrappers.ProductDetails?, success: Boolean, errorMessage: String? = null, errorCode: Int? = null) {
        if (success) {
            val price = productDetails?.priceAmount ?: 0.0  // Get actual price, default to 0.0 if unavailable
            val currency = productDetails?.priceCurrencyCode ?: "USD"  // Get actual currency, default to USD

            val tokenParts = purchaseInfo.purchaseToken.split(".")
            val tokenPart1 = tokenParts.getOrNull(0) ?: ""
            val tokenPart2 = tokenParts.getOrNull(1) ?: ""


        } else {

        }
    }

    private fun selectContinue() {
        binding.btnContinue.isEnabled = true
        binding.btnContinue.setOnClickListener {
            FirebaseLogUtils.logEvent("btn_continue_clicked_iap",""
            )
            iapConnector.subscribe(this, IapManager.skuKeyWeek)

        }
    }

    private fun unSelectContinue() {
        binding.btnContinue.isEnabled = false
    }

    private fun onClickListeners() {
        binding.closeButton.setOnClickListener {
            if (isFromEdit) {
                finish()
            } else if (isFromProPanel) {
                finish()
            } else if (isFromSplash) {
                // Splash → IAP → Main
                if (isOnboard) {
                    if (isLanguage) {
                        startActivity(Intent(this@IAPActivity, LanguageActivity::class.java))
                    } else if (survey_enable) {
                        startActivity(Intent(this@IAPActivity, SurveyActivity::class.java))
                    } else {
                        startActivity(Intent(this@IAPActivity, FullOnboardActivity::class.java))
                    }
                    finish()
                }
                else{
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            } else if (isOnboard) {
                if (isLanguage) {
                    startActivity(Intent(this@IAPActivity, LanguageActivity::class.java))
                } else if (survey_enable) {
                    startActivity(Intent(this@IAPActivity, SurveyActivity::class.java))
                } else {
                    startActivity(Intent(this@IAPActivity, FullOnboardActivity::class.java))
                }
                finish()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        binding.tvPrivacy.setOnClickListener {
          privacyPolicy()
        }
//        binding.textCancelAnytime.setOnClickListener {
//            try {
//                iapConnector.unsubscribe(this, skuKeyWeekNew)
//            } catch (e: Exception) {
//                ToastUtils.showErrorToast(this)
//            }
//        }
        binding.tvTerms.setOnClickListener {
            try {
             termsOfServices()
            } catch (e: Exception) {
                ToastUtils.showErrorToast(this)
            }
        }
    }

    private fun subscribeUi(product: DataWrappers.ProductDetails, isTrial: Boolean = false) {
        if (product == null) {
            binding.tvPriceInfo.text = "Unable to fetch plan details"
//            binding.tvPriceInfo.show()
            binding.btnContinue.invisible()
            binding.tvNoPaymentNow.visibility = View.GONE
        } else {
            loadedProduct = product
            availableProductDetails[product.productId.ifBlank { IapManager.skuKeyWeek }] = product
            isTrialOfferAvailable = isTrial

            if (!trialToggleInitialized) {
                trialToggleInitialized = true
                isTrialEnabled = isTrial
//                binding.switchFreeTrial.setCheckedSilently(isTrial, animate = false)
            }
            binding.tvPriceInfo.text="Enjoy faster image generations and unlimitted trending templates free for 3 days, then ${loadedProduct?.priceCurrencyCode ?: "$"} ${loadedProduct?.priceAmount ?: "--"} per week. Cancel anytime."
//            binding.tvPriceInfo.show()
//            binding.cardFreeTrial.visibility = if (isTrialOfferAvailable) View.VISIBLE else View.GONE
            binding.btnContinue.show()
            binding.textFetchingPrices.visibility = View.GONE
            updatePurchaseCardText(priceText = product.price.orEmpty())
            updateTrialPresentation()
        }
    }

    private fun updatePurchaseCardText(priceText: String) {
        binding.tvEnablePurchase.setTwoLineStyledText(
            topLine = getString(R.string.iap_plan_weekly),
            bottomLine = "${loadedProduct?.priceCurrencyCode?:"$"} ${loadedProduct?.priceAmount?:"--"}",
            topColor = ContextCompat.getColor(this, R.color.text_primary),
            bottomColor = ContextCompat.getColor(this, R.color.text_secondary),
            topTextSizeSp = 13f,
            bottomTextSizeSp = 11f,
        )
    }

    private fun updatePurchaseBadge(trialActive: Boolean) {
        binding.tvPurchaseBadge.visibility = View.VISIBLE
        if (trialActive) {
            binding.tvPurchaseBadge.setTwoLineStyledText(
                topLine = getString(R.string.iap_trial_enabled),
                bottomLine = getString(R.string.iap_no_payment_now),
                topColor = ContextCompat.getColor(this, R.color.text_primary),
                bottomColor = ContextCompat.getColor(this, R.color.text_secondary),
                topTextSizeSp = 12f,
                bottomTextSizeSp = 10f,
            )
            binding.tvPriceInfo.text="Enjoy faster image generations and unlimitted trending templates free for 3 days, then ${loadedProduct?.priceCurrencyCode ?: "$"} ${loadedProduct?.priceAmount ?: "--"} per week. Cancel anytime."

        } else {
            binding.tvPurchaseBadge.setTwoLineStyledText(
                topLine = getString(R.string.iap_trial_disabled),
                bottomLine = getString(R.string.iap_badge_cancel_anytime),
                topColor = ContextCompat.getColor(this, R.color.text_primary),
                bottomColor = ContextCompat.getColor(this, R.color.text_secondary),
                topTextSizeSp = 12f,
                bottomTextSizeSp = 10f,
            )
            binding.tvPriceInfo.text="You have  the apportunity to take the premium " +
                    "${loadedProduct?.priceCurrencyCode ?: "$"} ${loadedProduct?.priceAmount ?: "--"} per week. Cancel anytime"

        }
    }

    private fun updateTrialPresentation() {
        val product = loadedProduct
        val priceLabel = product?.price?.takeIf { it.isNotBlank() }
            ?: getString(R.string.iap_plan_price_placeholder).substringBefore(" /")
        val trialActive = isTrialEnabled

        if (product != null) {
            updatePurchaseCardText(priceText = product.price.orEmpty())
        }

        binding.tvDisclaimer.text = if (trialActive) {
            getString(R.string.iap_disclaimer_trial_enableds, priceLabel)
        } else {
            getString(R.string.iap_disclaimer_trial_disableds)
        }

        binding.btnContinue.text = if (trialActive) {
            getString(R.string.iap_continue_for_free)
        } else {
            getString(R.string.iap_continue)
        }

        binding.tvNoPaymentNow.text = if (trialActive) {
            getString(R.string.iap_no_payment_now)
        } else {
            getString(R.string.iap_cancel_anytime)
        }
        binding.tvNoPaymentNow.visibility = View.VISIBLE

        updatePurchaseBadge(trialActive)
    }

    companion object {
        private const val IAP_ENABLED = false

        const val EXTRA_FROM_SPLASH = "extra_from_splash"
        var billingMessage = ""
        var billingCode = -1
        var checkEditor=false

    }

    override fun onBackPressed() {
        if (isFromSplash) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        super.onBackPressed()
        if(SplashActivity.isOnboard){
            if(isLanguage) {
                startActivity(Intent(this@IAPActivity, LanguageActivity::class.java))
            }
            else{
                if(survey_enable) {
                    startActivity(
                        Intent(
                            this@IAPActivity,
                            SurveyActivity::class.java
                        )
                    )
                }
                else{
                    startActivity(
                        Intent(
                            this@IAPActivity,
                            FullOnboardActivity::class.java
                        )
                    )
                }
            }
            finish()
        }
        else{
            if (isFromEdit){
                finish()
            }else{
                startActivity(Intent(this, FullOnboardActivity::class.java))
                finish()
            }

        }
    }

   /* private fun showRewardedAd(){
        if (rewardedAd != null) {

            try {
                showRewardedWithAd(this, {}, {
                    Toast.makeText(
                        this,
                        "no ad available, please try again",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadRewardedAd(
                        this,
                        BuildConfig.reward_home_hf,
                        BuildConfig.reward_home,
                        AiFaceApp.isRewardHomeHf,
                        AiFaceApp.isRewardHome
                    ) { onLoaded ->
                        //   isRewardLoaded = onLoaded

                    }

                }, {
                    isRewarded = true
                    isShowingAd = false

                    finish()
                    if (isFromHome){
                        HomeFragment.isProClosedAfterReward.value = true
                    }else{
                        SeeAllFragment.isProClosedAfterRewardForSeeAll.value = true
                    }


                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Ad not loaded, Please try again", Toast.LENGTH_SHORT).show()
            loadRewardedAd(
                this,
                BuildConfig.reward_home_hf,
                BuildConfig.reward_home,
                AiFaceApp.isRewardHomeHf,
                AiFaceApp.isRewardHome
            ) { onLoaded ->
                //   isRewardLoaded = onLoaded

            }
        }
    }


    fun showRewardedWithAd(
        activity: FragmentActivity,
        onReward: (reward: RewardItem) -> Unit,
        onFailed: () -> Unit,
        onDismissed: () -> Unit
    ) {
        if (!AdsHelper.shouldShowAds() || !NetworkUtils.isOnline(activity)) {
            onDismissed(); return
        }

        fun actuallyShow(ad: RewardedAd) {
            activity.lifecycleScope.launch {
                try {
                    GlobalLoader.show(activity)
                    // Optional tiny delay to let loader render
                    delay(300)

                    var rewardGiven = false

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            isShowingAd = true
                            ad?.onPaidEventListener =
                                OnPaidEventListener { adValue ->
                                    adjustRevenueMMP(
                                        ad?.adUnitId,
                                        adValue.valueMicros / 1_000_000.0,
                                        adValue.currencyCode,
                                        "",
                                        "Rewarded Ad"
                                    )
                                }
                            activity.lifecycleScope.launch {
                                delay(1200)
                                GlobalLoader.hide(activity)
                            }

                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            GlobalLoader.hide(activity)
                            rewardedAd = null
                            isShowingAd = false
                            onFailed()
                        }

                        override fun onAdDismissedFullScreenContent() {

                            rewardedAd = null
                            isShowingAd = false
                            GlobalLoader.hide(activity)
                            onDismissed()
                            loadRewardedAd(
                                activity,
                                BuildConfig.reward_home_hf,
                                BuildConfig.reward_home,
                                AiFaceApp.isRewardHomeHf,
                                AiFaceApp.isRewardHome
                            ) { onLoaded ->
                                // isRewardLoaded = onLoaded

                            }
                        }
                    }

                    ad.show(activity) { rewardItem ->
                        rewardGiven = true
                        onReward(rewardItem)
                        // You can also navigate/unlock here, or defer to onDismiss if you prefer
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        rewardedAd?.let { ad ->
            actuallyShow(ad)
            rewardedAd = null
            return
        }
    }*/
}