package com.wendigo.tekrar.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_SNOOZE) return
        val appContext = context.applicationContext
        Notifications.cancelReminder(appContext)
        ReminderScheduler.scheduleSnooze(appContext)
    }
}
