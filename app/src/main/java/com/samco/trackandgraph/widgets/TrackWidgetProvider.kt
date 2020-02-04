package com.samco.trackandgraph.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.samco.trackandgraph.R

const val FEATURE_KEY = "FEATURE_KEY"

class TrackWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        appWidgetIds?.forEach { id ->
            val remoteViews = RemoteViews(context.packageName, R.layout.track_widget)
            val intent = Intent(context, TrackWidgetInputDataPoint::class.java)
            // Temp id; let the user choose on widget construction in the future.
            val featureId: Long = 2
            intent.putExtra(FEATURE_KEY, featureId)
            remoteViews.setOnClickPendingIntent(R.id.track_widget_button,
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))

            appWidgetManager.updateAppWidget(id, remoteViews)
        }
    }
}