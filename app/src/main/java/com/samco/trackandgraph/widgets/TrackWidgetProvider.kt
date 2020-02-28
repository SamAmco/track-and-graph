package com.samco.trackandgraph.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.samco.trackandgraph.R

const val WIDGET_PREFS_NAME = "TrackWidget"
const val DELETE_FEATURE_ID = "DELETE_FEATURE_ID"
const val UPDATE_FEATURE_ID = "UPDATE_FEATURE_ID"

class TrackWidgetProvider : AppWidgetProvider() {
    companion object {
        fun getFeatureIdPref(widgetId: Int) : String {
            return "widget_feature_id_$widgetId"
        }

        fun createRemoteViews(context: Context, appWidgetId: Int, title: String?, disable: Boolean = false) : RemoteViews {
            val remoteViews = RemoteViews(context.packageName, R.layout.track_widget)
            var drawable = 0

            if (disable) {
                drawable = R.drawable.warning_icon

                remoteViews.setTextViewText(R.id.track_widget_title, "Removed")
                remoteViews.setOnClickPendingIntent(R.id.widget_container, PendingIntent.getActivity(context, 0, Intent(), 0))
            } else {
                drawable = R.drawable.add_box

                val intent = Intent(context, TrackWidgetInputDataPoint::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                remoteViews.setOnClickPendingIntent(R.id.widget_container,
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))

                title?.let {
                    remoteViews.setTextViewText(R.id.track_widget_title, it)
                }
            }

            // Vector graphics in appwidgets need to be programmatically added.
            // Pre-Lollipop, these vectors need to be converted to a bitmap first.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                remoteViews.setImageViewResource(R.id.track_widget_icon, drawable)
            } else {
                ContextCompat.getDrawable(context, drawable)?.let { d ->
                    val b = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val c = Canvas(b)
                    d.setBounds(0, 0, c.width, c.height)
                    d.draw(c)
                    remoteViews.setImageViewBitmap(R.id.track_widget_icon, b)
                }
            }

            return remoteViews
        }

        fun updateWidgetsWithFeatureId(context: Context, featureId: Long) {
            getWidgetIdsForFeatureId(context, featureId).forEach { id ->
                val workIntent = Intent(context, WidgetJobIntentService::class.java)
                workIntent.putExtra("appWidgetIdExtra", id)
                WidgetJobIntentService.enqueueWork(context, workIntent)
            }
        }

        fun deleteWidgetsWithFeatureId(context: Context, featureId: Long) {
            getWidgetIdsForFeatureId(context, featureId).forEach { id ->
                val workIntent = Intent(context, WidgetJobIntentService::class.java)
                workIntent.putExtra("appWidgetIdExtra", id)
                workIntent.putExtra("disableWidgetExtra", true)
                WidgetJobIntentService.enqueueWork(context, workIntent)
            }
        }

        private fun getWidgetIdsForFeatureId(context: Context, featureId: Long) : List<Int> {
            return context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).let { sharedPrefs ->
                AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, TrackWidgetProvider::class.java)).let { ids ->
                        ids.filter { sharedPrefs.getLong(getFeatureIdPref(it), -1) == featureId }
                    }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds?.forEach { id ->
            val remoteViews = createRemoteViews(context, id, null)
            appWidgetManager.updateAppWidget(id, remoteViews)

            // Fire a background job to load the feature name.
            val workIntent = Intent(context, WidgetJobIntentService::class.java)
            workIntent.putExtra("appWidgetIdExtra", id)
            WidgetJobIntentService.enqueueWork(context, workIntent)
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

    override fun onReceive(context: Context, intent: Intent) {
        intent.extras?.let { extras ->
            when {
                extras.containsKey(UPDATE_FEATURE_ID) -> {
                    updateWidgetsWithFeatureId(context, extras.getLong(UPDATE_FEATURE_ID))
                }
                extras.containsKey(DELETE_FEATURE_ID) -> {
                    deleteWidgetsWithFeatureId(context, extras.getLong(DELETE_FEATURE_ID))
                }
                else -> super.onReceive(context, intent)
            }
        } ?: super.onReceive(context, intent)
    }
}