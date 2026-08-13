package com.aiface.aging.features.iap

import android.content.Context
import com.aiface.aging.features.iap.utils.IapConnector

object IapManager {
    private var iapConnector: IapConnector? = null

    const val skuKeyWeek = "weekly_pro"
    //licenseKey
    fun getIapConnector(context: Context): IapConnector {
        return if (iapConnector != null) {
            iapConnector as IapConnector
        } else {
            val nonConsumablesList = listOf("lifetime")
            val consumablesList = listOf("base", "moderate", "quite")
            val subsList = listOf(skuKeyWeek)
            IapConnector(
                context = context,
                nonConsumableKeys = nonConsumablesList,
                consumableKeys = consumablesList,
                subscriptionKeys = subsList,
                key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtlEywJNpRAENMed/rNPLSr2bGkzzKZCrQsjuQCMTSHLYq+3CEb0hRx/UtXUSX9KcFv613kPJSEE85me2Oz01fSmNfo19qM/DX2o/IXtPQCdZzN4TRFi5c73OUoddmgW6OTszk+GB+rmUL71bjDSNm8NCy4oqc1IV6vTWWR7CTrVv0UmXGJpqKSlOwbTteh3pZBNVKkloe52bR8lfnZaQ6lSFZPgelHg7GmSVvCrnPwI13xt1DW9/071R90sRuJ+ggWPAWeI5qCLGTNd7XcnDAjQyzXPuEA51GQkGgTsm0CJO+KbhPUjSc0Zk+FhPotbPw7ltcjRCljJyc3zEhXgjPwIDAQAB",
                enableLogging = true
            )
        }
    }
}