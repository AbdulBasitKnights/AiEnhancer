package com.aiface.aging.data.initializer

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

object RemoteInitializer {
    private val rcFirebase by lazy {
        Firebase.remoteConfig
    }
    private var isInitialized = false
    private var lastFetchAttemptAtMs: Long = 0L

    fun init(context: Context) {
        if (isInitialized) return

        val appContext = context.applicationContext
        RemoteLogicConfiguration.getInstance().init(appContext)
        RemoteAdsConfiguration.getInstance().init(appContext)
        RemoteUiConfiguration.getInstance().init(appContext)
        isInitialized = true
    }

    fun syncWithRemoteConfig(remoteConfig: FirebaseRemoteConfig) {
        if (!isInitialized) return

        RemoteLogicConfiguration.Companion.getInstance().sync(remoteConfig)
        RemoteAdsConfiguration.Companion.getInstance().sync(remoteConfig)
        RemoteUiConfiguration.Companion.getInstance().sync(remoteConfig)
    }

    /**
     * Fetch+activate Remote Config and then sync values into local configuration singletons.
     *
     * Note: Firebase Remote Config can still return cached values depending on server/client state,
     * but with [minimumFetchIntervalInSeconds]=0 it will attempt to refresh on each app reopen.
     */
    fun fetchAndSync(
        force: Boolean = false,
        minimumFetchIntervalSeconds: Long = 0L,
        onComplete: (Boolean) -> Unit = {},
    ) {
        if (!isInitialized) return

        val now = System.currentTimeMillis()
        // Prevent spamming fetch calls if multiple activities start quickly.
        if (!force && (now - lastFetchAttemptAtMs) < 1_000L) return
        lastFetchAttemptAtMs = now

        runCatching {
            val settings =
                FirebaseRemoteConfigSettings
                    .Builder()
                    .setMinimumFetchIntervalInSeconds(minimumFetchIntervalSeconds)
                    .build()
            rcFirebase.setConfigSettingsAsync(settings)
        }.onFailure { e ->
            Log.d("RemoteInitializer", "Failed to set RC settings: ${e.message}")
        }

        rcFirebase.fetchAndActivate().addOnCompleteListener { task ->
            val ok = task.isSuccessful
            if (ok) {
                runCatching { syncWithRemoteConfig(rcFirebase) }
                    .onFailure { e ->
                        Log.d("RemoteInitializer", "Failed to sync RC values: ${e.message}")
                    }
            }
            onComplete(ok)
        }
    }

    suspend fun retrySyncData(
        nextAction: suspend () -> Unit = {},
        onFail: suspend (Exception) -> Unit = {},
    ) {
        try {
            withTimeout(5000L) {
                syncRemoteConfig()
            }
            nextAction()
        } catch (e: Exception) {
            Log.d("RemoteInitializer", "Fail to sync remote config: ${e.message}")
            onFail(e)
        }
    }

    private suspend fun syncRemoteConfig(): Boolean = suspendCancellableCoroutine { continuation ->
        rcFirebase.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    try {
                        syncWithRemoteConfig(rcFirebase)
                        continuation.resume(true)
                    } catch (e: Exception) {
                        continuation.resume(false)
                    }
                } else {
                    continuation.resume(false)
                }
            }
    }
}
