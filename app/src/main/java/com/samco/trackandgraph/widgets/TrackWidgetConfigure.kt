package com.samco.trackandgraph.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.samco.trackandgraph.R

class TrackWidgetConfigure : Activity() {
    var appWidgetId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.track_widget_configure)

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        Log.d("widget", "saved app widget id as")
        Log.d("widget", appWidgetId.toString())
        setResult(RESULT_CANCELED)
    }

    fun onConfirm(view: View) {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, TrackWidgetProvider::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, arrayOf(appWidgetId))
        sendBroadcast(intent)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()

    }
}