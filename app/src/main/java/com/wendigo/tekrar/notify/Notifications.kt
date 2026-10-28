package com.wendigo.tekrar.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wendigo.tekrar.MainActivity
import com.wendigo.tekrar.R
import com.wendigo.tekrar.StudyRequest

object Notifications {

    const val CHANNEL_REMINDER = "tekrar_reminder"
    const val REMINDER_ID = 2001
    const val CHANNEL_WEEKLY = "tekrar_weekly"
    const val WEEKLY_ID = 2002

    private const val START_STUDY_REQUEST_CODE = 7301
    private const val SNOOZE_REQUEST_CODE = 7302
    private const val WEEKLY_CONTENT_REQUEST_CODE = 7401

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_REMINDER) != null) return
        val channel = NotificationChannel(
            CHANNEL_REMINDER,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun ensureWeeklyChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_WEEKLY) != null) return
        val channel = NotificationChannel(
            CHANNEL_WEEKLY,
            context.getString(R.string.notif_weekly_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_weekly_channel_desc)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showReminder(context: Context, title: String, text: String): Boolean {
        if (!canPost(context)) return false
        ensureChannel(context)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val startIntent = StudyRequest.activityIntent(context, StudyRequest.DEFAULT_LIMIT)
        val startPending = PendingIntent.getActivity(
            context,
            START_STUDY_REQUEST_CODE,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_SNOOZE
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(
                R.drawable.ic_notif,
                context.getString(R.string.notif_action_start),
                startPending,
            )
            .addAction(
                R.drawable.ic_notif,
                context.getString(R.string.notif_action_snooze),
                snoozePending,
            )
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(REMINDER_ID, notification)
            true
        }.getOrDefault(false)
    }

    fun cancelReminder(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(REMINDER_ID)
        }
    }

    fun showWeeklySummary(context: Context, text: String): Boolean {
        if (!canPost(context)) return false
        ensureWeeklyChannel(context)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            WEEKLY_CONTENT_REQUEST_CODE,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_WEEKLY)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(context.getString(R.string.notif_weekly_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(WEEKLY_ID, notification)
            true
        }.getOrDefault(false)
    }
}
