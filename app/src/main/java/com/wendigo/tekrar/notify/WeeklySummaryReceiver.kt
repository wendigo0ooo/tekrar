package com.wendigo.tekrar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wendigo.tekrar.core.WeeklySummary
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.repo.SettingsStore
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WeeklySummaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WeeklySummaryScheduler.ACTION_WEEKLY_SUMMARY) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsStore(appContext).settings.first()
                if (!settings.weeklySummaryEnabled) return@launch
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val logs = AppDatabase.get(appContext).reviewLogDao().allLogs()
                val stats = WeeklySummary.stats(logs, today, zone, settings.frozenDays)
                if (stats.cards > 0) {
                    Notifications.showWeeklySummary(
                        appContext,
                        WeeklySummary.summaryText(stats),
                    )
                }
            } catch (_: Exception) {
            } finally {
                try {
                    val enabled = runCatching {
                        SettingsStore(appContext).settings.first().weeklySummaryEnabled
                    }.getOrDefault(false)
                    if (enabled) WeeklySummaryScheduler.schedule(appContext)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
