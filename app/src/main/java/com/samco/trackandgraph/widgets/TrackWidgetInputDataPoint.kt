package com.samco.trackandgraph.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.samco.trackandgraph.displaytrackgroup.FEATURE_LIST_KEY

class TrackWidgetInputDataPoint : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras

        bundle?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)?.let { widgetId ->
            val sharedPref = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val featureId = sharedPref.getLong(TrackWidgetProvider.getFeatureIdPref(widgetId), -1)
            if (featureId == -1L) {
                finish()
                return
            }

            val dialog = TrackWidgetInputDataPointDialog()
            val args = Bundle()
            args.putLongArray(FEATURE_LIST_KEY, longArrayOf(featureId))
            dialog.arguments = args

            if (savedInstanceState == null) {
                supportFragmentManager.let { dialog.show(it, "input_data_points_dialog") }
            }
        }
    }
}