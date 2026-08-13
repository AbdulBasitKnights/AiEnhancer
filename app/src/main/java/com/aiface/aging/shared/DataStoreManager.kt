package com.aiface.aging.shared

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.let
import kotlin.toString

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val externalScope: CoroutineScope
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(DATA_STORE_NAME,
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ))

    private suspend fun <T> DataStore<Preferences>.getFromLocalStorage(PreferencesKey: Preferences.Key<T>, value: T, func: T.() -> Unit) {
        data.catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }.map {
            it[PreferencesKey]?: value
        }.collect {
            it?.let { func.invoke(it as T) }
        }
    }

    private suspend fun <T> storeValue(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit {
            it[key] = value
        }
    }

    private suspend fun <T> readValue(key: Preferences.Key<T>, value: T, responseFunc: T.() -> Unit) {
        context.dataStore.getFromLocalStorage(key, value) {
            responseFunc.invoke(this)
        }
    }

    fun <T> readDataStoreValue(key: Preferences.Key<T>, defaultValue: T, onCompleted: T.() -> Unit) {
        externalScope.launch {
            readValue(key, defaultValue) {
                if (this == null) {
                    writeDataStoreValue(key, defaultValue)
                } else {
                    onCompleted.invoke(this)
                }
            }
        }
    }

    fun <T> writeDataStoreValue(key: Preferences.Key<T>, value: T) {
        externalScope.launch {
            storeValue(key, value)
            Log.e("Languageset",value.toString())
        }
    }
}