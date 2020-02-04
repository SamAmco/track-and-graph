package com.samco.trackandgraph.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.samco.trackandgraph.R

const val TRACK_WIDGET_BUTTON_CLICK = "TRACK_WIDGET_BUTTON_CLICK"

class TrackWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        appWidgetIds?.forEach { id ->
            val remoteViews = RemoteViews(context.packageName, R.layout.track_widget)
            remoteViews.setOnClickPendingIntent(R.id.track_widget_button,
                getPendingSelfIntent(context, TRACK_WIDGET_BUTTON_CLICK))

            appWidgetManager.updateAppWidget(id, remoteViews)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == TRACK_WIDGET_BUTTON_CLICK) {
            Log.d("Widget", "Widget button clicked!")
        }
    }

    private fun getPendingSelfIntent(context: Context, action: String) : PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = action
        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }
}