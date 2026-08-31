package com.premium.timer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "premium_timer_prefs")

class UserPreferences private constructor(private val context: Context) {
    private object Keys {
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    /** Defaults to true per spec: "Default it to enabled, but allow disabling it." */
    val keepScreenOnFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.KEEP_SCREEN_ON] ?: true }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    companion object {
        @Volatile private var instance: UserPreferences? = null
        fun get(context: Context): UserPreferences =
            instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
    }
}
