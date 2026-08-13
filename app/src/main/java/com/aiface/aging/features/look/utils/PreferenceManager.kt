package com.aiface.aging.features.look.utils

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lookPreferencesDataStore by preferencesDataStore(name = "look_preferences")

class LookPreferenceManager(private val context: Context) {

    private val dataStore = context.lookPreferencesDataStore

    /** Save a value with a given Preferences.Key */
    suspend fun <T> putValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /** Get a value with a given Preferences.Key */
    fun <T> getValue(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    /** Remove a given key */
    suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    /** Clear all preferences */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

