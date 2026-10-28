package com.wendigo.tekrar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wendigo.tekrar.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) return
        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, 20)
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, 0)
        Notifications.showReminder(
            context,
            context.getString(R.string.notif_reminder_title),
            context.getString(R.string.notif_reminder_text),
        )
        ReminderScheduler.schedule(context, hour, minute)
    }
}
