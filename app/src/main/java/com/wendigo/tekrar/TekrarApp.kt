package com.wendigo.tekrar

import android.app.Application
import android.content.Context
import com.wendigo.tekrar.ai.AiRepository
import com.wendigo.tekrar.ai.AiSettingsStore
import com.wendigo.tekrar.ai.KeystoreAiKeyStore
import com.wendigo.tekrar.ai.OpenAiCompatibleClient
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.notify.Notifications
import com.wendigo.tekrar.notify.ReminderScheduler
import com.wendigo.tekrar.notify.WeeklySummaryScheduler
import com.wendigo.tekrar.repo.BackupManager
import com.wendigo.tekrar.repo.CourseProgressRepository
import com.wendigo.tekrar.repo.CourseRepository
import com.wendigo.tekrar.repo.DictionaryRepository
import com.wendigo.tekrar.repo.GrammarProgressRepository
import com.wendigo.tekrar.repo.GrammarRepository
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
    val courseRepository: CourseRepository by lazy {
        CourseRepository(appContext, database)
    }
    val courseProgress: CourseProgressRepository by lazy {
        CourseProgressRepository(database)
    }
    val grammarRepository: GrammarRepository by lazy {
        GrammarRepository(appContext)
    }
    val grammarProgress: GrammarProgressRepository by lazy {
        GrammarProgressRepository(courseProgress)
    }
    val aiSettings: AiSettingsStore by lazy {
        AiSettingsStore(appContext)
    }
    val aiRepository: AiRepository by lazy {
        AiRepository(
            db = database,
            client = OpenAiCompatibleClient(),
            keyStore = KeystoreAiKeyStore(appContext),
            settings = aiSettings,
        )
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
