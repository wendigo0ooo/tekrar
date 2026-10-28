package com.wendigo.tekrar.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wendigo.tekrar.core.WeeklySummary
import java.time.ZoneId

object WeeklySummaryScheduler {

    const val ACTION_WEEKLY_SUMMARY = "com.wendigo.tekrar.action.WEEKLY_SUMMARY"

    private const val REQUEST_CODE = 4201
    private const val FALLBACK_WINDOW_MS = 60 * 60 * 1000L

    fun schedule(context: Context, now: Long = System.currentTimeMillis()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context)
        val triggerAt = WeeklySummary.nextSundayAt(now, ZoneId.systemDefault())
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }.recoverCatching {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                FALLBACK_WINDOW_MS,
                pending,
            )
        }.recoverCatching {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, update = false))
    }

    private fun pendingIntent(context: Context, update: Boolean = true): PendingIntent {
        val intent = Intent(context, WeeklySummaryReceiver::class.java).apply {
            action = ACTION_WEEKLY_SUMMARY
        }
        val flags = if (update) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
