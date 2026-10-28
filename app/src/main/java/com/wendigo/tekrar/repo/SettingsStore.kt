package com.wendigo.tekrar.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tekrarDataStore by preferencesDataStore(name = "tekrar_settings")

data class AppSettings(
    val newPerDay: Int = SettingsStore.DEFAULT_NEW_PER_DAY,
    val dailyGoal: Int = SettingsStore.DEFAULT_DAILY_GOAL,
    val desiredRetention: Int = SettingsStore.DEFAULT_RETENTION,
    val themeMode: Int = THEME_SYSTEM,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 1.0f,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
) {
    val retentionFraction: Double get() = desiredRetention.coerceIn(70, 97) / 100.0

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }
}

class SettingsStore(context: Context) {

    private val appContext = context.applicationContext

    private object Keys {
        val newPerDay = intPreferencesKey("new_per_day")
        val dailyGoal = intPreferencesKey("daily_goal")
        val desiredRetention = intPreferencesKey("desired_retention")
        val themeMode = intPreferencesKey("theme_mode")
        val autoSpeak = booleanPreferencesKey("auto_speak")
        val speechRate = floatPreferencesKey("speech_rate")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
    }

    val settings: Flow<AppSettings> = appContext.tekrarDataStore.data.map { prefs ->
        AppSettings(
            newPerDay = (prefs[Keys.newPerDay] ?: DEFAULT_NEW_PER_DAY)
                .coerceIn(MIN_NEW_PER_DAY, MAX_NEW_PER_DAY),
            dailyGoal = (prefs[Keys.dailyGoal] ?: DEFAULT_DAILY_GOAL)
                .coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL),
            desiredRetention = (prefs[Keys.desiredRetention] ?: DEFAULT_RETENTION)
                .coerceIn(MIN_RETENTION, MAX_RETENTION),
            themeMode = (prefs[Keys.themeMode] ?: AppSettings.THEME_SYSTEM)
                .coerceIn(AppSettings.THEME_SYSTEM, AppSettings.THEME_DARK),
            autoSpeak = prefs[Keys.autoSpeak] ?: true,
            speechRate = (prefs[Keys.speechRate] ?: 1.0f).coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE),
            reminderEnabled = prefs[Keys.reminderEnabled] ?: false,
            reminderHour = (prefs[Keys.reminderHour] ?: 20).coerceIn(0, 23),
            reminderMinute = (prefs[Keys.reminderMinute] ?: 0).coerceIn(0, 59),
        )
    }

    val newPerDay: Flow<Int> = settings.map { it.newPerDay }

    suspend fun setNewPerDay(value: Int) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.newPerDay] = value.coerceIn(MIN_NEW_PER_DAY, MAX_NEW_PER_DAY)
        }
    }

    suspend fun setDailyGoal(value: Int) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.dailyGoal] = value.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)
        }
    }

    suspend fun setDesiredRetention(value: Int) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.desiredRetention] = value.coerceIn(MIN_RETENTION, MAX_RETENTION)
        }
    }

    suspend fun setThemeMode(value: Int) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.themeMode] =
                value.coerceIn(AppSettings.THEME_SYSTEM, AppSettings.THEME_DARK)
        }
    }

    suspend fun setAutoSpeak(value: Boolean) {
        appContext.tekrarDataStore.edit { prefs -> prefs[Keys.autoSpeak] = value }
    }

    suspend fun setSpeechRate(value: Float) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.speechRate] = value.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.reminderEnabled] = enabled
            prefs[Keys.reminderHour] = hour.coerceIn(0, 23)
            prefs[Keys.reminderMinute] = minute.coerceIn(0, 59)
        }
    }

    companion object {
        const val DEFAULT_NEW_PER_DAY = 20
        const val MIN_NEW_PER_DAY = 0
        const val MAX_NEW_PER_DAY = 200

        const val DEFAULT_DAILY_GOAL = 20
        const val MIN_DAILY_GOAL = 5
        const val MAX_DAILY_GOAL = 200

        const val DEFAULT_RETENTION = 90
        const val MIN_RETENTION = 70
        const val MAX_RETENTION = 97

        const val MIN_SPEECH_RATE = 0.6f
        const val MAX_SPEECH_RATE = 1.4f
    }
}
