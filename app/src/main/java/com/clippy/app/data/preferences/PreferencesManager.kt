package com.clippy.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clippy_preferences")

/**
 * Manager for app preferences using DataStore.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        // Preference keys
        private val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        private val KEY_SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        private val KEY_MAX_HISTORY_SIZE = intPreferencesKey("max_history_size")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        
        // Default values
        const val DEFAULT_SERVICE_ENABLED = true
        const val DEFAULT_SHOW_NOTIFICATION = true
        const val DEFAULT_MAX_HISTORY_SIZE = 100
        const val DEFAULT_FIRST_LAUNCH = true
    }
    
    /**
     * Whether the clipboard monitoring service is enabled.
     */
    val serviceEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SERVICE_ENABLED] ?: DEFAULT_SERVICE_ENABLED
    }
    
    suspend fun setServiceEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SERVICE_ENABLED] = enabled
        }
    }
    
    /**
     * Whether to show the persistent notification.
     */
    val showNotification: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SHOW_NOTIFICATION] ?: DEFAULT_SHOW_NOTIFICATION
    }
    
    suspend fun setShowNotification(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_NOTIFICATION] = show
        }
    }
    
    /**
     * Maximum number of clipboard items to keep in history.
     */
    val maxHistorySize: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_MAX_HISTORY_SIZE] ?: DEFAULT_MAX_HISTORY_SIZE
    }
    
    suspend fun setMaxHistorySize(size: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_MAX_HISTORY_SIZE] = size.coerceIn(10, 1000)
        }
    }
    
    /**
     * Whether this is the first launch of the app.
     */
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_FIRST_LAUNCH] ?: DEFAULT_FIRST_LAUNCH
    }
    
    suspend fun setFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[KEY_FIRST_LAUNCH] = false
        }
    }
    
    /**
     * Get current max history size synchronously (for service use).
     */
    suspend fun getMaxHistorySizeSync(): Int {
        var size = DEFAULT_MAX_HISTORY_SIZE
        dataStore.data.collect { preferences ->
            size = preferences[KEY_MAX_HISTORY_SIZE] ?: DEFAULT_MAX_HISTORY_SIZE
        }
        return size
    }
}