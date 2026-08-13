package com.aiface.aging.features.imgpicker.base

import android.Manifest
import android.os.Build
import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AppOpenManager


abstract class BaseFragment : Fragment(), EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }


    companion object {
        const val PERMISSION_REQUEST_CODE = 124
    }
    override fun onPause() {
        super.onPause()
        if (!EasyPermissions.hasPermissions(requireContext(), *permissions)){
            //  AppOpenManager.getInstance().disableAdResumeByClickAction()
           // AppOpenManager.getInstance().isDisableAdResumeByClickAction = true
        //   AperoAds.disableAppOpenAd()
        }else{
          // AperoAds.enableAppOpenAd()
        }
    }
    @AfterPermissionGranted(PERMISSION_REQUEST_CODE)
    fun getPermission() {
        com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
            if (!isAdded) return@runWhenAdsClear
            val ctx = context ?: return@runWhenAdsClear
            AppOpenManager.disableAppOpen = true
            if (EasyPermissions.hasPermissions(ctx, *permissions)) {
                if (view != null) {
                    onPermissionsGranted()
                }
            } else {
                EasyPermissions.requestPermissions(
                    this@BaseFragment,
                    getString(R.string.rationale_permissions),
                    PERMISSION_REQUEST_CODE,
                    *permissions
                )
            }
        }
    }

    // Handle permission request result using EasyPermissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    // EasyPermissions.PermissionCallbacks implementation
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (isAdded && view != null) {
            onPermissionsGranted()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (isAdded && view != null) {
            onPermissionsDenied(perms)
        }
    }

    // EasyPermissions.RationaleCallbacks implementation
    override fun onRationaleAccepted(requestCode: Int) {}

    override fun onRationaleDenied(requestCode: Int) {

    }


    // Override these methods in your fragments to handle permissions
    abstract fun onPermissionsGranted()

    abstract fun onPermissionsDenied(deniedPermissions: List<String>)


}