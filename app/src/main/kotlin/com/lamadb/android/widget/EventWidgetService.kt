package com.lamadb.android.widget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Serves the collection RemoteViews for the event ticker widget.
 */
class EventWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return EventWidgetRemoteViewsFactory(applicationContext, intent)
    }
}
