package com.wendigo.tekrar

import android.app.Application
import android.content.Context
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.notify.Notifications
import com.wendigo.tekrar.notify.ReminderScheduler
import com.wendigo.tekrar.notify.WeeklySummaryScheduler
import com.wendigo.tekrar.repo.BackupManager
import com.wendigo.tekrar.repo.DictionaryRepository
import com.wendigo.tekrar.repo.SettingsStore
import com.wendigo.tekrar.repo.WordRepository
import com.wendigo.tekrar.widget.DueWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    val repository: WordRepository by lazy {
        WordRepository(appContext, database, settings)
    }
    val backupManager: BackupManager by lazy {
        BackupManager(appContext, database, settings)
    }
    val dictionary: DictionaryRepository by lazy {
        DictionaryRepository(appContext)
    }

    fun start() {
        Notifications.ensureChannel(appContext)
        Notifications.ensureWeeklyChannel(appContext)
        DueWidgetProvider.refresh(appContext)
        scope.launch {
            runCatching {
                repository.seedIfEmpty()
                repository.backfillExampleTr()
            }
            DueWidgetProvider.refresh(appContext)
            runCatching { repository.syncStreakFreezes() }
            runCatching {
                val current = settings.settings.first()
                if (current.reminderEnabled) {
                    ReminderScheduler.schedule(
                        appContext,
                        current.reminderHour,
                        current.reminderMinute,
                    )
                }
                if (current.weeklySummaryEnabled) {
                    WeeklySummaryScheduler.schedule(appContext)
                } else {
                    WeeklySummaryScheduler.cancel(appContext)
                }
            }
        }
    }
}

class TekrarApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
