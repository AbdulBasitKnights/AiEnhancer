package com.aiface.aging.shared.ads


import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.domain.model.Template

sealed class HomeChildItem {

    data class TemplateItem(
        val template: Template
    ) : HomeChildItem()

    data class NativeAdItem(
        val nativeAd: NativeAd
    ) : HomeChildItem()
}