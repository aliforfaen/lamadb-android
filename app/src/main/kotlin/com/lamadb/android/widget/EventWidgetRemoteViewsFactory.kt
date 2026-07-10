package com.lamadb.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.lamadb.android.MainActivity
import com.lamadb.android.R
import com.lamadb.android.data.api.EventResponse
import com.lamadb.android.data.api.LamaDBApiClient
import com.lamadb.android.data.auth.SecureTokenStore
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

/**
 * RemoteViews factory that loads the latest events from LamaDB for the
 * ticker home-screen widget.
 */
class EventWidgetRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var events: List<EventViewModel> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        events = loadEvents()
    }

    override fun onDestroy() {
        events = emptyList()
    }

    override fun getCount(): Int = events.size

    override fun getViewAt(position: Int): RemoteViews {
        val event = events.getOrNull(position) ?: return emptyView()
        return RemoteViews(context.packageName, R.layout.widget_event_item).apply {
            setTextViewText(R.id.widget_event_title, event.title)
            setTextViewText(R.id.widget_event_meta, event.meta)
            setTextViewText(R.id.widget_event_severity, event.severity)

            val fillInIntent = Intent().apply {
                putExtra(EventWidgetProvider.EXTRA_EVENT_ID, event.id)
            }
            setOnClickFillInIntent(R.id.widget_event_item, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = events.getOrNull(position)?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun emptyView(): RemoteViews {
        val text = if (SecureTokenStore(context).load().getOrNull() == null) {
            R.string.widget_not_logged_in
        } else {
            R.string.widget_empty_text
        }
        return RemoteViews(context.packageName, R.layout.widget_event_empty).apply {
            setTextViewText(R.id.widget_empty_text, context.getString(text))
        }
    }

    private fun loadEvents(): List<EventViewModel> {
        val credentials = SecureTokenStore(context).load().getOrNull() ?: return emptyList()
        val client = LamaDBApiClient(credentials.serverUrl, credentials.apiKey)
        val formatter = SimpleDateFormat("MMM d HH:mm", Locale.getDefault())

        return runBlocking {
            client.getEvents(limit = 3).getOrElse { emptyList() }
                .map { event ->
                    EventViewModel(
                        id = event.id,
                        title = event.title,
                        meta = "${event.source} • ${formatTimestamp(event.ts, formatter)}",
                        severity = event.severity.uppercase()
                    )
                }
        }
    }

    private fun formatTimestamp(ts: String?, formatter: SimpleDateFormat): String {
        if (ts.isNullOrBlank()) return "?"
        return try {
            val instant = Instant.parse(ts)
            formatter.format(Date.from(instant))
        } catch (_: Exception) {
            ts
        }
    }

    private data class EventViewModel(
        val id: Int,
        val title: String,
        val meta: String,
        val severity: String
    )
}
