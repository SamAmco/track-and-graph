package com.samco.trackandgraph.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.JobIntentService
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import timber.log.Timber

class WidgetJobIntentService : JobIntentService() {
    companion object {
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, WidgetJobIntentService::class.java, 123, work)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
    }

    override fun onHandleWork(intent: Intent) {
        Timber.d("onHandleWork")

        val appWidgetManager = AppWidgetManager.getInstance(this)

        val appWidgetId = intent.getIntExtra("appWidgetIdExtra", -1)

        val featureId = this.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).getLong(
            TrackWidgetProvider.getFeatureIdPref(appWidgetId), -1)
        val title = TrackAndGraphDatabase.getInstance(this).trackAndGraphDatabaseDao.tryGetFeatureById(featureId)?.name
        val remoteViews = TrackWidgetProvider.createRemoteViews(this, appWidgetId, title)

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

}