package com.wendigo.tekrar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wendigo.tekrar.repo.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsStore(appContext).settings.first()
                if (settings.reminderEnabled) {
                    ReminderScheduler.schedule(
                        appContext,
                        settings.reminderHour,
                        settings.reminderMinute,
                    )
                } else {
                    ReminderScheduler.cancel(appContext)
                }
                if (settings.weeklySummaryEnabled) {
                    WeeklySummaryScheduler.schedule(appContext)
                } else {
                    WeeklySummaryScheduler.cancel(appContext)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
