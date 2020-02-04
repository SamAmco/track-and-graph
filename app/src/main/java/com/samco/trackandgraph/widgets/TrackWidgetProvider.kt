package com.samco.trackandgraph.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.DataPoint
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime

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
            GlobalScope.launch {
                val dao = TrackAndGraphDatabase.getInstance(context).trackAndGraphDatabaseDao
                val trackGroups = dao.getAllGroupsSync()
                val feature = dao.getFeatureById(trackGroups[0].id)
                val newDataPoint = DataPoint(OffsetDateTime.now(), feature.id, 1.0, "")
                Log.d("Widget", "inserting datapoint " + newDataPoint.toString())
                dao.insertDataPoint(newDataPoint)
            }
        }
    }

    private fun getPendingSelfIntent(context: Context, action: String) : PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = action
        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }
}