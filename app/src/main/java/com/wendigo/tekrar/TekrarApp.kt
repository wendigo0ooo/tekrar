package com.wendigo.tekrar

import android.app.Application
import android.content.Context
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.repo.SettingsStore
import com.wendigo.tekrar.repo.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    val repository: WordRepository by lazy {
        WordRepository(appContext, database, settings)
    }

    fun seed() {
        scope.launch {
            runCatching {
                repository.seedIfEmpty()
                repository.backfillExampleTr()
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
        container.seed()
    }
}
