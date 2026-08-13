package com.aiface.aging.ads_nextgen

import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration

/**
 * Preload buffer sizing.
 *
 * Buffer 1 only — never hold two ready interstitials.
 * After poll into [interstitialHome], preload is destroyed until ad is shown,
 * then restarted for the next single fill.
 */
object AdPreloadConfigFactory {

    private const val BUFFER_SIZE = 1

    fun create(request: AdRequest): PreloadConfiguration {
        return PreloadConfiguration(request, bufferSize = BUFFER_SIZE)
    }

    fun defaultBufferSizeLabel(): String = BUFFER_SIZE.toString()
}
