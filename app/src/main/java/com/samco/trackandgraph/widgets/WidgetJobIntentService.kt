package com.samco.trackandgraph.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.samco.trackandgraph.database.TrackAndGraphDatabase

class WidgetJobIntentService : JobIntentService() {
    companion object {
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, WidgetJobIntentService::class.java, 0, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val appWidgetManager = AppWidgetManager.getInstance(this)

        val appWidgetId = intent.getIntExtra("appWidgetIdExtra", -1)
        val disableWidget = intent.getBooleanExtra("disableWidgetExtra", false)

        val featureId = this.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).getLong(
            TrackWidgetProvider.getFeatureIdPref(appWidgetId), -1)
        val title = TrackAndGraphDatabase.getInstance(this).trackAndGraphDatabaseDao.tryGetFeatureById(featureId)?.name
        val remoteViews = TrackWidgetProvider.createRemoteViews(this, appWidgetId, title, disableWidget)
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

}