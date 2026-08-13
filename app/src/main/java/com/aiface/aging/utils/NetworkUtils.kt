package com.aiface.aging.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresPermission

class NetworkUtils {
    companion object {
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        fun isOnline(context: Context): Boolean {
            try {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivityManager.activeNetwork
                    if (network != null) {
                        val nc = connectivityManager.getNetworkCapabilities(network)
                        return nc!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    }
                } else {
                    val networkInfos = connectivityManager.allNetworkInfo
                    for (tempNetworkInfo in networkInfos) {
                        if (tempNetworkInfo.isConnected) {
                            return true
                        }
                    }
                }
                return false
            } catch (e: Exception) {
                return false
            }
        }
    }
}