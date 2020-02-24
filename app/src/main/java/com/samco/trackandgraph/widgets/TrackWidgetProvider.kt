package com.samco.trackandgraph.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.samco.trackandgraph.R
import timber.log.Timber

const val WIDGET_PREFS_NAME = "TrackWidget"
const val DELETE_FEATURE_ID = "DELETE_FEATURE_ID"

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
            context?.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
                remove(getFeatureIdPref(id))
                apply()
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val extras = intent?.extras
        if (extras?.containsKey(DELETE_FEATURE_ID) == true) {
            // Disable the widget if the feature is deleted.
            extras.getLong(DELETE_FEATURE_ID).let { removeFeatureId ->
                context?.let{ context ->
                    val sharedPref = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
                    for (appWidgetId in AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, this::class.java)
                    )) {
                        // Lookup the feature id for this widget and compare it to the deleted feature.
                        if (sharedPref.getLong(getFeatureIdPref(appWidgetId), -1) == removeFeatureId) {
                            // Disable updates to app widget.
                            AppWidgetHost(context, 0).deleteAppWidgetId(appWidgetId)
                            // TODO: change layout to indicate widget is no longer valid
                        }
                    }
                }
            }
        } else {
            super.onReceive(context, intent)
        }

    }
}