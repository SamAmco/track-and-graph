package com.samco.trackandgraph.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.samco.trackandgraph.R

class TrackWidgetConfigure : Activity() {

    private var appWidgetId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.track_widget_configure)

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED)
    }

    fun onConfirm(view: View) {
        appWidgetId?.let { id ->
            if (appWidgetId == null) {
                return
            }

            val featureId: Long = 2
            val sharedPref = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit()
            sharedPref.putLong(TrackWidgetProvider.getFeatureIdPref(id), featureId)
            sharedPref.apply()

            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, TrackWidgetProvider::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
            sendBroadcast(intent)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}