package com.lamadb.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.lamadb.android.MainActivity
import com.lamadb.android.R

/**
 * Home screen ticker widget showing the latest LamaDB events.
 *
 * Tapping an event row opens the app; tapping the header forces a refresh.
 */
class EventWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, EventWidgetProvider::class.java))
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_event_list)
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        private const val ACTION_REFRESH = "com.lamadb.android.widget.ACTION_REFRESH"

        @Suppress("DEPRECATION")
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_event_list)

            // Header click refreshes the widget.
            val refreshIntent = Intent(context, EventWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)

            // List adapter intent.
            val adapterIntent = Intent(context, EventWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(appWidgetId, R.id.widget_event_list, adapterIntent)

            // Empty view.
            views.setEmptyView(R.id.widget_event_list, R.id.widget_empty_view)

            // Item click opens the main activity.
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPending = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_event_list, clickPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        @Suppress("DEPRECATION")
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, EventWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_event_list)
            }
        }
    }
}
