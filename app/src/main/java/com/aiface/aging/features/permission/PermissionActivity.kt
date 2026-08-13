package com.aiface.aging.features.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.MainActivity
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.shared.DataStoreManager
import com.aiface.aging.shared.IS_LANGUAGE
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.databinding.ActivityPermissionBinding
import com.aiface.aging.features.imgpicker.base.BaseFragment.Companion.PERMISSION_REQUEST_CODE
import com.aiface.aging.utils.FirebaseLogUtils
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity(),EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {
    private var binding: ActivityPermissionBinding? = null

    var nativePermission: NativeAd? = null

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
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()
        applyLightSystemBars()

        FirebaseLogUtils.logEvent("permission_scr_view", "")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (AiFaceApp.isNativePermissionHf && AiFaceApp.isNativePermission) {
            startNative(tryHigh = true)
        } else if (AiFaceApp.isNativePermission) {
            startNative(tryHigh = false)
        } else {
            binding?.clbottom?.visibility = View.GONE
        }

        binding?.btnLater?.setOnClickListener {
            navigateNext(this)
        }
        binding?.btnAllow?.setOnClickListener {
            getPermission()
        }
    }



    private fun navigateNext(currentActivity: FragmentActivity){
        FirebaseLogUtils.logEvent("permission_scr_next", "")
          startActivity(Intent(currentActivity, MainActivity::class.java))
        currentActivity.finish()
    }

    private fun startNative(tryHigh: Boolean) {
        try {
            if (!AdsHelper.shouldShowAds()) {
                binding?.clbottom?.visibility = View.GONE
                AdShimmerHelper.hideNativeAdSlot(
                    adSlot = binding?.clbottom,
                    shimmerWrapper = binding?.shimmer,
                    nativeContainer = binding?.nativeAdView,
                )
                return
            }
            AdShimmerHelper.showLayoutNativePlaceholder(
                adSlot = binding?.clbottom,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_permission_hf,
                normalUnitId = BuildConfig.native_permission,
                onLoaded = { ad, unitId ->
                    try {
                        nativePermission?.destroy()
                        nativePermission = ad
                        val container = binding?.nativeAdView
                        if (container == null) {
                            ad.destroy()
                            AdShimmerHelper.hideNativeAdSlot(
                                adSlot = binding?.clbottom,
                                shimmerWrapper = binding?.shimmer,
                            )
                            return@loadWithFallback
                        }
                        NativeAdDisplayHelper.display(
                            container = container,
                            inflater = layoutInflater,
                            nativeAd = ad,
                            onDestroyPrevious = {},
                            adUnitId = unitId,
                            layoutResId = R.layout.layout_native_ads,
                            shimmer = binding?.shimmer
                        )
                    } catch (t: Throwable) {
                        try {
                            ad.destroy()
                        } catch (_: Throwable) {
                        }
                        AdShimmerHelper.hideNativeAdSlot(
                            adSlot = binding?.clbottom,
                            shimmerWrapper = binding?.shimmer,
                            nativeContainer = binding?.nativeAdView,
                        )
                    }
                },
                onFailed = {
                    AdShimmerHelper.hideNativeAdSlot(
                        adSlot = binding?.clbottom,
                        shimmerWrapper = binding?.shimmer,
                        nativeContainer = binding?.nativeAdView,
                    )
                }
            )
        } catch (t: Throwable) {
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clbottom,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }

    override fun onDestroy() {
        nativePermission?.destroy()
        nativePermission = null
        super.onDestroy()
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
    override fun onPermissionsGranted(
        requestCode: Int,
        perms: List<String?>
    ) {
        navigateNext(this)
    }

    override fun onPermissionsDenied(
        requestCode: Int,
        perms: List<String?>
    ) {
        navigateNext(this)
    }

    override fun onRationaleAccepted(requestCode: Int) {
    }

    override fun onRationaleDenied(requestCode: Int) {
    }



    val permissions =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @AfterPermissionGranted(PERMISSION_REQUEST_CODE)
    fun getPermission() {

        if (EasyPermissions.hasPermissions(this, *permissions)) {
            // onPermissionsGranted()
           navigateNext(this)
        } else {
            // Ask for both
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.rationale_permissions),
                PERMISSION_REQUEST_CODE,
                *permissions
            )
        }
    }
}