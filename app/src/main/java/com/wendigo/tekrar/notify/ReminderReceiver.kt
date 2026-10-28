package com.wendigo.tekrar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wendigo.tekrar.R
import com.wendigo.tekrar.core.ReminderMessage
import com.wendigo.tekrar.core.StatsMath
import com.wendigo.tekrar.data.AppDatabase
import com.wendigo.tekrar.repo.SettingsStore
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) return
        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, 20)
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, 0)
        val snooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZE, false)
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val message = runCatching { buildMessage(appContext) }.getOrNull()
                if (message != null) {
                    Notifications.showReminder(
                        appContext,
                        appContext.getString(R.string.notif_reminder_title),
                        message,
                    )
                }
            } finally {
                try {
                    if (!snooze) {
                        ReminderScheduler.schedule(appContext, hour, minute)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private suspend fun buildMessage(context: Context): String? {
        val words = AppDatabase.get(context).wordDao()
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val dayStart = StatsMath.startOfDay(LocalDate.now(zone), zone)
        val settings = SettingsStore(context).settings.first()
        val due = words.dueReviews(now).size
        val introduced = words.introducedCountSince(dayStart)
        val remaining = (settings.newPerDay - introduced).coerceAtLeast(0)
        val newAvailable = if (remaining > 0) words.newWords(remaining).size else 0
        return when (val message = ReminderMessage.of(due, newAvailable)) {
            is ReminderMessage.Due ->
                context.getString(R.string.notif_due_text, message.count)
            is ReminderMessage.NewWords ->
                context.getString(R.string.notif_new_text, message.count)
            ReminderMessage.None -> null
        }
    }
}
