package com.wendigo.tekrar.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tekrarDataStore by preferencesDataStore(name = "tekrar_settings")

class SettingsStore(context: Context) {

    private val appContext = context.applicationContext
    private val newPerDayKey = intPreferencesKey("new_per_day")

    val newPerDay: Flow<Int> = appContext.tekrarDataStore.data.map { prefs ->
        prefs[newPerDayKey] ?: DEFAULT_NEW_PER_DAY
    }

    suspend fun setNewPerDay(value: Int) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[newPerDayKey] = value.coerceIn(0, MAX_NEW_PER_DAY)
        }
    }

    companion object {
        const val DEFAULT_NEW_PER_DAY = 20
        const val MAX_NEW_PER_DAY = 200
    }
}
