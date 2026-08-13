package com.aiface.aging.shared.ads

import com.aiface.aging.ads_nextgen.AppOpenAdManager

/**
 * Compatibility facade. Resume app-open is handled by [AppOpenAdManager].
 */
class AppOpenManager {
    companion object {
        @JvmStatic
        var disableAppOpen: Boolean
            get() = AppOpenAdManager.disableAppOpen
            set(value) {
                AppOpenAdManager.disableAppOpen = value
            }

        @JvmStatic
        fun suppressForSystemUi() = AppOpenAdManager.suppressForSystemUi()

        /** Call before opening system Settings for permissions / overlay. */
        @JvmStatic
        fun suppressForSettings() = AppOpenAdManager.suppressForSettings()
    }
}
