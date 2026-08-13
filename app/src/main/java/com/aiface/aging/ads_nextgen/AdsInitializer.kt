package com.aiface.aging.ads_nextgen

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Next-Gen SDK init.
 *
 * Important: [MobileAds.initialize] returns quickly. The completion listener waits for
 * ALL mediation adapters (or Google's **30s timeout**). We mark ready when initialize()
 * returns — Google's recommended path for not blocking on slow adapters.
 *
 * @see <a href="https://developers.google.com/admob/android/next-gen/quick-start">Quick start</a>
 */
class AdsInitializer(private val application: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** Seconds until SDK call returned (ready to load ads). */
    private val _initDurationSeconds = MutableStateFlow<Double?>(null)
    val initDurationSeconds: StateFlow<Double?> = _initDurationSeconds.asStateFlow()

    /** One-shot: SDK ready to load ads (seconds). */
    private val _initCompletedEvent = MutableSharedFlow<Double>(extraBufferCapacity = 1)
    val initCompletedEvent: SharedFlow<Double> = _initCompletedEvent.asSharedFlow()

    /** One-shot: mediation adapters finished or hit 30s timeout (seconds). */
    private val _adaptersCompletedEvent = MutableSharedFlow<Double>(extraBufferCapacity = 1)
    val adaptersCompletedEvent: SharedFlow<Double> = _adaptersCompletedEvent.asSharedFlow()

    @Volatile
    private var initStarted = false

    val isReady: Boolean get() = _isInitialized.value

    fun initialize(onComplete: () -> Unit = {}) {
        if (_isInitialized.value) {
            mainHandler.post(onComplete)
            return
        }
        if (initStarted) {
            runWhenInitialized(onComplete)
            return
        }

        initStarted = true
        val startMs = System.currentTimeMillis()
        Log.d(TAG, "SDK init started")

        scope.launch {
            // Callback = adapters done OR 30s timeout. Do NOT block ad loading on this.
            MobileAds.initialize(
                application,
                InitializationConfig.Builder(AdConstants.APP_ID).build()
            ) { status ->
                val adapterSeconds = (System.currentTimeMillis() - startMs) / 1000.0
                status.adapterStatusMap.forEach { (name, adapterStatus) ->
                    Log.d(
                        TAG,
                        "Adapter $name -> ${adapterStatus.initializationState}: ${adapterStatus.description}"
                    )
                }
                Log.d(TAG, "Adapters callback in ${"%.2f".format(adapterSeconds)}s")
                _adaptersCompletedEvent.tryEmit(adapterSeconds)
            }

            // Google: "SDK initialization is complete. If you don't want to wait for
            // bidding adapters to finish initializing, start loading ads now."
            val seconds = (System.currentTimeMillis() - startMs) / 1000.0
            _initDurationSeconds.value = seconds
            _isInitialized.value = true
            _initCompletedEvent.tryEmit(seconds)
            Log.d(TAG, "SDK ready to load ads in ${"%.2f".format(seconds)}s")
            mainHandler.post(onComplete)
        }
    }

    fun runWhenInitialized(block: () -> Unit) {
        if (_isInitialized.value) {
            mainHandler.post(block)
            return
        }
        if (!initStarted) {
            initialize(block)
            return
        }
        scope.launch {
            isInitialized.first { it }
            mainHandler.post(block)
        }
    }

    companion object {
        private const val TAG = "AdsInitializer"
    }
}
