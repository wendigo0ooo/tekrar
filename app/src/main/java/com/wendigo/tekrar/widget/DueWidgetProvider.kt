package com.wendigo.tekrar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.wendigo.tekrar.R
import com.wendigo.tekrar.StudyRequest
import com.wendigo.tekrar.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DueWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateWidgets(appContext, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        private const val START_STUDY_REQUEST_CODE = 7351

        fun refresh(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(
                ComponentName(appContext, DueWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { updateWidgets(appContext, ids) }
            }
        }

        private suspend fun updateWidgets(context: Context, ids: IntArray) {
            if (ids.isEmpty()) return
            val due = runCatching {
                AppDatabase.get(context).wordDao().dueReviews(System.currentTimeMillis()).size
            }.getOrDefault(0)
            val views = buildViews(context, due)
            withContext(Dispatchers.Main) {
                val manager = AppWidgetManager.getInstance(context)
                ids.forEach { id -> manager.updateAppWidget(id, views) }
            }
        }

        private fun buildViews(context: Context, due: Int): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_due).apply {
                setTextViewText(
                    R.id.widget_count,
                    if (due > 0) {
                        context.getString(R.string.widget_due_count, due)
                    } else {
                        context.getString(R.string.widget_due_zero)
                    },
                )
                val intent = StudyRequest.activityIntent(context, StudyRequest.DEFAULT_LIMIT)
                val pending = PendingIntent.getActivity(
                    context,
                    START_STUDY_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                setOnClickPendingIntent(R.id.widget_action, pending)
            }
    }
}
