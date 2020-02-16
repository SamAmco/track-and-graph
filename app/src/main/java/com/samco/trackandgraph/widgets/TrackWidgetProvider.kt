package com.samco.trackandgraph.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.samco.trackandgraph.R

const val WIDGET_PREFS_NAME = "TrackWidget"

class TrackWidgetProvider : AppWidgetProvider() {
    companion object {
        fun getFeatureIdPref(widgetId: Int) : String {
            return "widget_feature_id_$widgetId"
        }
    }
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        appWidgetIds?.forEach { id ->
            val remoteViews = RemoteViews(context.packageName, R.layout.track_widget)
            val intent = Intent(context, TrackWidgetInputDataPoint::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)

            remoteViews.setOnClickPendingIntent(R.id.track_widget_button,
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))

            appWidgetManager.updateAppWidget(id, remoteViews)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)

        appWidgetIds?.forEach { id ->
            context?.getSharedPreferences(getFeatureIdPref(id), Context.MODE_PRIVATE)?.edit()?.apply {
                remove(getFeatureIdPref(id))
                apply()
            }
        }
    }
}