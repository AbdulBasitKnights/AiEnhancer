package com.aiface.aging.data.initializer

import android.content.Context
import android.content.SharedPreferences
import android.util.LruCache
import androidx.core.content.edit

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.aiface.aging.data.params.RemoteEnumString
import com.aiface.aging.data.params.RemoteKeys
import kotlin.collections.find
import kotlin.collections.joinToString
import kotlin.collections.mapNotNull
import kotlin.getOrDefault
import kotlin.getOrElse
import kotlin.let
import kotlin.runCatching
import kotlin.takeUnless
import kotlin.text.isNotEmpty
import kotlin.text.isNullOrBlank
import kotlin.text.split
import kotlin.text.toIntOrNull

abstract class BaseRemoteConfiguration {
    private var applicationContext: Context? = null
    private val cacheRemote = LruCache<String, Any>(Int.MAX_VALUE)
    internal abstract fun getPrefsName(): String

    abstract fun sync(remoteConfig: FirebaseRemoteConfig)

    fun init(application: Context) {
        this.applicationContext = application.applicationContext
    }

    val isReady: Boolean
        get() = applicationContext != null

    private fun getPrefs(): SharedPreferences? {
        val context = applicationContext ?: return null
        return context.getSharedPreferences(getPrefsName(), Context.MODE_PRIVATE)
    }

    internal fun FirebaseRemoteConfig.saveToLocal(keyType: RemoteKeys) {
        val prefs = getPrefs() ?: return
        val hasKeyRemote = runCatching {
            this.getString(keyType.remoteKey).isNotEmpty()
        }.getOrDefault(true)
        if (!hasKeyRemote) return
        val remoteConfig = this
        prefs.edit {
            val key = keyType.remoteKey
            when (keyType) {
                is RemoteKeys.BooleanKey -> {
                    putBoolean(
                        key,
                        runCatching {
                            remoteConfig.getBoolean(key)
                        }.getOrElse { keyType.defaultValue }
                    )
                }

                is RemoteKeys.StringKey -> {
                    putString(
                        key,
                        runCatching {
                            remoteConfig.getString(key)
                        }.getOrElse { keyType.defaultValue })
                }

                is RemoteKeys.DoubleKey -> {
                    putFloat(
                        key,
                        runCatching {
                            remoteConfig.getDouble(key)
                        }.getOrElse { keyType.defaultValue }.toFloat()
                    )
                }

                is RemoteKeys.LongKey -> {
                    putLong(
                        key,
                        runCatching {
                            remoteConfig.getLong(key)
                        }.getOrElse { keyType.defaultValue })
                }

                is RemoteKeys.ListIntegerKey -> {
                    putString(key, runCatching {
                        remoteConfig.getString(key)
                    }.getOrElse { keyType.defaultValue.joinToString(",") })
                }

                is RemoteKeys.StringEnumKey<*> -> {
                    putString(
                        key,
                        runCatching {
                            remoteConfig.getString(key)
                        }.getOrElse { keyType.defaultValue.remoteValue })
                }

                else -> Unit
            }
        }
    }

    internal fun RemoteKeys.StringKey.cacheOrGet(): String {
        return runCatching { cacheRemote[this.remoteKey] as String }.getOrElse { get() }
    }

    internal inline fun <reified T> RemoteKeys.StringEnumKey<T>.cacheOrGet(): T where T : RemoteEnumString, T : Enum<T> {
        return runCatching {
            enumValues<T>().find { it.remoteValue == (cacheRemote[remoteKey] as String) }
        }.getOrNull() ?: get()
    }

    internal fun RemoteKeys.BooleanKey.cacheOrGet(): Boolean {
        return runCatching { cacheRemote[remoteKey] as Boolean }.getOrElse { get() }
    }

    internal fun RemoteKeys.LongKey.cacheOrGet(): Long {
        return runCatching { cacheRemote[remoteKey] as Long }.getOrElse { get() }
    }

    internal fun RemoteKeys.ListIntegerKey.cacheOrGet(): List<Int> {
        return runCatching { cacheRemote[remoteKey] as List<Int> }.getOrElse { get() }
    }

    internal fun RemoteKeys.DoubleKey.cacheOrGet(): Double {
        return runCatching { cacheRemote[remoteKey] as Double }.getOrElse { get() }
    }

    internal fun RemoteKeys.StringKey.get(): String {
        val prefs = getPrefs() ?: return defaultValue
        return prefs.getString(remoteKey, defaultValue).takeUnless { it.isNullOrBlank() }
            ?: defaultValue
    }

    internal inline fun <reified T> RemoteKeys.StringEnumKey<T>.get(): T where T : RemoteEnumString, T : Enum<T> {
        val prefs = getPrefs() ?: return defaultValue
        return runCatching {
            val stringValue = prefs.getString(remoteKey, defaultValue.remoteValue)
                .takeUnless { it.isNullOrBlank() } ?: defaultValue.remoteValue
            enumValues<T>().find { it.remoteValue == stringValue } ?: defaultValue
        }.getOrNull() ?: defaultValue
    }

    internal fun RemoteKeys.BooleanKey.get(): Boolean {
        val prefs = getPrefs() ?: return defaultValue
        return prefs.getBoolean(remoteKey, defaultValue)
    }

    internal fun RemoteKeys.LongKey.get(): Long {
        val prefs = getPrefs() ?: return defaultValue
        return prefs.getLong(remoteKey, defaultValue)
    }

    internal fun RemoteKeys.ListIntegerKey.get(): List<Int> {
        val prefs = getPrefs() ?: return defaultValue
        return runCatching {
            prefs.getString(remoteKey, defaultValue.joinToString(","))
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
        }.getOrNull() ?: defaultValue
    }

    internal fun RemoteKeys.DoubleKey.get(): Double {
        val prefs = getPrefs() ?: return defaultValue
        return prefs.getFloat(remoteKey, defaultValue.toFloat()).toDouble()
    }
}
