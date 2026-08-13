package com.aiface.aging.utils.permission

import android.content.Context
import android.os.Build
import android.provider.Settings

object PermissionStateChecker {

    fun hasOverlayPermission(context: Context): Boolean {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Android does NOT expose a public API to check this.
     * We treat it as "best effort" and confirm by behavior later.
     */
    fun hasFullScreenIntentPermission(): Boolean {
        return true
    }
}
