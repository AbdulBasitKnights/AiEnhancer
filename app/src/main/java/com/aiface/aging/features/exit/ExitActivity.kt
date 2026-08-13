package com.aiface.aging.features.exit

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.AiFaceApp
import com.aiface.aging.BuildConfig
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.ads_nextgen.ProductAnalytics
import com.aiface.aging.databinding.ActivityExitBinding
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.closeAppCompletely
import com.aiface.aging.shared.goUTM
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.utils.FirebaseLogUtils

class ExitActivity : AppCompatActivity() {

    private var binding: ActivityExitBinding? = null
    private var nativeExitAd: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityExitBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        applyLightSystemBars()

        FirebaseLogUtils.logEvent(ProductAnalytics.SCREEN_EXIT, "")
        setupButtons()
        setupSponsoredApps()
        setupBackPress()
        loadNativeAd()
    }

    private fun setupButtons() {
        binding?.btnCancel?.setOnClickListener { finish() }
        binding?.btnExit?.setOnClickListener {
            closeAppCompletely()
        }
    }

    private fun setupSponsoredApps() {
        val sponsored = listOf(
            SponsoredApp(R.drawable.zm_player, R.string.exit_sponsored_zm_package),
            SponsoredApp(R.drawable.vc_changer, R.string.exit_sponsored_vc_package),
            SponsoredApp(R.drawable.pixel_ai, R.string.exit_sponsored_lab_package),
        )
        val iconViews = listOf(
            binding?.appZm?.ivAppIcon,
            binding?.appVc?.ivAppIcon,
            binding?.pixel?.ivAppIcon,
        )
        sponsored.forEachIndexed { index, app ->
            val iconView = iconViews.getOrNull(index) ?: return@forEachIndexed
            iconView.setImageResource(app.iconRes)
            val packageName = getString(app.packageNameRes).trim()
            val clickTarget = iconView.parent as? View ?: iconView
            clickTarget.setOnClickListener {
                if (packageName.isNotEmpty()) {
                    goUTM(packageName)
                }
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun loadNativeAd() {
        val hostBinding = binding ?: return
        if (!AdsHelper.shouldShowAds() || !AiFaceApp.exit_native) {
            hostBinding.clNativeAd.visibility = View.GONE
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = hostBinding.clNativeAd,
                shimmerWrapper = hostBinding.shimmer,
                nativeContainer = hostBinding.nativeAdView,
            )
            return
        }

        hostBinding.clNativeAd.visibility = View.VISIBLE
        AdShimmerHelper.showLayoutNativePlaceholder(
            adSlot = hostBinding.clNativeAd,
            shimmerWrapper = hostBinding.shimmer,
            nativeContainer = hostBinding.nativeAdView,
        )

        NextGenNativeLoader.loadWithFallback(
            tryHigh = AiFaceApp.exit_native,
            highUnitId = BuildConfig.native_home,
            normalUnitId = BuildConfig.native_home,
            onLoaded = { ad, unitId ->
                if (isFinishing || isDestroyed || binding == null) {
                    ad.destroy()
                    return@loadWithFallback
                }
                nativeExitAd?.destroy()
                nativeExitAd = ad
                val container = binding?.nativeAdView ?: run {
                    ad.destroy()
                    return@loadWithFallback
                }
                NativeAdDisplayHelper.display(
                    container = container,
                    inflater = layoutInflater,
                    nativeAd = ad,
                    onDestroyPrevious = {},
                    adUnitId = unitId,
                    layoutResId = R.layout.layout_native_ads_without_mediaview_b,
                    shimmer = binding?.shimmer,
                )
            },
            onFailed = {
                AdShimmerHelper.hideNativeAdSlot(
                    adSlot = binding?.clNativeAd,
                    shimmerWrapper = binding?.shimmer,
                    nativeContainer = binding?.nativeAdView,
                )
            },
        )
    }

    override fun onDestroy() {
        nativeExitAd?.destroy()
        nativeExitAd = null
        binding = null
        super.onDestroy()
    }

    private data class SponsoredApp(
        val iconRes: Int,
        val packageNameRes: Int,
    )
}
