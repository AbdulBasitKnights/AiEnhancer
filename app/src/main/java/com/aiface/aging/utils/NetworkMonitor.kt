package com.aiface.aging.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService

object NetworkMonitor {

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isRegistered = false

    private val listeners = mutableSetOf<(Boolean) -> Unit>()
    private var lastKnownState: Boolean? = null

    fun init(context: Context) {
        if (isRegistered) return

        connectivityManager = context.applicationContext.getSystemService()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val isConnected =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                notifyState(isConnected)
            }

            override fun onLost(network: Network) {
                notifyState(false)
            }

            override fun onUnavailable() {
                notifyState(false)
            }
        }

        networkCallback = callback
        connectivityManager?.registerDefaultNetworkCallback(callback)
        isRegistered = true

        notifyState(isInternetAvailable())
    }

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
        lastKnownState?.let { listener(it) }
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    fun isInternetAvailable(): Boolean {
        val cm = connectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun notifyState(isConnected: Boolean) {
        if (lastKnownState == isConnected) return
        lastKnownState = isConnected
        listeners.forEach { it.invoke(isConnected) }
    }
}