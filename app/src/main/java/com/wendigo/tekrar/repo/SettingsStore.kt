package com.wendigo.tekrar.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wendigo.tekrar.core.BackupSettings
import com.wendigo.tekrar.core.StreakFreeze
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.tekrarDataStore by preferencesDataStore(name = "tekrar_settings")

data class AppSettings(
    val newPerDay: Int = SettingsStore.DEFAULT_NEW_PER_DAY,
    val dailyGoal: Int = SettingsStore.DEFAULT_DAILY_GOAL,
    val weeklyGoalDays: Int = SettingsStore.DEFAULT_WEEKLY_GOAL_DAYS,
    val desiredRetention: Int = SettingsStore.DEFAULT_RETENTION,
    val themeMode: Int = THEME_SYSTEM,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 1.0f,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val seedApplied: Boolean = false,
    val streakFreezes: Int = 0,
    val lastFreezeMilestone: Int = 0,
    val frozenDays: Set<LocalDate> = emptySet(),
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
        val weeklyGoalDays = intPreferencesKey("weekly_goal_days")
        val desiredRetention = intPreferencesKey("desired_retention")
        val themeMode = intPreferencesKey("theme_mode")
        val autoSpeak = booleanPreferencesKey("auto_speak")
        val speechRate = floatPreferencesKey("speech_rate")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val seedApplied = booleanPreferencesKey("seed_applied")
        val streakFreezes = intPreferencesKey("streak_freezes")
        val lastFreezeMilestone = intPreferencesKey("last_freeze_milestone")
        val frozenDays = stringSetPreferencesKey("frozen_days")
    }

    val settings: Flow<AppSettings> = appContext.tekrarDataStore.data.map { prefs ->
        AppSettings(
            newPerDay = (prefs[Keys.newPerDay] ?: DEFAULT_NEW_PER_DAY)
                .coerceIn(MIN_NEW_PER_DAY, MAX_NEW_PER_DAY),
            dailyGoal = (prefs[Keys.dailyGoal] ?: DEFAULT_DAILY_GOAL)
                .coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL),
            weeklyGoalDays = (prefs[Keys.weeklyGoalDays] ?: DEFAULT_WEEKLY_GOAL_DAYS)
                .coerceIn(MIN_WEEKLY_GOAL_DAYS, MAX_WEEKLY_GOAL_DAYS),
            desiredRetention = (prefs[Keys.desiredRetention] ?: DEFAULT_RETENTION)
                .coerceIn(MIN_RETENTION, MAX_RETENTION),
            themeMode = (prefs[Keys.themeMode] ?: AppSettings.THEME_SYSTEM)
                .coerceIn(AppSettings.THEME_SYSTEM, AppSettings.THEME_DARK),
            autoSpeak = prefs[Keys.autoSpeak] ?: true,
            speechRate = (prefs[Keys.speechRate] ?: 1.0f).coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE),
            reminderEnabled = prefs[Keys.reminderEnabled] ?: false,
            reminderHour = (prefs[Keys.reminderHour] ?: 20).coerceIn(0, 23),
            reminderMinute = (prefs[Keys.reminderMinute] ?: 0).coerceIn(0, 59),
            seedApplied = prefs[Keys.seedApplied] ?: false,
            streakFreezes = (prefs[Keys.streakFreezes] ?: 0)
                .coerceIn(0, StreakFreeze.MAX_FREEZES),
            lastFreezeMilestone = (prefs[Keys.lastFreezeMilestone] ?: 0).coerceAtLeast(0),
            frozenDays = prefs[Keys.frozenDays].orEmpty()
                .mapNotNull { it.toLongOrNull() }
                .map { LocalDate.ofEpochDay(it) }
                .toSet(),
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

    suspend fun setWeeklyGoalDays(value: Int) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.weeklyGoalDays] =
                value.coerceIn(MIN_WEEKLY_GOAL_DAYS, MAX_WEEKLY_GOAL_DAYS)
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

    suspend fun setSeedApplied(value: Boolean) {
        appContext.tekrarDataStore.edit { prefs -> prefs[Keys.seedApplied] = value }
    }

    suspend fun setFreezeState(stock: Int, frozenDays: Set<LocalDate>, milestone: Int) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.streakFreezes] = stock.coerceIn(0, StreakFreeze.MAX_FREEZES)
            prefs[Keys.frozenDays] =
                frozenDays.map { it.toEpochDay().toString() }.toSet()
            prefs[Keys.lastFreezeMilestone] = milestone.coerceAtLeast(0)
        }
    }

    suspend fun clearFreezeState() {
        setFreezeState(stock = 0, frozenDays = emptySet(), milestone = 0)
    }

    suspend fun applyAll(settings: BackupSettings) {
        appContext.tekrarDataStore.edit { prefs ->
            prefs[Keys.newPerDay] =
                settings.newPerDay.coerceIn(MIN_NEW_PER_DAY, MAX_NEW_PER_DAY)
            prefs[Keys.dailyGoal] =
                settings.dailyGoal.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)
            prefs[Keys.weeklyGoalDays] =
                settings.weeklyGoalDays.coerceIn(MIN_WEEKLY_GOAL_DAYS, MAX_WEEKLY_GOAL_DAYS)
            prefs[Keys.desiredRetention] =
                settings.desiredRetention.coerceIn(MIN_RETENTION, MAX_RETENTION)
            prefs[Keys.themeMode] =
                settings.themeMode.coerceIn(AppSettings.THEME_SYSTEM, AppSettings.THEME_DARK)
            prefs[Keys.autoSpeak] = settings.autoSpeak
            prefs[Keys.speechRate] =
                settings.speechRate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
            prefs[Keys.reminderEnabled] = settings.reminderEnabled
            prefs[Keys.reminderHour] = settings.reminderHour.coerceIn(0, 23)
            prefs[Keys.reminderMinute] = settings.reminderMinute.coerceIn(0, 59)
        }
    }

    companion object {
        const val DEFAULT_NEW_PER_DAY = 20
        const val MIN_NEW_PER_DAY = 0
        const val MAX_NEW_PER_DAY = 200

        const val DEFAULT_DAILY_GOAL = 20
        const val MIN_DAILY_GOAL = 5
        const val MAX_DAILY_GOAL = 200

        const val DEFAULT_WEEKLY_GOAL_DAYS = 5
        const val MIN_WEEKLY_GOAL_DAYS = 3
        const val MAX_WEEKLY_GOAL_DAYS = 7

        const val DEFAULT_RETENTION = 90
        const val MIN_RETENTION = 70
        const val MAX_RETENTION = 97

        const val MIN_SPEECH_RATE = 0.6f
        const val MAX_SPEECH_RATE = 1.4f
    }
}
