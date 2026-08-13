package com.aiface.aging.utils

import android.content.Context
import android.provider.Settings
import com.aiface.aging.data.local.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.isNullOrEmpty

@Singleton
class DeviceIdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager,
) {
    /**
     * Returns a stable, persisted device ID.
     * Priority: stored ID → Android ID → random UUID fallback.
     * Never regenerates on every call.
     */
    suspend fun getDeviceId(): String {
        val stored = preferenceManager.deviceId.first()
        if (!stored.isNullOrEmpty()) return stored

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        )

        val deviceId = if (!androidId.isNullOrEmpty()) {
            androidId
        } else {
            UUID.randomUUID().toString()
        }

        preferenceManager.saveDeviceId(deviceId)
        return deviceId
    }
}
