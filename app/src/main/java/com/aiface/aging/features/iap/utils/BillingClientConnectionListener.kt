package com.aiface.aging.features.iap.utils

interface BillingClientConnectionListener {
    fun onConnected(status: Boolean, billingResponseCode: Int)
}